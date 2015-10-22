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
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.ConfigurableNavigationHandler;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.apache.myfaces.custom.tree2.TreeModel;
import org.apache.myfaces.custom.tree2.TreeModelBase;
import org.apache.myfaces.custom.tree2.TreeNode;
import org.apache.myfaces.custom.tree2.TreeNodeBase;

import com.google.gson.Gson;
import com.tremolosecurity.provisioning.service.util.ApprovalDetails;
import com.tremolosecurity.provisioning.service.util.ApprovalSummary;
import com.tremolosecurity.provisioning.service.util.Organization;
import com.tremolosecurity.provisioning.service.util.PortalURL;
import com.tremolosecurity.provisioning.service.util.ProvisioningResult;
import com.tremolosecurity.provisioning.service.util.ReportInformation;
import com.tremolosecurity.provisioning.service.util.TremoloUser;
import com.tremolosecurity.provisioning.service.util.WFCall;
import com.tremolosecurity.provisioning.service.util.WFDescription;
import com.tremolosecurity.saml.Attribute;
import com.tremolosecurity.scale.config.ScaleConfiguration;
import com.tremolosecurity.scale.config.xml.AppUiConfigType;
import com.tremolosecurity.scale.config.xml.ScaleConfigType;
import com.tremolosecurity.scale.ui.orgs.OrgTreeNode;
import com.tremolosecurity.scale.ui.util.SaveResult;
import com.tremolosecurity.scale.ui.workflows.WorkflowRequest;
import com.tremolosecurity.scale.util.HttpClientInfo;
import com.tremolosecurity.scale.util.UnisonUserData;

@ManagedBean(name = "scaleUser")
@SessionScoped
public class ScaleUser {
	static Logger logger = Logger.getLogger(ScaleUser.class.getName());
	@ManagedProperty(value = "#{scaleConfiguration}")
	ScaleConfiguration scaleConfig;

	@ManagedProperty(value = "#{scaleSession}")
	ScaleSession scaleSession;
	
	String login;
	String displayName;

	
	ArrayList<String> groups;

	

	private TreeModel orgTree;
	private Organization currentOrg;
	private List<WFDescription> currentWorkflows;
	private HashMap<String, WorkflowRequest> cart;
	private HashMap<String, List<PortalURL>> urlsByOrgs;
	private List<PortalURL> portalURLs;
	private List<PortalURL> currentURLs;
	private HashMap<String, List<ReportInformation>> reports;

	private ArrayList<ScaleAttribute> attributes;

	private HashSet<String> executedWorkflows;

	public ScaleUser() {
		this.attributes = new ArrayList<ScaleAttribute>();
		this.groups = new ArrayList<String>();
		this.cart = new HashMap<String, WorkflowRequest>();
		this.urlsByOrgs = new HashMap<String, List<PortalURL>>();
		this.portalURLs = new ArrayList<PortalURL>();
		this.reports = new HashMap<String,List<ReportInformation>>();
	}

	@PostConstruct
	public void init()  {
		try {
			HttpServletRequest request = (HttpServletRequest) FacesContext
					.getCurrentInstance().getExternalContext().getRequest();
			this.login = request.getRemoteUser();
	
			
			
	
			UserObj userObj = loadUserFromUnison(this.login,
					scaleConfig.getAttributeData());
	
			this.displayName = userObj.getDisplayName();
			this.groups = userObj.getGroups();
			this.attributes = userObj.getAttributes();
	
			this.orgTree = null;
	
			this.loadURLs();
			this.loadReports();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void loadURLs() throws Exception {
		StringBuffer callURL = new StringBuffer();
		callURL.append(
				scaleConfig.getRawConfig().getServiceConfiguration()
						.getUnisonURL()
						+ "/services/portal/urls?uid=")
				.append(this.login)
				.append("&uidAttr=")
				.append(scaleConfig.getRawConfig().getServiceConfiguration()
						.getLookupAttributeName());

		HttpGet httpget = new HttpGet(callURL.toString());

		HttpResponse response = scaleSession.getHttp().execute(httpget);
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

		if (pres.isSuccess()) {
			for (PortalURL url : pres.getPortalURLs().getUrls()) {
				this.portalURLs.add(url);
				List<PortalURL> orgUrls = this.urlsByOrgs.get(url.getOrg());
				if (orgUrls == null) {
					orgUrls = new ArrayList<PortalURL>();
					this.urlsByOrgs.put(url.getOrg(), orgUrls);
				}

				orgUrls.add(url);
			}
		} else {
			throw new Exception("Error loading portal urls : "
					+ pres.getError().getError());
		}

	}
	
	private void loadReports() throws Exception {
		StringBuffer callURL = new StringBuffer();
		callURL.append(
				scaleConfig.getRawConfig().getServiceConfiguration()
						.getUnisonURL()
						+ "/services/reports/list?uid=")
				.append(this.login)
				.append("&uidAttr=")
				.append(scaleConfig.getRawConfig().getServiceConfiguration()
						.getLookupAttributeName());

		HttpGet httpget = new HttpGet(callURL.toString());

		HttpResponse response = scaleSession.getHttp().execute(httpget);
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

		if (pres.isSuccess()) {
			for (ReportInformation ri : pres.getReportsList().getReports()) {
				List<ReportInformation> reports = this.reports.get(ri.getOrgID());
				if (reports == null) {
					reports = new ArrayList<ReportInformation>();
					this.reports.put(ri.getOrgID(), reports);
				}
				
				reports.add(ri);
			}
			
		} else {
			throw new Exception("Error loading portal urls : "
					+ pres.getError().getError());
		}

	}

	

	public String saveUser(SaveUser toSave) throws Exception {
		TremoloUser user = new TremoloUser();
		user.setUid(this.getLogin());

		for (ScaleAttribute attr : toSave.getAttributes()) {
			Attribute uattr = new Attribute(attr.getName());
			uattr.getValues().add(attr.getValue());
			user.getAttributes().add(uattr);
		}

		WFCall wfcall = new WFCall();
		wfcall.setUidAttributeName(this.getScaleConfig().getRawConfig()
				.getServiceConfiguration().getLookupAttributeName());
		wfcall.setUser(user);
		wfcall.setName(this.getScaleConfig().getRawConfig().getWorkflows()
				.getSaveUserProfileWorkflowName());

		// touch to ensure the session is alive
		StringBuffer callURL = new StringBuffer();
		callURL.append(scaleConfig.getRawConfig().getServiceConfiguration()
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
		callURL.append(scaleConfig.getRawConfig().getServiceConfiguration()
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

		return null;

	}
	
	public UserObj loadUserFromUnison(String loginID,
			AttributeData attributeData) throws UnsupportedEncodingException,
			IOException, ClientProtocolException {
		UnisonUserData userData = scaleSession.loadUserFromUnison(loginID, attributeData);
		
		if (userData == null) {
			logger.warn("No user data, returning null");
			return null;
		}
		
		
		
		UserObj userObj = userData.getUserObj();
		TremoloUser user = userData.getFromUnison();
		
		this.executedWorkflows = userObj.getExecutedWorkflows();
		
		AppUiConfigType uiCfg = scaleConfig.getRawConfig().getAppUiConfig();
		
		if (uiCfg.isUseGenericGroups()) {
			userObj.getGroups().addAll(user.getGroups());
		} else {
			Attribute groupsAttr = null;
			for (Attribute attr : user.getAttributes()) {
				if (attr.getName().equalsIgnoreCase(
						uiCfg
								.getGroupsAttribute())) {
					groupsAttr = attr;
					break;

				}
			}

			if (groupsAttr != null) {
				userObj.getGroups().addAll(groupsAttr.getValues());
			} else {
				HttpServletRequest request = (HttpServletRequest) FacesContext
						.getCurrentInstance().getExternalContext().getRequest();
				Attribute attr = (Attribute) request.getAttribute(uiCfg.getGroupsAttribute());
				if (attr != null) {
					userObj.getGroups().addAll(attr.getValues());
				}
			}
		}
		
		return userObj;
	}

	public String getLogin() {
		return login;
	}

	public String getDisplayName() {
		return displayName;
	}

	public ArrayList<ScaleAttribute> getAttributes() {

		return attributes;
	}

	public ArrayList<String> getGroups() {
		return groups;
	}

	public ScaleConfiguration getScaleConfig() {
		return scaleConfig;
	}

	public void setScaleConfig(ScaleConfiguration scaleConfig) {
		this.scaleConfig = scaleConfig;
	}

	private void addChildOrgs(Organization org, TreeNode root) {

		for (Organization o : org.getSubOrgs()) {
			if (logger.isDebugEnabled()) logger.debug("Loading org : '" + o.getName() + "'");
			if (o.getName() != null && ! o.getName().isEmpty()) {
				if (logger.isDebugEnabled()) logger.debug("not empty, adding children");
				OrgTreeNode node = new OrgTreeNode(o);
			
				root.getChildren().add(node);
				
				addChildOrgs(o, node);
			}
			
		}
	}

	public TreeModel getUserOrgs() throws Exception {
		if (this.orgTree == null) {
			// load orgs
			StringBuffer callURL = new StringBuffer();
			callURL.append(scaleConfig.getRawConfig().getServiceConfiguration()
					.getUnisonURL()
					+ "/services/wf/orgs?uid="
					+ URLEncoder.encode(this.login, "UTF-8")
					+ "&uidAttr="
					+ URLEncoder.encode(
							this.scaleConfig.getRawConfig()
									.getServiceConfiguration()
									.getLookupAttributeName(), "UTF-8"));

			HttpGet httpget = new HttpGet(callURL.toString());

			HttpResponse response = scaleSession.getHttp().execute(httpget);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));
			String line = null;
			StringBuffer json = new StringBuffer();
			while ((line = in.readLine()) != null) {
				json.append(line);
			}

			httpget.abort();

			Gson gson = new Gson();
			ProvisioningResult pres = gson.fromJson(json.toString(),
					ProvisioningResult.class);

			if (pres.isSuccess()) {
				Organization org = pres.getOrg();

				TreeNode root = new OrgTreeNode(org);
				addChildOrgs(org, root);
				this.orgTree = new TreeModelBase(root);
				this.currentOrg = org;
				loadWorkflows();
			} else {
				Organization o = new Organization();
				o.setName("Root");
				o.setId("");
				o.setDescription("No Organizations Have Been Configured");
				TreeNode n = new OrgTreeNode(o);
				this.orgTree = new TreeModelBase(n);
				this.currentOrg = o;
				this.currentWorkflows = new ArrayList<WFDescription>();
			}

		}

		return this.orgTree;

	}

	public String chooseOrg(OrgTreeNode node) throws Exception {

		this.currentOrg = node.getOrganization();

		loadWorkflows();

		return "";
	}

	private void loadWorkflows() throws UnsupportedEncodingException,
			IOException, ClientProtocolException {
		// Get available workflows
		StringBuffer callURL = new StringBuffer();
		callURL.append(scaleConfig.getRawConfig().getServiceConfiguration()
				.getUnisonURL()
				+ "/services/wf/list?uuid="
				+ URLEncoder.encode(this.currentOrg.getId(), "UTF-8"));

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

		if (pres.isSuccess()) {
			this.currentWorkflows = pres.getWfDescriptions().getWorkflows();
		} else {
			this.currentWorkflows = new ArrayList<WFDescription>();
			logger.error("Problem loading workflows : "
					+ pres.getError().getError());
		}
	}

	
	public List<ReportInformation> getReports() {
		return this.reports.get(this.currentOrg.getId());
	}
	
	public Organization getCurrentOrg() {
		return this.currentOrg;
	}

	public List<WFDescription> getWorkflows() {
		return this.currentWorkflows;
	}

	public String toggleCart(WFDescription wf) {
		if (this.cart.containsKey(wf.getName())) {
			this.cart.remove(wf.getName());
		} else {
			this.cart.put(wf.getName(), new WorkflowRequest(wf));
		}

		return "";
	}

	public String determineLabel(WFDescription wf) {
		if (this.cart.containsKey(wf.getName())) {
			return "Remove From Cart";
		} else {
			return "Add To Cart";
		}
	}

	public HashMap<String, WorkflowRequest> getCart() {
		return this.cart;
	}

	public List<Map.Entry<String, WorkflowRequest>> getCartAsList() {
		List<Map.Entry<String, WorkflowRequest>> list = new ArrayList<Map.Entry<String, WorkflowRequest>>();
		list.addAll(this.cart.entrySet());
		return list;
	}

	public String saveRequests() {
		SaveResult res = new SaveResult();

		HttpServletRequest req = (HttpServletRequest) FacesContext
				.getCurrentInstance().getExternalContext().getRequest();
		req.setAttribute("executeWorkflows", res);

		ArrayList<String> toRemoveFromCart = new ArrayList<String>();

		for (String key : this.cart.keySet()) {
			WorkflowRequest wfr = this.cart.get(key);
			// check if there's a reason
			if (!wfr.hasReason()) {
				res.getErrors().add(
						"No reason specified for '" + wfr.getWf().getLabel()
								+ "'");
				res.setError(true);
			} else {
				try {
					// touch to ensure the session is alive
					StringBuffer callURL = new StringBuffer();
					callURL.append(scaleConfig.getRawConfig()
							.getServiceConfiguration().getUnisonURL()
							+ "/services/wf/login");

					HttpGet httpget = new HttpGet(callURL.toString());

					HttpResponse response = scaleSession.getHttp().execute(httpget);
					BufferedReader in = new BufferedReader(
							new InputStreamReader(response.getEntity()
									.getContent()));
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
					callURL.append(scaleConfig.getRawConfig()
							.getServiceConfiguration().getUnisonURL()
							+ "/services/wf/execute");
					if (logger.isDebugEnabled()) logger.debug("URL for wf : '" + callURL.toString()
							+ "'");

					TremoloUser user = new TremoloUser();
					user.setUid(this.getLogin());
					user.getAttributes()
							.add(new Attribute(this.getScaleConfig()
									.getRawConfig().getServiceConfiguration()
									.getLookupAttributeName(), this.getLogin()));

					WFCall wfcall = new WFCall();
					wfcall.setUidAttributeName(this.getScaleConfig()
							.getRawConfig().getServiceConfiguration()
							.getLookupAttributeName());
					wfcall.setUser(user);
					wfcall.setName(key);
					wfcall.setReason(wfr.getReason());

					String sjson = gson.toJson(wfcall);
					HttpPost post = new HttpPost(callURL.toString());
					List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
					urlParameters.add(new BasicNameValuePair("wfcall", sjson));
					post.setEntity(new UrlEncodedFormEntity(urlParameters));

					response = scaleSession.getHttp().execute(post);
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
						logger.error("Error : '"
								+ pres.getError().getError() + "'");
						res.setError(true);
						res.getErrors().add("Could not submit '" + wfr.getWf().getLabel() + "'");
					} else {
						res.setSaved(true);
						res.getSavedRequests().add(
								"Request '" + wfr.getWf().getLabel() + "' submitted");
						toRemoveFromCart.add(key);
					}
				} catch (Exception e) {
					e.printStackTrace();
					res.setError(true);
					res.getErrors().add("Could not submit '" + wfr.getWf().getLabel() + "'");
				}

			}

		}

		for (String key : toRemoveFromCart) {
			this.cart.remove(key);
		}

		return "";
	}

	public List<ApprovalSummary> listApprovalSummaries() throws Exception {

		StringBuffer callURL = new StringBuffer();
		callURL.append(scaleConfig.getRawConfig().getServiceConfiguration()
				.getUnisonURL()
				+ "/services/approvals/list?approvalID=0&approver="
				+ this.getLogin());

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

			logger.error("Error retreiving lists of approvals : "
					+ pres.getError().getError());
			return new ArrayList<ApprovalSummary>();
		} else {
			return pres.getSummaries().getApprovals();
		}

	}

	public ApprovalReview listApprovalDetails(ApprovalSummary approval)
			throws Exception {
		StringBuffer callURL = new StringBuffer();
		callURL.append(scaleConfig.getRawConfig().getServiceConfiguration()
				.getUnisonURL()
				+ "/services/approvals/list?approvalID="
				+ approval.getApproval() + "&approver=" + this.getLogin());

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

			logger.error("Error retreiving lists of approvals : "
					+ pres.getError().getError());
			return null;
		} else {
			UserObj userObj = this.loadUserFromUnison(pres.getApprovalDetail()
					.getUser(), this.scaleConfig.getApprovalsAttributes());

			if (userObj == null) {
				// the user doesn't exist yet, so this is going to be an add
				userObj = new UserObj(this.scaleConfig.getApprovalsAttributes());
				for (String attrName : this.scaleConfig
						.getApprovalsAttributes().getAttributeNames()) {
					
					if (logger.isDebugEnabled()) logger.debug("attribute name : '" + attrName + "'");
					
					Attribute attr = pres.getApprovalDetail().getUserObj()
							.getAttribs().get(attrName);
					
					if (logger.isDebugEnabled()) logger.debug("attr from approval : '" + attr + "'");

					if (attrName.equalsIgnoreCase(this.scaleConfig
							.getRawConfig().getUiConfig()
							.getDisplayNameAttribute())) {
						if (attr != null) {
							userObj.setDisplayName(attr.getValues().get(0));
						} else {
							userObj.setDisplayName(pres.getApprovalDetail()
									.getUser());
						}
					}

					if (attr != null) {
						String value = attr.getValues().get(0);
						String label = scaleConfig
								.getApprovalsAttributes().getLabels()
								.get(attrName);
						
						if (logger.isDebugEnabled()) logger.debug("attr is not null, adding - " + attrName + " / " + value + " / " + label);
						userObj.getAttrs().put(attrName, new ScaleAttribute(attrName,label ,value));
						
					} else {
						if (logger.isDebugEnabled()) logger.debug("attr is null, adding blank");
						userObj.getAttrs().put(attrName, new ScaleAttribute(attrName, "", scaleConfig
								.getApprovalsAttributes().getLabels()
								.get(attrName)));
						
						
								
					}
				}

			}

			ApprovalReview review = new ApprovalReview();
			review.setApproved(false);
			review.setDetails(pres.getApprovalDetail());
			review.setReason("");
			review.setUserObj(userObj);

			return review;
		}
	}

	public String confirmRequest(ApprovalReview review, boolean approved) {

		review.setApproved(approved);

		if (review.validate()) {
			return "confirmapprovals.xhtml";
		} else {
			return "";
		}
	}

	public String executeRequest(ApprovalReview review) {
		review.getErrors().clear();

		try {
			StringBuffer callURL = new StringBuffer();
			callURL.append(scaleConfig.getRawConfig().getServiceConfiguration()
					.getUnisonURL()
					+ "/services/approvals/execute?approvalID="
					+ review.getDetails().getApproval()
					+ "&approver="
					+ this.getLogin()
					+ "&approved="
					+ review.isApproved()
					+ "&reason="
					+ URLEncoder.encode(review.getReason(), "UTF-8"));

			HttpGet httpget = new HttpGet(callURL.toString());

			HttpResponse response = scaleSession.getHttp().execute(httpget);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));
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
				throw new Exception(pres.getError().getError());
			} else {
				review.setComplete(true);
				return "confirmedapprovals.xhtml";
			}

		} catch (Exception e) {
			e.printStackTrace();
			review.getErrors()
					.add("There was a problem executing the request, please try again later or contact your system administrator");
			return "reviewapprovals.xhtml";
		}
	}

	public String disregardRequest() {
		return "approvals.xhtml";
	}

	public List<PortalURL> loadPortalURLs(boolean useOrgs) {
		if (useOrgs) {
			return this.urlsByOrgs.get(this.currentOrg.getId());
		} else {
			return this.portalURLs;
		}
	}

	public void checkCart(ComponentSystemEvent event) {
		if (logger.isDebugEnabled()) logger.debug("in check cart");
		FacesContext fc = FacesContext.getCurrentInstance();

		ConfigurableNavigationHandler nav = (ConfigurableNavigationHandler) fc.getApplication().getNavigationHandler();

		if (this.cart.isEmpty()) {
			if (logger.isDebugEnabled()) logger.debug("go direct to logout");
			HttpServletRequest req = (HttpServletRequest) FacesContext
					.getCurrentInstance().getExternalContext().getRequest();
			HttpServletResponse res = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
			try {
				res.sendRedirect("finish-logout.xhtml");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public ScaleSession getScaleSession() {
		return scaleSession;
	}

	public void setScaleSession(ScaleSession scaleSession) {
		this.scaleSession = scaleSession;
	}

	public boolean isWorkflowCompleted(String name) {
		return this.executedWorkflows.contains(name);
	}
	
	public boolean isUserReports() {
		return ! this.reports.isEmpty();
	}
}
