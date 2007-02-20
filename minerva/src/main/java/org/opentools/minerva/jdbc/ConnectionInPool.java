/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Licensed under the X license (see http://www.x.org/terms.htm)
 */
package org.opentools.minerva.jdbc;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.opentools.minerva.cache.LeastRecentlyUsedCache;
import org.opentools.minerva.cache.ObjectCache;
import org.opentools.minerva.pool.PoolEvent;
import org.opentools.minerva.pool.PoolEventListener;
import org.opentools.minerva.pool.PooledObject;

/**
 * Wrapper for database connections in a pool. Handles closing appropriately. The connection is returned to the pool rather than
 * truly closing, any outstanding statements are closed, and the connection is rolled back. This class is also used by statements,
 * etc. to update the last used time for the connection.
 * 
 * @author Aaron Mulder (ammulder@alumni.princeton.edu)
 */
public class ConnectionInPool implements PooledObject, ConnectionWrapper {
    private final static String CLOSED = "Connection has been closed!";

    public final static int PS_CACHE_UNLIMITED = 0;

    public final static int PS_CACHE_DISABLED = -1;

    public final static Map<Connection, ObjectCache> psCaches = new HashMap<Connection, ObjectCache>();

    private Connection con;

    private Set<Statement> statements;

    private Vector<PoolEventListener> listeners;

    private int preparedStatementCacheSize = 0;

    private ObjectCache preparedStatementCache;

    /**
     * Creates a new connection wrapper.
     * 
     * @param con
     *            The "real" database connection to wrap.
     */
    public ConnectionInPool(Connection con) {
        this.con = con;
        preparedStatementCache = psCaches.get(con);
        if (preparedStatementCache == null) {
            PreparedStatementFactory factory = new PreparedStatementFactory(con);
            preparedStatementCache = new LeastRecentlyUsedCache(factory, preparedStatementCacheSize);
            psCaches.put(con, preparedStatementCache);
        }
        statements = new HashSet<Statement>();
        listeners = new Vector<PoolEventListener>();
    }

    /**
     * Creates a new connection wrapper, using the specified maximum size for the prepared statement cache.
     * 
     * @param con
     *            The "real" database connection to wrap.
     * @param psCacheSize
     *            The size of the PreparedStatement cache.
     * @see #PS_CACHE_UNLIMITED
     * @see #PS_CACHE_DISABLED
     */
    public ConnectionInPool(Connection con, int psCacheSize) {
        this.con = con;
        if (psCacheSize >= 0) {
            preparedStatementCache = psCaches.get(con);
            if (preparedStatementCache == null) {
                PreparedStatementFactory factory = new PreparedStatementFactory(con);
                preparedStatementCache = new LeastRecentlyUsedCache(factory, preparedStatementCacheSize);
                psCaches.put(con, preparedStatementCache);
            }
        }
        setPSCacheSize(psCacheSize);
        statements = new HashSet<Statement>();
        listeners = new Vector<PoolEventListener>();
    }

    /**
     * Sets the number of PreparedStatements to be cached for each Connection. Your DB product may impose a limit on the number of
     * open PreparedStatements.
     * 
     * @see #PS_CACHE_UNLIMITED
     * @see #PS_CACHE_DISABLED
     */
    public void setPSCacheSize(int maxSize) {
        preparedStatementCacheSize = maxSize;
        if (maxSize >= 0 && preparedStatementCache != null)
            preparedStatementCache.setSize(maxSize);
    }

    /**
     * Gets the number of PreparedStatements to be cached for each Connection.
     */
    public int getPSCacheSize() {
        return preparedStatementCacheSize;
    }

    /**
     * Adds a listener for pool events.
     */
    public void addPoolEventListener(PoolEventListener listener) {
        listeners.addElement(listener);
    }

    /**
     * Removes a listener for pool events.
     */
    public void removePoolEventListener(PoolEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Gets a reference to the "real" connection. This should only be used if you need to cast that to a specific type to call a
     * proprietary method - you will defeat all the pooling if you use the underlying connection directly.
     */
    public Connection getUnderlyingConnection() {
        return con;
    }

    /**
     * Closes this connection wrapper permanently. All further calls with throw a SQLException.
     */
    public void shutdown() {
        con = null;
        statements = null;
        listeners = null;
    }

    /**
     * Updates the last used time for this connection to the current time.
     */
    public void setLastUsed() {
        firePoolEvent(new PoolEvent(this, PoolEvent.OBJECT_USED));
    }

    /**
     * Indicates that an error occured on this connection.
     */
    public void setError(SQLException e) {
        firePoolEvent(new PoolEvent(this, PoolEvent.OBJECT_ERROR));
    }

    /**
     * Indicates that an error occured on this connection.
     */
    public void setCatastrophicError(SQLException e) {
        PoolEvent pe = new PoolEvent(this, PoolEvent.OBJECT_ERROR);
        pe.setCatastrophic();
        firePoolEvent(pe);
    }

    /**
     * Indicates that a statement has been closed and no longer needs to be tracked. Outstanding statements are closed when the
     * connection is returned to the pool.
     */
    public void statementClosed(Statement st) {
        statements.remove(st);
        if ((con != null) && (st instanceof PreparedStatementInPool)) {
            // Now return the "real" statement to the pool
            PreparedStatementInPool ps = (PreparedStatementInPool) st;
            PreparedStatement ups = ps.getUnderlyingPreparedStatement();
            if (preparedStatementCacheSize >= 0) {
                preparedStatementCache.returnObject(ps.getArgs(), ups);
            } else {
                try {
                    ups.close();
                } catch (SQLException e) {
                }
            }
            /*
             * int rsType = ResultSet.TYPE_FORWARD_ONLY; int rsConcur = ResultSet.CONCUR_READ_ONLY; // We may have JDBC 1.0 driver
             * try { rsType = ups.getResultSetType(); rsConcur = ups.getResultSetConcurrency(); } catch (Throwable th) { }
             * PreparedStatementInPool.preparedStatementCache.put( new PSCacheKey(con, ps.getSql(), rsType, rsConcur), ups);
             */
        }
    }

    /**
     * Prepares a connection to be returned to the pool. All outstanding statements are closed, and if AutoCommit is off, the
     * connection is rolled back. No further SQL calls are possible once this is called.
     */
    public void reset() throws SQLException {
        List<Statement> copy = new ArrayList<Statement>(statements);
        Iterator<Statement> it = copy.iterator();
        while (it.hasNext())
            try {
                it.next().close();
            } catch (SQLException e) {
            }
        if (!con.getAutoCommit()) {
            con.rollback();
        }
        con = null;
    }

    /**
     * Dispatches an event to the listeners.
     */
    protected void firePoolEvent(PoolEvent evt) {
        Vector local = (Vector) listeners.clone();
        for (int i = local.size() - 1; i >= 0; i--)
            if (evt.getType() == PoolEvent.OBJECT_CLOSED)
                ((PoolEventListener) local.elementAt(i)).objectClosed(evt);
            else if (evt.getType() == PoolEvent.OBJECT_ERROR)
                ((PoolEventListener) local.elementAt(i)).objectError(evt);
            else
                ((PoolEventListener) local.elementAt(i)).objectUsed(evt);
    }

    // ---- Implementation of java.sql.Connection ----
    public Statement createStatement() throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            StatementInPool st = new StatementInPool(con.createStatement(), this);
            statements.add(st);
            return st;
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public PreparedStatement prepareStatement(PreparedStatementArgs args) throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            PreparedStatementInPool wrapper = null;
            if (preparedStatementCacheSize >= 0) {
                PreparedStatement ps = (PreparedStatement) preparedStatementCache.useObject(args);
                if (ps == null)
                    throw new SQLException("Unable to create PreparedStatement!");
                wrapper = new PreparedStatementInPool(ps, this, args);
            } else {
                wrapper = new PreparedStatementInPool(con.prepareStatement(args.sql), this, args);
            }
            statements.add(wrapper);
            return wrapper;
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            return con.prepareCall(sql);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public String nativeSQL(String sql) throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            return con.nativeSQL(sql);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            con.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public boolean getAutoCommit() throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            return con.getAutoCommit();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public void commit() throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            con.commit();
        } catch (SQLException e) {
            setCatastrophicError(e);
            throw e;
        }
    }

    public void rollback() throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            con.rollback();
        } catch (SQLException e) {
            setCatastrophicError(e);
            throw e;
        }
    }

    public void close() throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        firePoolEvent(new PoolEvent(this, PoolEvent.OBJECT_CLOSED));
        shutdown();
    }

    public boolean isClosed() throws SQLException {
        if (con == null)
            return true;
        try {
            return con.isClosed();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            return con.getMetaData();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            con.setReadOnly(readOnly);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public boolean isReadOnly() throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            return con.isReadOnly();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public void setCatalog(String catalog) throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            con.setCatalog(catalog);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public String getCatalog() throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            return con.getCatalog();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public void setTransactionIsolation(int level) throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            con.setTransactionIsolation(level);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public int getTransactionIsolation() throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            return con.getTransactionIsolation();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public SQLWarning getWarnings() throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            return con.getWarnings();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public void clearWarnings() throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            con.clearWarnings();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            StatementInPool st = new StatementInPool(con.createStatement(resultSetType, resultSetConcurrency), this);
            statements.add(st);
            return st;
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return prepareStatement(sql);
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            return con.prepareCall(sql, resultSetType, resultSetConcurrency);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public Map<String, Class<?>> getTypeMap() throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            return con.getTypeMap();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        if (con == null) {
            throw new SQLException(CLOSED);
        }
        try {
            con.setTypeMap(map);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public int getHoldability() throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            return con.getHoldability();

        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public void setHoldability(int holdability) throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            con.setHoldability(holdability);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public Savepoint setSavepoint() throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);

        try {
            return con.setSavepoint();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            con.releaseSavepoint(savepoint);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            con.rollback(savepoint);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            Statement s = con.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
            statements.add(s);
            return s;
        } catch (SQLException e) {
            setError(e);
            throw e;
        }

    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            CallableStatement cs = con.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
            statements.add(cs);
            return cs;
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            return prepareStatement(new PreparedStatementArgs(sql, autoGeneratedKeys));
        } catch (SQLException e) {
            setError(e);
            throw e;
        }

    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            PreparedStatement ps = con.prepareStatement(sql, resultSetType, resultSetConcurrency);
            statements.add(ps);
            return ps;
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public PreparedStatement prepareStatement(String sql, int columnIndexes[]) throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            PreparedStatement ps = con.prepareStatement(sql, columnIndexes);
            statements.add(ps);
            return ps;
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);

        try {
            return con.setSavepoint(name);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public PreparedStatement prepareStatement(String sql, String columnNames[]) throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        try {
            PreparedStatement ps = con.prepareStatement(sql, columnNames);
            statements.add(ps);
            return ps;
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    /**
     * @see java.sql.Connection#prepareStatement(java.lang.String)
     */
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        if (con == null)
            throw new SQLException(CLOSED);
        return prepareStatement(new PreparedStatementArgs(sql));
    }

}
