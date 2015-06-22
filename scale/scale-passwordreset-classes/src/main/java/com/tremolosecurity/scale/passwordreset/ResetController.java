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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
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
import com.google.gson.JsonSyntaxException;
import com.tremolosecurity.provisioning.service.util.ProvisioningResult;
import com.tremolosecurity.provisioning.service.util.TremoloUser;
import com.tremolosecurity.provisioning.service.util.WFCall;
import com.tremolosecurity.saml.Attribute;
import com.tremolosecurity.scale.config.ScaleCommonConfig;
import com.tremolosecurity.scale.config.xml.ScalePasswordResetConfigType;
import com.tremolosecurity.scale.user.AttributeData;
import com.tremolosecurity.scale.user.ScaleAttribute;
import com.tremolosecurity.scale.user.ScaleSession;
import com.tremolosecurity.scale.user.UserObj;
import com.tremolosecurity.scale.util.UnisonUserData;

@ManagedBean(name="scaleResetController")
@SessionScoped
public class ResetController {
	static Logger logger = Logger.getLogger(ResetController.class.getName());
	@ManagedProperty(value="#{scaleCommonConfig}")
	ScaleCommonConfig commonConfig;
	
	@ManagedProperty(value = "#{scaleSession}")
	ScaleSession scaleSession;
	
	@ManagedProperty("#{scaleResetValidator}")
	ValidationService passwordValidationService;
	
	ScalePasswordResetConfigType resetCfg;
	
	boolean resetSubmitted;
	
	String password1;
	String password2;
	
	List<String> errors;

	private ArrayList<ScaleAttribute> attributes;
	AttributeData attrs;

	private String login;

	private String displayName;

	private UserObj user;
	
	
	
	@PostConstruct
	public void init()  {
		try {
			HttpServletRequest request = (HttpServletRequest) FacesContext
					.getCurrentInstance().getExternalContext().getRequest();
			
			this.resetCfg = (ScalePasswordResetConfigType) commonConfig.getScaleConfig();
			this.resetSubmitted = false;
			this.errors = null;
			
			//Lookup the account
			this.attrs = new AttributeData(resetCfg.getAttributes());
			this.attributes = new ArrayList<ScaleAttribute>();
			
			this.login = request.getRemoteUser();
	
			
			
			UnisonUserData userData = this.scaleSession.loadUserFromUnison(this.login,this.attrs);
			
			this.user = userData.getUserObj();
			
			this.displayName = userData.getUserObj().getDisplayName();
			
			this.attributes = userData.getUserObj().getAttributes();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public String resetPassword() {
		this.errors = null;
		
		
		ArrayList<String> lerrors = new ArrayList<String>();
		
		if (! password1.equals(password2)) {
			lerrors.add("Passwords not equal");
		} 
		
		List<String> valErrors = this.passwordValidationService.getPasswordValidator().validate(password1, this.user);
		if (valErrors != null) {
			lerrors.addAll(valErrors);
		}
		
		if (lerrors.size() == 0) {
			try {
				TremoloUser user = new TremoloUser();
				user.setUid(this.scaleSession.getLogin());
				user.getAttributes().add(new Attribute(this.commonConfig.getScaleConfig().getServiceConfiguration().getLookupAttributeName(),this.scaleSession.getLogin()));
				user.setUserPassword(password1);
				WFCall wfcall = new WFCall();
				wfcall.setUidAttributeName(this.commonConfig.getScaleConfig().getServiceConfiguration().getLookupAttributeName());
				wfcall.setUser(user);
				wfcall.setName(this.resetCfg.getWorkflowName());
				

				// touch to ensure the session is alive
				StringBuffer callURL = new StringBuffer();
				callURL.append(this.commonConfig.getScaleConfig().getServiceConfiguration()
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

					return "Could not connect to Unison";
				}

				// Execute workflow
				callURL.setLength(0);
				callURL.append(this.commonConfig.getScaleConfig().getServiceConfiguration()
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
					logger.error("Error : '" + pres.getError().getError() + "'");
					return "There was a problem updating your profile, please contact the system administrator for help";
				}
			} catch (JsonSyntaxException | IllegalStateException | IOException e) {
				lerrors.add("There was a problem saving the password, please contact your system administrator for help");
				e.printStackTrace();
			}
		}
		
		if (lerrors.size() > 0) {
			this.errors = lerrors;
			this.resetSubmitted = false;
		} else {
			this.resetSubmitted = true;
		}
		
		
		return "";
	}
	
	public ScaleCommonConfig getCommonConfig() {
		return commonConfig;
	}

	public void setCommonConfig(ScaleCommonConfig commonConfig) {
		this.commonConfig = commonConfig;
	}



	public boolean isResetSubmitted() {
		return resetSubmitted;
	}



	public String getPassword1() {
		return "";
	}



	public void setPassword1(String password1) {
		this.password1 = password1;
	}



	public String getPassword2() {
		return "";
	}



	public void setPassword2(String password2) {
		this.password2 = password2;
	}	
	
	public List<String> getErrors() {
		return this.errors;
	}

	public ScaleSession getScaleSession() {
		return scaleSession;
	}

	public void setScaleSession(ScaleSession scaleSession) {
		this.scaleSession = scaleSession;
	}
	
	public String getDisplayName() {
		return this.displayName;
	}

	public ValidationService getPasswordValidationService() {
		return passwordValidationService;
	}

	public void setPasswordValidationService(
			ValidationService passwordValidationService) {
		this.passwordValidationService = passwordValidationService;
	}
	
	
	
}
