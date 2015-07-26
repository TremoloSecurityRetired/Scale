/*******************************************************************************
 * Copyright 2015 Tremolo Security, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.tremolosecurity.scale.ui.reports;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Date;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.gson.Gson;
import com.tremolosecurity.provisioning.service.util.ProvisioningResult;
import com.tremolosecurity.provisioning.service.util.ReportInformation;
import com.tremolosecurity.provisioning.service.util.ReportResults;
import com.tremolosecurity.scale.config.ScaleConfiguration;
import com.tremolosecurity.scale.user.ScaleSession;
import com.tremolosecurity.scale.user.ScaleUser;

@ManagedBean(name = "scaleReport")
@SessionScoped
public class ReportViewer {
	
	static Logger logger = Logger.getLogger(ReportViewer.class.getName());
	
	@ManagedProperty(value = "#{scaleConfiguration}")
	ScaleConfiguration scaleConfig;

	@ManagedProperty(value = "#{scaleSession}")
	ScaleSession scaleSession;
	
	@ManagedProperty(value= "#{scaleUser}")
	ScaleUser scaleUser;
	
	
	String userKey;
	Date beginDate;
	Date endDate;
	String error;
	
	
	String paramError;
	
	
	ReportResults results;
	
	ReportInformation reportInfo;
	
	String runDateTime;
	
	boolean reportLoaded;
	
	public void loadReport() throws ClientProtocolException, IOException {
		
		
		
		StringBuffer callURL = new StringBuffer();
		callURL.append(
				scaleConfig.getRawConfig().getServiceConfiguration()
						.getUnisonURL()
						+ "/services/reports/run?name=")
				.append(URLEncoder.encode(reportInfo.getName(),"UTF-8"));
				
		for (String paramType : reportInfo.getParameters()) {
			switch (paramType) {
				case "currentUser" : callURL.append("&currentUser=").append(URLEncoder.encode(scaleUser.getLogin(),"UTF-8")); break;
				case "userKey" : callURL.append("&userKey=").append(URLEncoder.encode(userKey,"UTF-8")); break;
				case "beginDate" : callURL.append("&beginDate=").append(URLEncoder.encode(new DateTime(beginDate).toString(),"UTF-8")); break;
				case "endDate" : callURL.append("&endDate=").append(URLEncoder.encode(new DateTime(endDate).toString(),"UTF-8")); break;
			}
		}

		HttpGet httpget = new HttpGet(callURL.toString());

		HttpResponse response = scaleSession.getHttp().execute(httpget);
		BufferedReader in = new BufferedReader(new InputStreamReader(response
				.getEntity().getContent()));
		String line = null;
		StringBuffer json = new StringBuffer();
		while ((line = in.readLine()) != null) {
			json.append(line);
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Report JSON : '" + json + "'");
		}
		
		Gson gson = new Gson();
		ProvisioningResult pres = gson.fromJson(json.toString(),
				ProvisioningResult.class);
		
		if (logger.isDebugEnabled()) {
			logger.debug("Provision results success : " + pres.isSuccess());
			logger.debug("Provision results : " + pres.getReportResults());
		}
		
		if (! pres.isSuccess()) {
			this.error = "There was a problem running the report - " + pres.getError().getError();
		} else {
			this.results = pres.getReportResults();
			this.error = null;
		}
		
		this.reportLoaded = true;

	}
	
	public String runReport(ReportInformation ri) {
		try {
			this.reportInfo = ri;
			this.beginDate = null;
			this.endDate = null;
			this.userKey = null;
			this.error = null;
			this.paramError = null;
			this.results = null;
			this.runDateTime = null;
			this.reportLoaded = false;
			
			DateTimeFormatter fmt = DateTimeFormat.forPattern("MMMM dd, yyyy HH:mm:ss zzz");
			this.runDateTime = fmt.print(System.currentTimeMillis());
			
			if (ri.getParameters().size() == 0 || (ri.getParameters().size() == 1 && ri.getParameters().contains("currentUser"))) {
				//no need for the parameters screen
				return "reportWait.xhtml";
			} else {
				return "reportParams.xhtml";
			}
		} catch (Throwable t) {
			this.error = "There was a problem loading the report";
			logger.error("Error loading report : " + ri.getName(),t);
			return "showReport.xhtml";
		}
	}
	

	
	public String finishReport() {
		try {
			
			if (this.isUserKeyParam() && (this.userKey == null || this.userKey.isEmpty())) {
				this.paramError = "User is required";
				return "reportParams.xhtml";
			}
			
			if (this.isBeginDateParam() && this.beginDate == null) {
				this.paramError = "Begin Date is required";
				return "reportParams.xhtml";
			}
			
			if (this.isEndDateParam() && this.endDate == null) {
				this.paramError = "End Date is required";
				return "reportParams.xhtml";
			}			
				
				return "reportWait.xhtml";
			
		} catch (Throwable t) {
			this.error = "There was a problem loading the report";
			logger.error("Error loading report : " + reportInfo.getName(),t);
			return "reportWait.xhtml";
		}
	}

	public ScaleConfiguration getScaleConfig() {
		return scaleConfig;
	}

	public void setScaleConfig(ScaleConfiguration scaleConfig) {
		this.scaleConfig = scaleConfig;
	}

	public ScaleSession getScaleSession() {
		return scaleSession;
	}

	public void setScaleSession(ScaleSession scaleSession) {
		this.scaleSession = scaleSession;
	}

	public ReportResults getResults() {
		if (logger.isDebugEnabled()) {
			logger.debug("Results : " + results);
			if (results != null) {
				logger.debug("Results groups : " + results.getGrouping());
				
				if (results.getGrouping() != null) {
					logger.debug("Number of groupings : " + results.getGrouping().size());
				}
			}
		}
		return results;
	}

	public ScaleUser getScaleUser() {
		return scaleUser;
	}

	public void setScaleUser(ScaleUser scaleUser) {
		this.scaleUser = scaleUser;
	}

	public String getUserKey() {
		return userKey;
	}

	public void setUserKey(String userKey) {
		this.userKey = userKey;
	}

	public Date getBeginDate() {
		return beginDate;
	}

	public String getEndDateLabel() {
		if (this.endDate == null) {
			return "Not Selected";
		} else {
			
			DateTimeFormatter fmt = DateTimeFormat.forPattern("MMMM dd, yyyy");
			return fmt.print(this.endDate.getTime());
		}
	}
	
	public String getBeginDateLabel() {
		if (this.beginDate == null) {
			return "Not Selected";
		} else {
			
			DateTimeFormatter fmt = DateTimeFormat.forPattern("MMMM dd, yyyy");
			return fmt.print(this.beginDate.getTime());
		}
	}

	public void setBeginDate(Date beginDate) {
		this.beginDate = beginDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public String getError() {
		return error;
	}

	public ReportInformation getReportInfo() {
		return reportInfo;
	}
	
	public boolean isUserKeyParam() {
		return this.reportInfo.getParameters().contains("userKey");
	}
	
	public boolean isBeginDateParam() {
		return this.reportInfo.getParameters().contains("beginDate");
	}
	
	public boolean isEndDateParam() {
		return this.reportInfo.getParameters().contains("endDate");
	}
	
	public String getParamError() {
		return this.paramError;
	}
	
	public String getRunDateTime() {
		return this.runDateTime;
	}
	
	public String getExcelName() {
		return this.reportInfo.getName().replaceAll(" ", "_") + "-" + this.runDateTime.replaceAll(" ", "_") + ".xlsx";
	}
	
	public boolean isReportDone() {
		return this.reportLoaded;
	}
}
