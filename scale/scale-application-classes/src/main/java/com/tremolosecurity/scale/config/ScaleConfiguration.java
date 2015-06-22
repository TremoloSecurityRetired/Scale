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
package com.tremolosecurity.scale.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import com.tremolosecurity.saml.Attribute;
import com.tremolosecurity.scale.config.xml.AppUiConfigType;
import com.tremolosecurity.scale.config.xml.InitParamType;
import com.tremolosecurity.scale.config.xml.ScaleConfigType;

import com.tremolosecurity.scale.ui.UiDecisions;
import com.tremolosecurity.scale.user.AttributeData;
import com.tremolosecurity.scale.user.ScaleAttribute;

@ManagedBean(name="scaleConfiguration")
@ApplicationScoped
public class ScaleConfiguration {

	@ManagedProperty(value="#{scaleCommonConfig}")
	ScaleCommonConfig commonConfig;
	
	
	AttributeData approvalAttrs;
	
	AttributeData attributeData;
	
	
	UiDecisions uiDecisions;
	
	

	@PostConstruct
	public void init() throws Exception {
		
		ScaleConfigType scaleConfig = (ScaleConfigType) this.commonConfig.getScaleConfig();
		
		this.attributeData = new AttributeData(scaleConfig.getUserAttributesConfig());
		
		
		HashMap<String,Attribute> decisionConfig = new HashMap<String,Attribute>();
		AppUiConfigType uiCfg =  scaleConfig.getAppUiConfig();
		String decisionClassName = uiCfg.getUiDecsionClass().getClassName();
		for (InitParamType param : uiCfg.getUiDecsionClass().getInitParams()) {
			Attribute attr = decisionConfig.get(param.getName());
			if (attr == null) {
				attr = new Attribute(param.getName());
				decisionConfig.put(attr.getName(), attr);
			}
			
			attr.getValues().add(param.getValue());
		}
		
		this.uiDecisions = (UiDecisions) Class.forName(decisionClassName).newInstance();
		this.uiDecisions.init(decisionConfig);
		
		this.approvalAttrs = new AttributeData(scaleConfig.getApprovals().getAttributes());
		
		
		
		
	}
	
	
	public AttributeData getAttributeData() {
		return this.attributeData;
	}
	
	public ScaleConfigType getRawConfig() {
		return (ScaleConfigType) commonConfig.getScaleConfig();
	}


	public UiDecisions getUiDecisions() {
		return uiDecisions;
	}
	
	public AttributeData getApprovalsAttributes() {
		return this.approvalAttrs;
	}


	public ScaleCommonConfig getCommonConfig() {
		return commonConfig;
	}


	public void setCommonConfig(ScaleCommonConfig commonConfig) {
		this.commonConfig = commonConfig;
	}


	
	
	
	
	
}
