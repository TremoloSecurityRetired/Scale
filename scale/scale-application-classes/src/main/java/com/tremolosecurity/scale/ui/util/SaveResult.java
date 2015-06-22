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
package com.tremolosecurity.scale.ui.util;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;

@ManagedBean(name="saveResults")
@SessionScoped
public class SaveResult {
	boolean saved;
	boolean error;
	List<String> errors;
	List<String> savedRequests;
	
	public SaveResult() {
		this.errors = new ArrayList<String>();
		this.saved = false;
		this.error = false;
		this.savedRequests = new ArrayList<String>();
	}
	
	
	public boolean isSaved() {
		return saved;
	}
	public void setSaved(boolean saved) {
		this.saved = saved;
	}
	public boolean isError() {
		return error;
	}
	public void setError(boolean error) {
		this.error = error;
	}
	public List<String> getErrors() {
		return errors;
	}
	public void setErrors(List<String> errors) {
		this.errors = errors;
	}
	
	public void reset() {
		this.saved = false;
		this.error = false;
		this.errors = new ArrayList<String>();
		this.savedRequests = new ArrayList<String>();
	}


	public List<String> getSavedRequests() {
		return savedRequests;
	}

	
	
}
