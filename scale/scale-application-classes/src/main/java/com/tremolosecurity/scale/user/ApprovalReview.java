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
import java.util.List;

import com.tremolosecurity.provisioning.service.util.ApprovalDetails;

public class ApprovalReview {
	UserObj userObj;
	ApprovalDetails details;
	String reason;
	boolean approved;
	
	boolean complete;
	
	ArrayList<String> errors;
	
	public ApprovalReview() {
		this.errors = new ArrayList<String>();
		this.complete = false;
	}
	
	public List<String> getErrors() {
		return this.errors;
	}
	
	public boolean isError() {
		return ! this.errors.isEmpty();
	}
	
	
	
	
	public UserObj getUserObj() {
		return userObj;
	}
	
	public void setUserObj(UserObj userObj) {
		this.userObj = userObj;
	}
	
	public ApprovalDetails getDetails() {
		return details;
	}
	
	public void setDetails(ApprovalDetails details) {
		this.details = details;
	}
	
	public String getReason() {
		if (reason.isEmpty()) {
			return "Supply Reason";
		} else {
			return reason;
		}
	}
	
	public void setReason(String reason) {
		if (! reason.equalsIgnoreCase("Supply Reason")) {
			this.reason = reason;
		}
	}
	
	public boolean hasReason() {
		return ! this.reason.isEmpty();
	}
	
	public boolean isApproved() {
		return approved;
	}
	public void setApproved(boolean approved) {
		this.approved = approved;
	}
	
	public boolean  validate() {
		this.errors.clear();
		if (this.reason == null || this.reason.isEmpty()) {
			this.errors.add("Justification Required");
		}
		
		return ! this.isError();
	}

	public boolean isComplete() {
		return complete;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}
	
	
	
}
