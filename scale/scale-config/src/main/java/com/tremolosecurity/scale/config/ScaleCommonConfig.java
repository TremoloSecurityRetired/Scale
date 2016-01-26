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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
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
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

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
	public static final String version = "1.0.6-2016012501";
	
	static Logger logger;
	ScaleCommonConfigType scaleConfig;
	
	
	
	KeyStore tlsKeys;
	private SSLContext sslctx;
	
	@PostConstruct
	public void init()  {
		try {
			ServletContext context = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
			
			
			String logPath = null;
			
			try {
				logPath = InitialContext.doLookup("java:comp/env/scaleLog4jPath");
			} catch (NamingException ne) {
				try {
					logPath = InitialContext.doLookup("java:/env/scaleLog4jPath");
				} catch (NamingException ne2) {
					logPath = null;
				}
			}
			if (logPath == null) {
				Properties props = new Properties();
				props.put("log4j.rootLogger", "info,console");
				
				//props.put("log4j.appender.console","org.apache.log4j.RollingFileAppender");
				//props.put("log4j.appender.console.File","/home/mlb/myvd.log");
				props.put("log4j.appender.console","org.apache.log4j.ConsoleAppender");
				props.put("log4j.appender.console.layout","org.apache.log4j.PatternLayout");
				props.put("log4j.appender.console.layout.ConversionPattern","[%d][%t] %-5p %c{1} - %m%n");
				
				
				
				PropertyConfigurator.configure(props);
			} else {
				
				if (logPath.startsWith("WEB-INF/")) {
					org.apache.log4j.xml.DOMConfigurator.configure(context.getRealPath(logPath));
				} else {
					org.apache.log4j.xml.DOMConfigurator.configure(logPath);
				}
				
				
			}
			

			logger =   Logger.getLogger(ScaleCommonConfig.class.getName());
			
			logger.info("Initializing Scale " + version);
			
			
			String configPath = null;
			
			try {
				configPath = InitialContext.doLookup("java:comp/env/scaleConfigPath");
			} catch (NamingException ne) {
				try {
					configPath = InitialContext.doLookup("java:/env/scaleConfigPath");
				} catch (NamingException ne2) {
					configPath = null;
				}
				
			}
			
			
			if (configPath == null) {
				configPath = "WEB-INF/scaleConfig.xml";
				logger.warn("No configuraiton path found - Loading configuration from '" + configPath + "'");
			} else {
				logger.info("Loading configuration from '" + configPath + "'");
			}
			
			InputStream in = null;
			
			if (configPath.startsWith("WEB-INF")) {
				
				in = new ByteArrayInputStream(ScaleCommonConfig.includeEnvironmentVariables( context.getRealPath( "/" + configPath)).getBytes("UTF-8"));
				
				
			} else {
				in = new ByteArrayInputStream(ScaleCommonConfig.includeEnvironmentVariables( configPath).getBytes("UTF-8"));
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
		} catch (Exception e) {
			e.printStackTrace();
		}
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
	
	public static String includeEnvironmentVariables(String srcPath) throws IOException {
		StringBuffer b = new StringBuffer();
		String line = null;
		
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(srcPath)));
		
		while ((line = in.readLine()) != null) {
			b.append(line).append('\n');
		}
		
		String cfg = b.toString();
		if (logger.isDebugEnabled()) {
			logger.debug("---------------");
			logger.debug("Before environment variables : '" + srcPath + "'");
			logger.debug(cfg);
			logger.debug("---------------");
		}
		
		int begin,end;
		
		b.setLength(0);
		begin = 0;
		end = 0;
		
		String finalCfg = null;
		
		begin = cfg.indexOf("#[");
		while (begin > 0) {
			if (end == 0) {
				b.append(cfg.substring(0,begin));
			} else {
				b.append(cfg.substring(end,begin));
			}
			
			end = cfg.indexOf(']',begin + 2);
			
			String envVarName = cfg.substring(begin + 2,end);
			String value = System.getenv(envVarName);
			
			if (value == null) {
				value = System.getProperty(envVarName);
			}
			
			if (logger.isDebugEnabled()) {
				logger.debug("Environment Variable '" + envVarName + "'='" + value + "'");
			}
			
			b.append(value);
			
			begin = cfg.indexOf("#[",end + 1);
			end++;
			
		}
		
		if (end == 0) {
			finalCfg = cfg;
		} else {
			b.append(cfg.substring(end));
			finalCfg = b.toString();
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("---------------");
			logger.debug("After environment variables : '" + srcPath + "'");
			logger.debug(finalCfg);
			logger.debug("---------------");
		}
		
		return finalCfg;
		
		
	}
}
