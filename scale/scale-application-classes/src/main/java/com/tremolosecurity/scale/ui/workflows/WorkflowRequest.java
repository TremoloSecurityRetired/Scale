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
package com.tremolosecurity.scale.ui.workflows;

import com.tremolosecurity.provisioning.service.util.WFDescription;

public class WorkflowRequest {
	WFDescription wf;
	String reason;
	
	public WorkflowRequest(WFDescription wf) {
		this.wf = wf;
		this.reason = "";
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

	public WFDescription getWf() {
		return wf;
	}
	
	
}
