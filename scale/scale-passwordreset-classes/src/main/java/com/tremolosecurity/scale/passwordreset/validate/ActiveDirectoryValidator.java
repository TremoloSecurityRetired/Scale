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
package com.tremolosecurity.scale.passwordreset.validate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.tremolosecurity.saml.Attribute;
import com.tremolosecurity.scale.user.ScaleAttribute;
import com.tremolosecurity.scale.user.UserObj;

public class ActiveDirectoryValidator extends BasicValidator {
	List<String> attrsToCheck;
	
	@Override
	public void init(HashMap<String, Attribute> initParams) throws Exception {
		super.init(initParams);
		this.attrsToCheck = new ArrayList<String>();
		Attribute attr = initParams.get("attributesToCheck");
		if (attr != null) {
			this.attrsToCheck.addAll(attr.getValues());
		}
	}

	@Override
	public List<String> validate(String password, UserObj user) {
		List<String> errors =  super.validate(password, user);
		for (String attrName : this.attrsToCheck) {
			ScaleAttribute attr = user.getAttrs().get(attrName);
			if (attr != null) {
				if (this.hasWord(attr.getValue(), password)) {
					errors.add("Your new password must not contain more then 3 consecutive characters from your " + attr.getLabel());
				}
			}
			
			
		}
		
		return errors;
	}
	
	private boolean hasWord(String val,String password) {
		val = val.toLowerCase();
		password = password.toLowerCase();
		
		
		for (int i=0;(i<val.length()-3);i++) {
			if (password.contains(val.subSequence(i, i + 3))) {
				
				return true;
			}
		}
		
		return false;
	}

}
