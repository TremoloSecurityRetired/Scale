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
package com.tremolosecurity.scale.register;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import com.tremolosecurity.provisioning.service.util.TremoloUser;
import com.tremolosecurity.provisioning.service.util.WFCall;
import com.tremolosecurity.saml.Attribute;
import com.tremolosecurity.scale.config.xml.ScaleRegisterConfigType;
import com.tremolosecurity.scale.user.AttributeData;
import com.tremolosecurity.scale.user.ScaleAttribute;

public class SimpleRegisterUser implements CreateRegisterUser {

	
	String workflowName;
	String workflowReason;
	
	@Override
	public WFCall createTremoloUser(List<ScaleAttribute> attributes,
			List<String> errors, AttributeData attributesConfig,
			ScaleRegisterConfigType registerConfig) {
	
		WFCall wfcall = new WFCall();
		wfcall.setName(workflowName);
		wfcall.setReason(this.workflowReason);
		wfcall.setRequestParams(new HashMap<String,Object>());
		wfcall.setUidAttributeName(registerConfig.getServiceConfiguration().getLookupAttributeName());
		TremoloUser user = new TremoloUser();
		
		ArrayList<Attribute> tuAttrs = new ArrayList<Attribute>();
		for (ScaleAttribute attr : attributes) {
			tuAttrs.add(new Attribute(attr.getName(),attr.getValue()));
			if (attr.getName().equalsIgnoreCase(wfcall.getUidAttributeName())) {
				user.setUid(attr.getValue());
			}
		}
		
		user.setAttributes(tuAttrs);
		wfcall.setUser(user);
		
		return wfcall;
		
	}

	@Override
	public void init(HashMap<String, Attribute> config) {
		Attribute attr = null;
		attr = config.get("workflowName");
		if (attr == null) {
			System.err.println("worfklowName not found");
		}
		this.workflowName = attr.getValues().get(0);
		
		attr = config.get("workflowReason");
		if (attr == null) {
			System.err.println("worfklowReason not found");
		}
		this.workflowReason = attr.getValues().get(0);
		
	}



}
