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
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import javax.net.ssl.SSLContext;
import javax.servlet.ServletContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import com.tremolosecurity.saml.Attribute;
import com.tremolosecurity.scale.config.xml.InitParamType;
import com.tremolosecurity.scale.config.xml.ScaleCommonConfigType;
import com.tremolosecurity.scale.config.xml.ScaleConfigType;
import com.tremolosecurity.scale.user.AttributeData;
import com.tremolosecurity.scale.user.ScaleAttribute;
import com.tremolosecurity.scale.util.HttpClientInfo;

@ManagedBean(name="scaleCommonConfig")
@ApplicationScoped
public class ScaleCommonConfig {
	ScaleCommonConfigType scaleConfig;
	
	
	
	KeyStore tlsKeys;
	private SSLContext sslctx;
	
	@PostConstruct
	public void init() throws Exception {
		ServletContext context = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
		
		String configPath = context.getInitParameter("scaleConfigPath");
		if (configPath == null) {
			throw new Exception("No scaleConfigPath found");
		}
		
		InputStream in = null;
		
		if (configPath.startsWith("WEB-INF")) {
			in = context.getResourceAsStream("/" + configPath);
		} else {
			in = new FileInputStream(configPath);
		}
		
		
		
		
		JAXBContext jc = JAXBContext.newInstance("com.tremolosecurity.scale.config.xml");
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		
		
		Object obj = unmarshaller.unmarshal(in);
		
		JAXBElement<ScaleCommonConfigType> scaleConfig = (JAXBElement<ScaleCommonConfigType>) obj;
		
		this.scaleConfig = scaleConfig.getValue();
		
		
		
		
		String ksPath = this.scaleConfig.getServiceConfiguration().getKeyStorePath();
		String ksPass = this.scaleConfig.getServiceConfiguration().getKeyStorePassword();
		
		in = null;
		
		if (ksPath.startsWith("WEB-INF")) {
			in = context.getResourceAsStream("/" + ksPath);
		} else {
			in = new FileInputStream(ksPath);
		}
		
		this.tlsKeys = KeyStore.getInstance("JKS");
		this.tlsKeys.load(in,ksPass.toCharArray());
		
		this.sslctx = SSLContexts.custom().loadTrustMaterial(this.tlsKeys).loadKeyMaterial(this.tlsKeys, ksPass.toCharArray()).build();
	}

	public ScaleCommonConfigType getScaleConfig() {
		return scaleConfig;
	}


	
	public HttpClientInfo createHttpClientInfo() {
		
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslctx,new AllowAllHostnameVerifier());
		PlainConnectionSocketFactory sf = PlainConnectionSocketFactory.getSocketFactory();
		Registry<ConnectionSocketFactory> r = RegistryBuilder.<ConnectionSocketFactory>create()
		        .register("http", sf)
		        .register("https", sslsf)
		        .build();
		
		RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY).setRedirectsEnabled(true).build();
		
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(r);
		
		return new HttpClientInfo(cm,globalConfig);
	}
}
