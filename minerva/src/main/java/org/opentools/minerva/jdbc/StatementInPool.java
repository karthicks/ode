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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.SQLWarning;

/**
 * Wraps a Statement to track errors and the last used time for the owning connection. That time is updated every time a SQL action
 * is performed (executeQuery, executeUpdate, etc.).
 * 
 * @author Aaron Mulder (ammulder@alumni.princeton.edu)
 */
public class StatementInPool implements Statement {
    private final static String CLOSED = "Statement has been closed!";

    private Statement impl;

    private ConnectionWrapper con;


    /**
     * Creates a new statement from a source statement and wrapper connection.
     */
    public StatementInPool(Statement source, ConnectionWrapper owner) {
        if (source == null || owner == null)
            throw new NullPointerException();
        impl = source;
        con = owner;
    }

    /**
     * Updates the last used time for the owning connection to the current time.
     */
    public void setLastUsed() {
        if (con != null)
            con.setLastUsed();
    }

    /**
     * Indicates that an error occured on the owning connection.
     */
    public void setError(SQLException e) {
        if (con != null)
            con.setError(e);
    }

    /**
     * Gets a reference to the "real" Statement. This should only be used if you need to cast that to a specific type to call a
     * proprietary method - you will defeat all the pooling if you use the underlying Statement directly.
     */
    public Statement getUnderlyingStatement() {
        return impl;
    }

    // ---- Implementation of java.sql.Statement ----

    public void addBatch(String arg0) throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            impl.addBatch(arg0);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public void cancel() throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            impl.cancel();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public void clearBatch() throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            impl.clearBatch();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public void clearWarnings() throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            impl.clearWarnings();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public void close() throws SQLException {
        if (impl != null) {
            impl.close();
            con.statementClosed(this);
        }
        clearFields();
    }

    protected void clearFields() {
        con = null;
        impl = null;
    }

    public boolean execute(String arg0) throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            setLastUsed();
            return impl.execute(arg0);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public int[] executeBatch() throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            setLastUsed();
            return impl.executeBatch();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public ResultSet executeQuery(String arg0) throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            setLastUsed();
            return new ResultSetInPool(impl.executeQuery(arg0), this);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public int executeUpdate(String arg0) throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            setLastUsed();
            return impl.executeUpdate(arg0);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public Connection getConnection() throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        return con;
    }

    public int getFetchDirection() throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            return impl.getFetchDirection();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public int getFetchSize() throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            return impl.getFetchSize();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public int getMaxFieldSize() throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            return impl.getMaxFieldSize();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public int getMaxRows() throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            return impl.getMaxRows();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public boolean getMoreResults() throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            return impl.getMoreResults();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public int getQueryTimeout() throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            return impl.getQueryTimeout();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public ResultSet getResultSet() throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            return new ResultSetInPool(impl.getResultSet(), this);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public int getResultSetConcurrency() throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            return impl.getResultSetConcurrency();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public int getResultSetType() throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            return impl.getResultSetType();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public int getUpdateCount() throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            return impl.getUpdateCount();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public SQLWarning getWarnings() throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            return impl.getWarnings();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public void setCursorName(String arg0) throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            impl.setCursorName(arg0);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public void setEscapeProcessing(boolean arg0) throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            impl.setEscapeProcessing(arg0);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public void setFetchDirection(int arg0) throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            impl.setFetchDirection(arg0);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public void setFetchSize(int arg0) throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            impl.setFetchSize(arg0);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public void setMaxFieldSize(int arg0) throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            impl.setMaxFieldSize(arg0);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public void setMaxRows(int arg0) throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            impl.setMaxRows(arg0);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public void setQueryTimeout(int arg0) throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            impl.setQueryTimeout(arg0);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public int getResultSetHoldability() throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            setLastUsed();
            return impl.getResultSetHoldability();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }

    }

    public boolean getMoreResults(int current) throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            setLastUsed();
            return impl.getMoreResults(current);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            setLastUsed();
            return impl.executeUpdate(sql, autoGeneratedKeys);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            setLastUsed();
            return impl.execute(sql, autoGeneratedKeys);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public int executeUpdate(String sql, int columnIndexes[]) throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            setLastUsed();
            return impl.executeUpdate(sql, columnIndexes);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public boolean execute(String sql, int columnIndexes[]) throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            setLastUsed();
            return impl.execute(sql, columnIndexes);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            setLastUsed();
            return impl.getGeneratedKeys();
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public int executeUpdate(String sql, String columnNames[]) throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            setLastUsed();
            return impl.executeUpdate(sql, columnNames);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }

    public boolean execute(String sql, String columnNames[]) throws SQLException {
        if (impl == null)
            throw new SQLException(CLOSED);
        try {
            setLastUsed();
            return impl.execute(sql, columnNames);
        } catch (SQLException e) {
            setError(e);
            throw e;
        }
    }
    // ---- End Implementation of Statement ----
}
