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
package com.tremolosecurity.scale.passwordreset;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;

import com.tremolosecurity.saml.Attribute;
import com.tremolosecurity.scale.config.ScaleCommonConfig;
import com.tremolosecurity.scale.config.xml.InitParamType;
import com.tremolosecurity.scale.config.xml.ScalePasswordResetConfigType;

@ManagedBean(name="scaleResetValidator")
@ApplicationScoped
public class ValidationService {
	@ManagedProperty(value="#{scaleCommonConfig}")
	ScaleCommonConfig commonConfig;
	
	PasswordValidator pwdValidator;
	
	@PostConstruct
	public void init()  {
		try {
			ScalePasswordResetConfigType resetCfg = (ScalePasswordResetConfigType) commonConfig.getScaleConfig();
			String className = resetCfg.getPasswordValidation().getClassName();
			pwdValidator = (PasswordValidator) Class.forName(className).newInstance();
			HashMap<String,Attribute> params = new HashMap<String,Attribute>();
			for (InitParamType ipt : resetCfg.getPasswordValidation().getInitParams()) {
				Attribute attr = params.get(ipt.getName());
				if (attr == null) {
					attr = new Attribute(ipt.getName());
					params.put(ipt.getName(), attr);
				}
				attr.getValues().add(ipt.getValue());
				
			
			}
			
			
			pwdValidator.init(params);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ScaleCommonConfig getCommonConfig() {
		return commonConfig;
	}

	public void setCommonConfig(ScaleCommonConfig commonConfig) {
		this.commonConfig = commonConfig;
	}
	
	
	public PasswordValidator getPasswordValidator() {
		return this.pwdValidator;
	}

}
