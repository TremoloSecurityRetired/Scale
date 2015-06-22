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

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import com.tremolosecurity.provisioning.service.util.ApprovalSummary;
import com.tremolosecurity.scale.config.ScaleConfiguration;

@ManagedBean(name="openApprovals")
@RequestScoped
public class OpenApprovals {
	
	
	@ManagedProperty(value="#{scaleConfiguration}")
	ScaleConfiguration scaleConfig;
	
	@ManagedProperty(value="#{scaleUser}")
	ScaleUser scaleUser;
	
	List<ApprovalSummary> approvalSummaries;
	
	static SimpleDateFormat sdf = new SimpleDateFormat();
	
	boolean error;
	
	@PostConstruct
	public void loadApprovals() {
		try {
			this.approvalSummaries = scaleUser.listApprovalSummaries();
			this.error = false;
		} catch (Exception e) {
			e.printStackTrace();
			this.error = true;
			this.approvalSummaries = new ArrayList<ApprovalSummary>();
		}
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
	
	public int getNumberOfOpenApprovals() {
		return this.approvalSummaries.size();
	}
	
	public List<ApprovalSummary> getApprovals() {
		return this.approvalSummaries;
	}
	
	public String formatDate(long dt) {
		Timestamp date = new Timestamp(dt);
		
		return sdf.format(date);
	}
	
	public String review(ApprovalSummary approval) {
		HttpServletRequest request = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
		
		ApprovalReview review = null;
		
		try {
			review = scaleUser.listApprovalDetails(approval);
		} catch (Exception e) {
			e.printStackTrace();
			review = null;
		}
		
		request.getSession().setAttribute("approvalReview", review);
		
		
		
		return "reviewapprovals.xhtml";
	}


	public boolean isError() {
		return error;
	}


	public void setError(boolean error) {
		this.error = error;
	}
	
	
}
