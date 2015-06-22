/*
Copyright 2015 Tremolo Security, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.tremolosecurity.scale.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Logger;

public class UserObj {
	
	static Logger logger  = Logger.getLogger(UserObj.class.getName());
	
	HashMap<String,ScaleAttribute> attrs;
	ArrayList<String> groups;
	String displayName;
	private AttributeData attributeData;
	HashSet<String> executedWorkflows;
	
	public UserObj(AttributeData attributeData) { 
		this.attrs = new HashMap<String,ScaleAttribute>();
		this.groups = new ArrayList<String>();
		this.attributeData = attributeData;
		this.executedWorkflows = new HashSet<String>();
		
	}
	
	public HashMap<String, ScaleAttribute> getAttrs() {
		return attrs;
	}
	public void setAttrs(HashMap<String, ScaleAttribute> attrs) {
		this.attrs = attrs;
	}
	public ArrayList<String> getGroups() {
		return groups;
	}
	public void setGroups(ArrayList<String> groups) {
		this.groups = groups;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	public ArrayList<ScaleAttribute> getAttributes() {
		ArrayList<ScaleAttribute> attributes = new ArrayList<ScaleAttribute>();
		
		for (String name : attributeData.getAttributeNames()) {
			if (logger.isDebugEnabled()) logger.debug("in get attributes - name - " + name);
			ScaleAttribute attr = attrs.get(name);
			if (logger.isDebugEnabled()) logger.debug(attr);
			if (attr != null) {
				attributes.add(attr);
			} else {
				attributes.add(new ScaleAttribute(name,attributeData.getLabels().get(name),""));
			}
		}
		
		return attributes;
	}

	public HashSet<String> getExecutedWorkflows() {
		return executedWorkflows;
	}
	
	
	
	
}
