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
import java.util.LinkedHashMap;

import com.tremolosecurity.util.NVP;

public class ScaleAttribute {
	String name;
	String label;
	String value;
	
	boolean isText;
	boolean isList;
	boolean isCheckbox;
	
	ArrayList<NVP> listValues;
	
	
	public ScaleAttribute() {
		this.name = "";
		this.label = "";
		this.value = "";
		
		this.isText = true;
		this.isList = false;
		this.isCheckbox = false;
	}
	
	public ScaleAttribute(String name,String label,String value) {
		this.name = name;
		this.label = label;
		this.value = value;
		
		this.isText = true;
		this.isList = false;
		this.isCheckbox = false;
	}
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}

	public boolean isText() {
		return isText;
	}

	public void setText(boolean isText) {
		this.isText = isText;
	}

	public boolean isList() {
		return isList;
	}

	public void setList(boolean isList) {
		this.isList = isList;
	}

	public boolean isCheckbox() {
		return isCheckbox;
	}

	public void setCheckbox(boolean isCheckbox) {
		this.isCheckbox = isCheckbox;
	}

	public ArrayList<NVP> getListValues() {
		
		return listValues;
	}

	public void setListValues(ArrayList<NVP> listValues) {
		
		this.listValues = listValues;
	}
	

	
}
