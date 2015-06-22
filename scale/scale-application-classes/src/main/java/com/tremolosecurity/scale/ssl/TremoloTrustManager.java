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
package com.tremolosecurity.scale.ssl;


import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.Certificate;



public class TremoloTrustManager implements X509TrustManager {


	KeyStore gks;
	HashSet<String> issuers;

	
	
	public TremoloTrustManager(KeyStore ks) throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
		gks = ks;
		this.issuers = null;
	}
	
	public TremoloTrustManager(KeyStore ks,HashSet<String> issuers) throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
		gks = ks;
		this.issuers = issuers;
	}
	
	

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		
		
		KeyStore ks = gks;
		
		if (ks == null) {
			
		}
		
		/*try {
			Enumeration<String> enumer = ks.aliases();
			while (enumer.hasMoreElements()) {
				System.out.println("In Keystore - " + enumer.nextElement());
			}
		} catch (KeyStoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
		
		
		boolean trusted = false;
		for (X509Certificate cert : chain) {
			try {
				//System.out.println("checking cert - " + cert.getSubjectDN().getName());
				String alias = ks.getCertificateAlias(cert);
				//System.out.println("exists? : " + (alias != null));
				if (alias != null) {
					trusted = true;
					break;
				}
			} catch (KeyStoreException e) {
				e.printStackTrace();
				throw new CertificateException(e);
			}
			
		}
		
		if (! trusted) {
			
			X509Certificate last = chain[chain.length-1];
			if (last.getIssuerX500Principal().equals(last.getSubjectX500Principal())) {
				//self signed, no point in continuing
				throw new CertificateException("Could not validated certificate chain");
			}
			
			try {
				Enumeration<String> aliases = ks.aliases();
				while (aliases.hasMoreElements()) {
					String alias = aliases.nextElement();
					java.security.cert.Certificate cert = ks.getCertificate(alias);
					
					if (cert instanceof X509Certificate) {
						X509Certificate ca = (X509Certificate) cert;
						if (ca.getSubjectX500Principal().equals(last.getIssuerX500Principal())) {
							try {
								last.verify(ca.getPublicKey());
								trusted = true;
								
								
								
								
								
								break;
							} catch (Throwable t) {
								t.printStackTrace();
								/*if (logger.isDebugEnabled()) {
									logger.debug("Could not verify " + last.getSubjectDN() + " using alias " + alias);
								}*/
							}
						}
					}
				}
			} catch (KeyStoreException e) {
				throw new CertificateException("Could not validated certificate chain",e);
			}
			
			
			if (! trusted) {
				throw new CertificateException("Could not validated certificate chain");
			}
		}
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		this.checkClientTrusted(chain, authType);
		
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		try {
			
			KeyStore ks = gks;
			

			
			ArrayList<X509Certificate> certs = new ArrayList<X509Certificate>();
			Enumeration<String> aliases = ks.aliases();
			while (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				java.security.cert.Certificate c = ks.getCertificate(alias);
				if (c instanceof X509Certificate) {
					if ( issuers == null || issuers.contains(((X509Certificate) c).getSubjectX500Principal().getName())) {
						certs.add((X509Certificate) c);
					}
				}
			}
			
			X509Certificate[] x509s = new X509Certificate[certs.size()];
			certs.toArray(x509s);
			return x509s;
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}



