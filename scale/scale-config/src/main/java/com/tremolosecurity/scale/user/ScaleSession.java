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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.tremolosecurity.provisioning.service.util.ProvisioningResult;
import com.tremolosecurity.provisioning.service.util.TremoloUser;
import com.tremolosecurity.saml.Attribute;
import com.tremolosecurity.scale.config.ScaleCommonConfig;
import com.tremolosecurity.scale.config.xml.AppUiConfigType;
import com.tremolosecurity.scale.util.HttpClientInfo;
import com.tremolosecurity.scale.util.UnisonUserData;


@ManagedBean(name = "scaleSession")
@SessionScoped
public class ScaleSession {
	static Logger logger = Logger.getLogger(ScaleSession.class.getName());
	String login;
	
	@ManagedProperty(value="#{scaleCommonConfig}")
	ScaleCommonConfig commonConfig;
	
	CloseableHttpClient http;
	
	
	@PostConstruct
	public void init() {
		try {
			HttpClientInfo httpci = this.commonConfig.createHttpClientInfo();
	
			http  = HttpClients.custom()
			        .setConnectionManager(httpci.getCm()).setDefaultRequestConfig(httpci.getGlobalConfig()).setHostnameVerifier(new AllowAllHostnameVerifier())
			        .build();
			
	
			URL uurl = new URL(commonConfig.getScaleConfig().getServiceConfiguration()
					.getUnisonURL());
			int port = uurl.getPort();
			
			HttpServletRequest request = (HttpServletRequest) FacesContext
					.getCurrentInstance().getExternalContext().getRequest();
			this.login = request.getRemoteUser();
		} catch (Exception e) {
			logger.error("Could not initialize ScaleSession",e);
		}
		
	}
	
	public UnisonUserData loadUserFromUnison(String loginID,
			AttributeData attributeData) throws UnsupportedEncodingException,
			IOException, ClientProtocolException {

		UserObj userObj = new UserObj(attributeData);

		StringBuffer callURL = new StringBuffer();
		callURL.append(
				this.commonConfig.getScaleConfig().getServiceConfiguration()
						.getUnisonURL()
						+ "/services/wf/search?filter=").append(
				URLEncoder.encode(
						"("
								+ this.commonConfig.getScaleConfig()
										.getServiceConfiguration()
										.getLookupAttributeName() + "="
								+ loginID + ")", "UTF-8"));

		HttpGet httpget = new HttpGet(callURL.toString());

		HttpResponse response = http.execute(httpget);
		BufferedReader in = new BufferedReader(new InputStreamReader(response
				.getEntity().getContent()));
		String line = null;
		StringBuffer json = new StringBuffer();
		while ((line = in.readLine()) != null) {
			json.append(line);
		}
		
		
		

		Gson gson = new Gson();
		ProvisioningResult pres = gson.fromJson(json.toString(),
				ProvisioningResult.class);

		if (!pres.isSuccess()) {
			logger.error("Could not load user : '" + pres.getError().getError() + "'");
			return null;
		}

		TremoloUser user = pres.getUser();

		for (Attribute attr : user.getAttributes()) {
			if (attr.getName().equalsIgnoreCase(
					this.commonConfig.getScaleConfig().getUiConfig()
							.getDisplayNameAttribute())) {
				userObj.setDisplayName(attr.getValues().get(0));
			}

			if (attributeData.getLabels().containsKey(attr.getName())) {
				userObj.getAttrs().put(
						attr.getName(),
						new ScaleAttribute(attr.getName(), attributeData
								.getLabels().get(attr.getName()), attr
								.getValues().get(0)));

			}

		}

		
		//determine executed workflows
		callURL.setLength(0);
		callURL.append(
				this.commonConfig.getScaleConfig().getServiceConfiguration()
						.getUnisonURL()
						+ "/services/wf/executed?user=").append(loginID);

		httpget = new HttpGet(callURL.toString());

		response = http.execute(httpget);
		in = new BufferedReader(new InputStreamReader(response
				.getEntity().getContent()));
		line = null;
		json.setLength(0);
		while ((line = in.readLine()) != null) {
			json.append(line);
		}
		
		
		
		
		pres = gson.fromJson(json.toString(),
				ProvisioningResult.class);

		if (!pres.isSuccess()) {
			return null;
		}

		userObj.getExecutedWorkflows().addAll(pres.getWorkflowIds());
		
		return new UnisonUserData(userObj,user);
	}
	

	public ScaleCommonConfig getCommonConfig() {
		return commonConfig;
	}

	public void setCommonConfig(ScaleCommonConfig commonConfig) {
		this.commonConfig = commonConfig;
	}


	public CloseableHttpClient getHttp() {
		return http;
	}

	public String getLogin() {
		return login;
	}

	
	
	
}
