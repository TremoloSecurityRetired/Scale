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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.tremolosecurity.provisioning.service.util.ProvisioningResult;
import com.tremolosecurity.provisioning.service.util.WFCall;
import com.tremolosecurity.saml.Attribute;
import com.tremolosecurity.scale.config.ScaleCommonConfig;
import com.tremolosecurity.scale.config.xml.InitParamType;
import com.tremolosecurity.scale.config.xml.ScaleRegisterConfigType;
import com.tremolosecurity.scale.user.AttributeData;
import com.tremolosecurity.scale.user.ScaleAttribute;
import com.tremolosecurity.scale.user.ScaleSession;
import com.tremolosecurity.scale.user.UserObj;
import com.tremolosecurity.scale.util.UnisonUserData;

@ManagedBean(name="scaleRegisterController")
@SessionScoped
public class RegisterController {
	
	static Logger logger = Logger.getLogger(RegisterController.class.getName());
	
	ScaleRegisterConfigType registerConfig;
	
	@ManagedProperty(value="#{scaleCommonConfig}")
	ScaleCommonConfig commonConfig;
	
	@ManagedProperty(value = "#{scaleSession}")
	ScaleSession scaleSession;
	
	
	CreateRegisterUser createUserImpl;
	
	List<String> errors;

	AttributeData registerUserAttrs;
	
	ArrayList<ScaleAttribute> attrs;
	
	private boolean acceptTermsAndConditions;
	
	private boolean registerSubmitted;
	
	private String recaptcha;

	public List<String> getErrors() {
		return errors;
	}

	public void setErrors(List<String> errors) {
		this.errors = errors;
	}



	public boolean isRegisterSubmitted() {
		return registerSubmitted;
	}

	public void setRegisterSubmitted(boolean registerSubmitted) {
		this.registerSubmitted = registerSubmitted;
	}
	
	
	
	
	public ScaleCommonConfig getCommonConfig() {
		return commonConfig;
	}

	public void setCommonConfig(ScaleCommonConfig commonConfig) {
		this.commonConfig = commonConfig;
	}

	public ScaleSession getScaleSession() {
		return scaleSession;
	}

	public void setScaleSession(ScaleSession scaleSession) {
		this.scaleSession = scaleSession;
	}



	@PostConstruct
	public void init() {
		this.acceptTermsAndConditions = false;
		this.errors = new ArrayList<String>();
		
		HttpServletRequest request = (HttpServletRequest) FacesContext
				.getCurrentInstance().getExternalContext().getRequest();
		
		this.registerConfig = (ScaleRegisterConfigType) this.commonConfig.getScaleConfig();
		
		this.registerSubmitted = false;
		this.errors = null;
		
		//Lookup the account
		this.registerUserAttrs = new AttributeData(this.registerConfig.getAttributes());
		
		this.attrs = new ArrayList<ScaleAttribute>();
		if (logger.isDebugEnabled()) logger.debug("Creating attrs");
		for (String name : this.registerUserAttrs.getAttributeNames()) {
			if (logger.isDebugEnabled()) logger.debug("attr : '" + name + "'");
			ScaleAttribute scaleAttr = new ScaleAttribute();
			scaleAttr.setName(name);
			scaleAttr.setLabel(this.registerUserAttrs.getLabels().get(name));
			scaleAttr.setText(this.registerUserAttrs.getTextControls().contains(name));
			scaleAttr.setList(this.registerUserAttrs.getListControls().contains(name));
			scaleAttr.setCheckbox(this.registerUserAttrs.getCheckBoxControls().contains(name));
			scaleAttr.setListValues(this.registerUserAttrs.getListSources().get(name));
			this.attrs.add(scaleAttr);
		}
		
		try {
			this.createUserImpl = (CreateRegisterUser) Class.forName(this.registerConfig.getWorkflowConfiguration().getClassName()).newInstance();
		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		HashMap<String,Attribute> initParams = new HashMap<String,Attribute>();
		
		for (InitParamType init : this.registerConfig.getWorkflowConfiguration().getInitParams()) {
			Attribute attr = initParams.get(init.getName());
			if (attr == null) {
				attr = new Attribute(init.getName());
				initParams.put(init.getName(), attr);
			}
			
			attr.getValues().add(init.getValue());
		}
		
		this.createUserImpl.init(initParams);
		
		
	}

	public ScaleRegisterConfigType getRegisterConfig() {
		return registerConfig;
	}

	public AttributeData getRegisterUserAttrs() {
		return registerUserAttrs;
	}

	public ArrayList<ScaleAttribute> getAttrs() {
		if (logger.isDebugEnabled()) logger.debug("Retrieving attrs : " + this.attrs);
		return attrs;
	}

	public boolean isAcceptTermsAndConditions() {
		return acceptTermsAndConditions;
	}

	public void setAcceptTermsAndConditions(boolean acceptTermsAndConditions) {
		this.acceptTermsAndConditions = acceptTermsAndConditions;
	}

	public String getRecaptcha() {
		return "";
	}

	public void setRecaptcha(String recaptcha) {
		this.recaptcha = recaptcha;
	}
	
	public String newReCaptcha() {
		return "";
	}
	
	public String register() {
		if (this.errors == null) {
			this.errors = new ArrayList<String>();
		}
		this.errors.clear();
		
		for (ScaleAttribute attr : this.attrs) {
			
			if (this.registerUserAttrs.isRequired(attr.getName()) && attr.getValue().trim().isEmpty()) {
				this.errors.add(attr.getLabel() + " is a required field");
			} else if (this.registerUserAttrs.getMinSizes().get(attr.getName()) > 0 && attr.getValue().length() < this.registerUserAttrs.getMinSizes().get(attr.getName())) {
				this.errors.add(attr.getLabel() + " must have at least " + this.registerUserAttrs.getMinSizes().get(attr.getName()) + " characters");
			} else if (this.registerUserAttrs.getMaxSizes().get(attr.getName()) > 0 && attr.getValue().length() > this.registerUserAttrs.getMaxSizes().get(attr.getName())) {
				
				this.errors.add(attr.getLabel() + " must not have more than " + this.registerUserAttrs.getMaxSizes().get(attr.getName()) + " characters");
			} else if (this.registerUserAttrs.getRegExs().get(attr.getName()) != null && ! this.registerUserAttrs.getRegExs().get(attr.getName()).matcher(attr.getValue()).matches()) {
				
				this.errors.add(this.registerUserAttrs.getFailErrMsgs().get(attr.getName()));
			}
		}
		
		if (this.registerConfig.getTermsAndConditions() != null && ! this.registerConfig.getTermsAndConditions().isEmpty() && ! this.acceptTermsAndConditions) {
			errors.add("You must accept the terms and conditions");
		}
		
		if (this.registerConfig.isRecaptcha()) {
			HttpServletRequest request = (HttpServletRequest) FacesContext
					.getCurrentInstance().getExternalContext().getRequest();
			String fromSession = (String) request.getSession().getAttribute("captchaSessionKeyName_registrationCheck");
			if (logger.isDebugEnabled()) logger.debug("From session : '" + fromSession + "'");
			if (logger.isDebugEnabled()) logger.debug("from captcha : '" + this.recaptcha + "'");
			
			
			Enumeration enumer = request.getSession().getAttributeNames();
			while (enumer.hasMoreElements()) {
				String name = (String) enumer.nextElement();
				if (logger.isDebugEnabled()) logger.debug(name + "='" + request.getSession().getAttribute(name) + "'");
			}
			
			if (logger.isDebugEnabled()) logger.debug(this.recaptcha + " / " + fromSession + " / " + fromSession.equals(this.recaptcha));
			
			if (fromSession == null || ! fromSession.equals(this.recaptcha)) {
				errors.add("Invalid code");
			}
		}
		
		if (errors.size() > 0) {
			return "";
		}
		
		for (String uniqueAttr : this.registerConfig.getUniqueAttribute()) {
			if (logger.isDebugEnabled()) logger.debug("Checking uniqueness of  : '" + uniqueAttr + "'");
			try {
				if (! this.checkAttributeUnique(uniqueAttr)) {
					errors.add(this.registerUserAttrs.getLabels().get(uniqueAttr) + " is not available");
				}
			} catch (Exception e) {
				e.printStackTrace();
				errors.add("We are unable to process your request at this time");
			}
		}
	
		WFCall workflow = null;
		if (errors.size() == 0) {
			workflow = this.createUserImpl.createTremoloUser(attrs, errors, registerUserAttrs, registerConfig);
		}
		
		if (errors.size() == 0) {
			try {
				this.executeWorkflow(workflow);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		
		if (errors.size() == 0) {
			
			this.registerSubmitted = true;
		}
		
		return "";
	}

	private void executeWorkflow(WFCall wfcall) throws Exception {
		// touch to ensure the session is alive
				StringBuffer callURL = new StringBuffer();
				callURL.append(this.registerConfig.getServiceConfiguration()
						.getUnisonURL()
						+ "/services/wf/login");

				HttpGet httpget = new HttpGet(callURL.toString());

				HttpResponse response = scaleSession.getHttp().execute(httpget);
				BufferedReader in = new BufferedReader(new InputStreamReader(response
						.getEntity().getContent()));
				String line = null;
				StringBuffer json = new StringBuffer();
				while ((line = in.readLine()) != null) {
					json.append(line);
				}

				httpget.abort();

				Gson gson = new Gson();
				ProvisioningResult pres = gson.fromJson(json.toString(),
						ProvisioningResult.class);
				if (!pres.isSuccess()) {
					errors.add("We could not submit your request at this time, please try again later");
					System.err.println(pres.getError().getError());
					return;
				}

				// Execute workflow
				callURL.setLength(0);
				callURL.append(this.registerConfig.getServiceConfiguration()
						.getUnisonURL()
						+ "/services/wf/execute");
				if (logger.isDebugEnabled()) logger.debug("URL for wf : '" + callURL.toString() + "'");
				String sjson = gson.toJson(wfcall);
				HttpPost post = new HttpPost(callURL.toString());
				List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
				urlParameters.add(new BasicNameValuePair("wfcall", sjson));
				post.setEntity(new UrlEncodedFormEntity(urlParameters));

				response = scaleSession.getHttp().execute(post);
				in = new BufferedReader(new InputStreamReader(response.getEntity()
						.getContent()));
				line = null;
				json.setLength(0);
				while ((line = in.readLine()) != null) {
					json.append(line);
				}

				pres = gson.fromJson(json.toString(), ProvisioningResult.class);
				if (!pres.isSuccess()) {
					errors.add("We could not submit your request at this time, please try again later");
					System.err.println(pres.getError().getError());
					return;
				}
	}
	
	private boolean checkAttributeUnique(String name) throws Exception {
		
		ScaleAttribute attr = null;
		
		for (ScaleAttribute a : this.attrs) {
			if (a.getName().equalsIgnoreCase(name)) {
				attr = a;
				break;
			}
			
		}
		
		if (attr == null) {
			throw new Exception("Attribute '" + name + "' not configured");
		}
		
		StringBuffer callURL = new StringBuffer();
		callURL.append(
				this.commonConfig.getScaleConfig().getServiceConfiguration()
						.getUnisonURL()
						+ "/services/wf/search?filter=").append(
				URLEncoder.encode(
						"("
								+ name + "="
								+ attr.getValue() + ")", "UTF-8"));

		HttpGet httpget = new HttpGet(callURL.toString());

		HttpResponse response = this.scaleSession.getHttp().execute(httpget);
		BufferedReader in = new BufferedReader(new InputStreamReader(response
				.getEntity().getContent()));
		String line = null;
		StringBuffer json = new StringBuffer();
		while ((line = in.readLine()) != null) {
			json.append(line);
		}

		if (logger.isDebugEnabled()) logger.debug("Response from server : '" + json.toString() + "'");
		
		Gson gson = new Gson();
		ProvisioningResult pres = gson.fromJson(json.toString(),
				ProvisioningResult.class);

		return ! pres.isSuccess() || pres.getUser() == null;
	}
	
}
