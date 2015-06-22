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
import java.util.LinkedHashMap;
import java.util.regex.Pattern;








import org.apache.log4j.Logger;

import com.tremolosecurity.scale.config.xml.AttributeType;
import com.tremolosecurity.scale.config.xml.NameValueType;
import com.tremolosecurity.scale.config.xml.UserAttributesConfigType;
import com.tremolosecurity.util.NVP;


public class AttributeData {
	
	static Logger logger = Logger.getLogger(AttributeData.class.getName());
	
	HashMap<String,String> labels;
	HashSet<String> readOnly;
	HashSet<String> required;
	ArrayList<String> attributeNames;
	HashMap<String,Integer> minSizes;
	HashMap<String,Integer> maxSizes;
	HashMap<String,Pattern> regExs;
	HashMap<String,String> failErrMsgs;
	HashSet<String> textControls;
	HashSet<String> listControls;
	HashSet<String> checkBoxControls;
	HashMap<String,ArrayList<NVP>> listSources;
	
	
	public AttributeData() {
		this.labels = new HashMap<String,String>();
		this.attributeNames = new ArrayList<String>();
		this.readOnly = new HashSet<String>();
		this.required = new HashSet<String>();
		this.maxSizes = new HashMap<String,Integer>();
		this.minSizes = new HashMap<String,Integer>();
		this.regExs = new HashMap<String,Pattern>();
		this.failErrMsgs = new HashMap<String,String>();
		
		this.textControls = new HashSet<String>();
		this.listControls = new HashSet<String>();
		this.checkBoxControls = new HashSet<String>();
		this.listSources = new HashMap<String,ArrayList<NVP>>();
	}
	
	public AttributeData(String... attrName) {
		this();
		
		for (String attr : attrName) {
			this.labels.put(attr, attr);
			this.attributeNames.add(attr);
			this.readOnly.add(attr);
		}
	}
	
	public AttributeData(UserAttributesConfigType cfg) {
		this();
		
		for (AttributeType attr : cfg.getAttribute()) {
			this.labels.put(attr.getName(), attr.getLabel());
			this.attributeNames.add(attr.getName());
			if (attr.isReadOnly()) {
				this.readOnly.add(attr.getName());
			}
			
			if (attr.isRequired()) {
				this.required.add(attr.getName());
			}
			
			this.minSizes.put(attr.getName(), attr.getMinSize());
			this.maxSizes.put(attr.getName(), attr.getMaxSize());
			if (attr.getRegEx() != null && ! attr.getRegEx().isEmpty()) {
				Pattern p = Pattern.compile(attr.getRegEx());
				this.regExs.put(attr.getName(),p);
				this.failErrMsgs.put(attr.getName(), attr.getRegExFailedMsg());
			}
			
			if (attr.getControlType().equalsIgnoreCase("text")) {
				this.textControls.add(attr.getName());
			} else if (attr.getControlType().equalsIgnoreCase("list")) {
				this.listControls.add(attr.getName());
			} else if (attr.getControlType().equalsIgnoreCase("checkbox")) {
				this.checkBoxControls.add(attr.getName());
			}
			
			
			if (logger.isDebugEnabled()) logger.debug("Checking list value");
			if (attr.getListValue() != null) {
				if (logger.isDebugEnabled()) logger.debug("Creating list values");
				ArrayList<NVP> listVals = new ArrayList<NVP>();
				for (NameValueType nvt : attr.getListValue()) {
					listVals.add(new NVP(nvt.getName(), nvt.getValue()));					
				}
				
				this.listSources.put(attr.getName(), listVals);
				if (logger.isDebugEnabled()) logger.debug("List sources : " + this.listSources);
			}
		}
				
				
	}
	

	
	public HashMap<String,String> getLabels() {
		return this.labels;
	}
	
	public boolean isReadOnly(String name) {
		return this.readOnly.contains(name);
	}
	
	public boolean isRequired(String name) {
		return this.required.contains(name);
	}
	
	public ArrayList<String> getAttributeNames() {
		return this.attributeNames;
	}



	public HashMap<String, Integer> getMinSizes() {
		return minSizes;
	}



	public HashMap<String, Integer> getMaxSizes() {
		return maxSizes;
	}



	public HashMap<String, Pattern> getRegExs() {
		return regExs;
	}



	public HashMap<String, String> getFailErrMsgs() {
		return failErrMsgs;
	}



	public HashSet<String> getTextControls() {
		return textControls;
	}



	public HashSet<String> getListControls() {
		return listControls;
	}



	public HashSet<String> getCheckBoxControls() {
		return checkBoxControls;
	}



	public HashMap<String, ArrayList<NVP>> getListSources() {
		
		return listSources;
	}
	
	
}
