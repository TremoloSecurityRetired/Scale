package com.tremolosecurity.scale.singlerequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
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
import com.tremolosecurity.provisioning.service.util.TremoloUser;
import com.tremolosecurity.provisioning.service.util.WFCall;
import com.tremolosecurity.saml.Attribute;
import com.tremolosecurity.scale.config.ScaleCommonConfig;
import com.tremolosecurity.scale.config.xml.ScaleRegisterConfigType;
import com.tremolosecurity.scale.config.xml.ScaleSingleRequestConfigType;
import com.tremolosecurity.scale.user.AttributeData;
import com.tremolosecurity.scale.user.ScaleAttribute;
import com.tremolosecurity.scale.user.ScaleSession;

@ManagedBean(name="scaleSingleRequestController")
@SessionScoped
public class SingleRequestController {
	static Logger logger = Logger.getLogger(SingleRequestController.class.getName());
	
	@ManagedProperty(value="#{scaleCommonConfig}")
	ScaleCommonConfig commonConfig;
	
	@ManagedProperty(value = "#{scaleSession}")
	ScaleSession scaleSession;
	

	
	String error;

	AttributeData registerUserAttrs;

	private ScaleSingleRequestConfigType singleRequestConfig;

	private boolean singleRequestSubmitted;
	
	private String requestReason;
	
	
	
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

	

	public String getError() {
		return error;
	}

	public boolean isSingleRequestSubmitted() {
		return singleRequestSubmitted;
	}

	public SingleRequestController() {
		
	}
	
	@PostConstruct
	public void init() throws UnsupportedEncodingException, ClientProtocolException, IOException {
		
		this.error = "";
		
		HttpServletRequest request = (HttpServletRequest) FacesContext
				.getCurrentInstance().getExternalContext().getRequest();
		
		this.singleRequestConfig = (ScaleSingleRequestConfigType) this.commonConfig.getScaleConfig();
		
		this.singleRequestSubmitted = false;
		
		
		
	}

	public String getRequestReason() {
		return requestReason;
	}

	public void setRequestReason(String requestReason) {
		this.requestReason = requestReason;
	}
	
	public void makeRequest() {
		HttpServletRequest request = (HttpServletRequest) FacesContext
				.getCurrentInstance().getExternalContext().getRequest();
		
		this.error = "";
		
		if (this.requestReason == null || this.requestReason.isEmpty()) {
			this.error = "Reason is required";
			return;
		}
		
		
		try {
			WFCall wfcall = new WFCall();
			wfcall.setName(this.singleRequestConfig.getWorkflowName());
			wfcall.setReason(this.requestReason);
			wfcall.setRequestParams(new HashMap<String,Object>());
			wfcall.setUidAttributeName(this.singleRequestConfig.getServiceConfiguration().getLookupAttributeName());
			TremoloUser user = new TremoloUser();
			
			ArrayList<Attribute> tuAttrs = new ArrayList<Attribute>();
			tuAttrs.add(new Attribute(this.singleRequestConfig.getServiceConfiguration().getLookupAttributeName(),request.getRemoteUser()));
			
			user.setAttributes(tuAttrs);
			wfcall.setUser(user);
			
			// touch to ensure the session is alive
			StringBuffer callURL = new StringBuffer();
			callURL.append(this.singleRequestConfig.getServiceConfiguration()
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
				this.error = "We could not submit your request at this time, please try again later";
				System.err.println(pres.getError().getError());
				return;
			}
	
			// Execute workflow
			callURL.setLength(0);
			callURL.append(this.singleRequestConfig.getServiceConfiguration()
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
				this.error = "We could not submit your request at this time, please try again later";
				logger.error(pres.getError().getError());
				return;
			} else {
				this.singleRequestSubmitted = true;
			}
		} catch (Throwable t) {
			logger.error("Could not submit request",t);
			this.error = "We could not submit your request at this time, please try again later";
			return;
		}
		
	}

	public ScaleSingleRequestConfigType getSingleRequestConfig() {
		return singleRequestConfig;
	}
	
	
}
