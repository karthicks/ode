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
package org.apache.ode.bpel.obj;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import de.danielbechler.diff.annotation.ObjectDiffProperty;

/**
 * Compiled represnetation of a BPEL activity.
 */
public abstract class OActivity extends OAgent  implements Serializable{
	public static final long serialVersionUID = -1L;

	private static final String JOINCONDITION = "joinCondition";
	private static final String SUPPRESSJOINFAILURE = "suppressJoinFailure";
	private static final String SOURCELINKS = "sourceLinks";
	private static final String TARGETLINKS = "targetLinks";
	private static final String NAME = "name";
	private static final String FAILUREHANDLING = "failureHandling";
	private static final String PARENT = "parent";

	@JsonCreator
	public OActivity() {
		setSuppressJoinFailure(false);
	}

	public OActivity(OProcess owner, OActivity parent) {
		super(owner);
		setParent(parent);
		setSourceLinks(new HashSet<OLink>());
		setTargetLinks(new HashSet<OLink>());
		setSuppressJoinFailure(false);
	}

	@Override
	public String digest() {
		StringBuffer buf = new StringBuffer(getClass().getSimpleName());
		buf.append('#');
		buf.append(getId());
		buf.append("{");
		List<OAgent> l = new ArrayList<OAgent>();
		l.addAll(getNested());
		Collections.sort(l, new Comparator<OAgent>() {
			public int compare(OAgent o1, OAgent o2) {
				return key(o1).compareTo(key(o2));
			}

			private String key(OAgent o) {
				return o.getClass().getSimpleName() + "#" + o.getId();
			}
		});

		for (OAgent child : l) {
			buf.append(child.digest());
			buf.append(";");
		}
		buf.append("}");
		return buf.toString();
	}

	@ObjectDiffProperty(ignore = true)
	@JsonIgnore
	public OFailureHandling getFailureHandling() {
		OFailureHandling handling = (OFailureHandling) fieldContainer
				.get(FAILUREHANDLING);
		if (handling == null) {
			OActivity parent = this.getParent();
			while (parent != null && handling == null) {
				handling = parent.getFailureHandling();
				parent = parent.getParent();
			}
		}
		return handling;
	}

	@JsonIgnore
	public OExpression getJoinCondition() {
		Object o = fieldContainer.get(JOINCONDITION);
		return o == null ? null : (OExpression) o;
	}

	@JsonIgnore
	public String getName() {
		Object o = fieldContainer.get(NAME);
		return o == null ? null : (String) o;
	}

	@SuppressWarnings("unchecked")
	@JsonIgnore
	public Set<OLink> getSourceLinks() {
		Object o = fieldContainer.get(SOURCELINKS);
		return o == null ? null : (Set<OLink>) o;
	}

	@JsonIgnore
	public boolean isSuppressJoinFailure() {
		Object o = fieldContainer.get(SUPPRESSJOINFAILURE);
		return o == null ? null : (Boolean) o;
	}

	@SuppressWarnings("unchecked")
	@JsonIgnore
	public Set<OLink> getTargetLinks() {
		Object o = fieldContainer.get(TARGETLINKS);
		return o == null ? null : (Set<OLink>) o;
	}

	@JsonIgnore
	public String getType() {
		return getClass().getSimpleName();
	}

	public void setFailureHandling(OFailureHandling failureHandling) {
		fieldContainer.put(FAILUREHANDLING, failureHandling);
	}

	public void setJoinCondition(OExpression joinCondition) {
		fieldContainer.put(JOINCONDITION, joinCondition);
	}

	public void setName(String name) {
		fieldContainer.put(NAME, name);
	}

	public void setSourceLinks(Set<OLink> sourceLinks) {
		if (getSourceLinks() == null) {
			fieldContainer.put(SOURCELINKS, sourceLinks);
		}
	}

	public void setSuppressJoinFailure(boolean suppressJoinFailure) {
		fieldContainer.put(SUPPRESSJOINFAILURE, suppressJoinFailure);
	}

	public void setTargetLinks(Set<OLink> targetLinks) {
		if (getTargetLinks() == null) {
			fieldContainer.put(TARGETLINKS, targetLinks);
		}
	}

	public String toString() {
		StringBuffer buf = new StringBuffer(super.toString());
		if (getName() != null) {
			buf.append('-');
			buf.append(getName());
		}

		return buf.toString();
	}

	@JsonIgnore
	public OActivity getParent() {
		Object o = fieldContainer.get(PARENT);
		return o == null ? null : (OActivity)o;
	}

	private void setParent(OActivity parent) {
		fieldContainer.put(PARENT, parent);
	}
}
