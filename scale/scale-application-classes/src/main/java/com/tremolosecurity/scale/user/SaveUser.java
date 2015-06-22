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

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;

import org.apache.log4j.Logger;

import com.tremolosecurity.scale.config.ScaleConfiguration;
import com.tremolosecurity.scale.ui.util.SaveResult;

@ManagedBean(name="saveUser")
@SessionScoped

public class SaveUser {
	
	static Logger logger = Logger.getLogger(SaveUser.class.getName());
	
	@ManagedProperty(value="#{saveResults}")
	SaveResult saveResult;
	
	@ManagedProperty(value="#{scaleConfiguration}")
	ScaleConfiguration scaleConfig;
	
	@ManagedProperty(value="#{scaleUser}")
	ScaleUser scaleUser;
	
	ArrayList<ScaleAttribute> attributes;
	
	
	public SaveUser() {
		//System.out.println("CREATING ATTRIBUTES");
		
		
		this.attributes = new ArrayList<ScaleAttribute>();
	}
	
	@PostConstruct
	public void postConstruct() {
		
	}
	
	public void preRender(ComponentSystemEvent event) {
		
		if (! FacesContext.getCurrentInstance().isPostback()) {
			
			this.attributes = new ArrayList<ScaleAttribute>();
			for (ScaleAttribute attr : this.scaleUser.getAttributes()) {
				this.attributes.add(new ScaleAttribute(attr.getName(),attr.getLabel(),attr.getValue()));
			}
			this.saveResult.reset();
		} else {
			
		}
	}
	
	public String saveUser() throws Exception {
		
		this.saveResult.reset();
		
		
		
		
		for (ScaleAttribute attr : this.attributes) {
			if (logger.isDebugEnabled()) logger.debug(attr.getName() + "/" + this.scaleConfig.getAttributeData().isRequired(attr.getName()));
			if (this.scaleConfig.getAttributeData().isRequired(attr.getName()) && attr.getValue().trim().isEmpty()) {
				this.saveResult.setError(true);
				this.saveResult.getErrors().add(attr.getLabel() + " is a required field");
			} else if (this.scaleConfig.getAttributeData().getMinSizes().get(attr.getName()) > 0 && attr.getValue().length() < this.scaleConfig.getAttributeData().getMinSizes().get(attr.getName())) {
				this.saveResult.setError(true);
				this.saveResult.getErrors().add(attr.getLabel() + " must have at least " + this.scaleConfig.getAttributeData().getMinSizes().get(attr.getName()) + " characters");
			} else if (this.scaleConfig.getAttributeData().getMaxSizes().get(attr.getName()) > 0 && attr.getValue().length() > this.scaleConfig.getAttributeData().getMaxSizes().get(attr.getName())) {
				this.saveResult.setError(true);
				this.saveResult.getErrors().add(attr.getLabel() + " must not have more than " + this.scaleConfig.getAttributeData().getMaxSizes().get(attr.getName()) + " characters");
			} else if (this.scaleConfig.getAttributeData().getRegExs().get(attr.getName()) != null && ! this.scaleConfig.getAttributeData().getRegExs().get(attr.getName()).matcher(attr.getValue()).matches()) {
				this.saveResult.setError(true);
				this.saveResult.getErrors().add(this.scaleConfig.getAttributeData().getFailErrMsgs().get(attr.getName()));
			}
		}
		
		if (logger.isDebugEnabled()) logger.debug("Running workflow? : " + this.saveResult.isError());
		
		if (! this.saveResult.isError()) {
			String result = this.scaleUser.saveUser(this);
			if (logger.isDebugEnabled()) logger.debug("Result - '" + result + "'");
			if (result != null) {
				this.saveResult.setError(true);
				this.saveResult.getErrors().add(result);
			}
		}
		
		
		if (! this.saveResult.isError()) {
			this.saveResult.setSaved(true);
		} else {
			
			this.saveResult.setSaved(false);
		}
		
		if (this.saveResult.isSaved()) {
			return "userUpdated.xhtml";
		} else {
			return "";
		}
	}

	public SaveResult getSaveResult() {
		return saveResult;
	}

	public void setSaveResult(SaveResult saveResult) {
		this.saveResult = saveResult;
	}

	public ScaleConfiguration getScaleConfig() {
		return scaleConfig;
	}

	public void setScaleConfig(ScaleConfiguration scaleConfig) {
		this.scaleConfig = scaleConfig;
	}

	public ScaleUser getScaleUser() {
		return scaleUser;
	}

	public void setScaleUser(ScaleUser scaleUser) {
		this.scaleUser = scaleUser;
	}

	public ArrayList<ScaleAttribute> getAttributes() {
		return attributes;
	}
	
	
}
