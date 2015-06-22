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
package com.tremolosecurity.scale.totp;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Hashtable;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.tremolosecurity.json.Token;
import com.tremolosecurity.scale.config.ScaleCommonConfig;
import com.tremolosecurity.scale.config.xml.ScalePasswordResetConfigType;
import com.tremolosecurity.scale.config.xml.ScaleTOTPConfigType;
import com.tremolosecurity.scale.user.AttributeData;
import com.tremolosecurity.scale.user.ScaleAttribute;
import com.tremolosecurity.scale.user.ScaleSession;
import com.tremolosecurity.scale.user.UserObj;
import com.tremolosecurity.scale.util.UnisonUserData;

@ManagedBean(name="scaleTotpController")
@SessionScoped
public class TotpController {
	
	static Logger logger = Logger.getLogger(TotpController.class.getName());
	
	@ManagedProperty(value="#{scaleCommonConfig}")
	ScaleCommonConfig commonConfig;
	
	@ManagedProperty(value = "#{scaleSession}")
	ScaleSession scaleSession;
	
	ScaleTOTPConfigType scaleTotpConfig;
	
	private String login;

	private String displayName;

	private UserObj user;

	private String encryptedToken;

	private String otpURL;
	
	private String error;

	private String encodedQRCode;
	
	
	@PostConstruct
	public void init() {
		this.error = null;
		HttpServletRequest request = (HttpServletRequest) FacesContext
				.getCurrentInstance().getExternalContext().getRequest();
		
		this.scaleTotpConfig = (ScaleTOTPConfigType) commonConfig.getScaleConfig();
		
		this.login = request.getRemoteUser();

		
		
		UnisonUserData userData;
		try {
			userData = this.scaleSession.loadUserFromUnison(this.login,new AttributeData(scaleTotpConfig.getServiceConfiguration().getLookupAttributeName(),scaleTotpConfig.getUiConfig().getDisplayNameAttribute(),scaleTotpConfig.getAttributeName()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		this.user = userData.getUserObj();
		
		this.displayName = userData.getUserObj().getDisplayName();
		
		ScaleAttribute scaleAttr = userData.getUserObj().getAttrs().get(scaleTotpConfig.getAttributeName());
		if (scaleAttr == null) {
			if (logger.isDebugEnabled()) logger.debug("no sattribute");
			this.error = "Token not found";
			return;
		}
		
		this.encryptedToken = scaleAttr.getValue();
		
		try {
			byte[] decryptionKeyBytes = Base64.decodeBase64(scaleTotpConfig.getDecryptionKey().getBytes("UTF-8"));
			SecretKey decryptionKey = new SecretKeySpec(decryptionKeyBytes, 0, decryptionKeyBytes.length, "AES");
			
			Gson gson = new Gson();
			Token token = gson.fromJson(new String(Base64.decodeBase64(this.encryptedToken.getBytes("UTF-8"))), Token.class);
			byte[] iv = org.bouncycastle.util.encoders.Base64.decode(token.getIv());
			IvParameterSpec spec =  new IvParameterSpec(iv);
		    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, decryptionKey,spec);
			
			String decryptedJSON = new String(cipher.doFinal(Base64.decodeBase64(token.getEncryptedRequest().getBytes("UTF-8"))));
			
			if (logger.isDebugEnabled()) logger.debug(decryptedJSON);
			
			TOTPKey totp = gson.fromJson(decryptedJSON, TOTPKey.class);
			
			this.otpURL = "otpauth://totp/" + totp.getUserName() + "@" + totp.getHost() + "?secret=" + totp.getSecretKey();
			
		} catch (Exception e) {
			e.printStackTrace();
			this.error = "Could not decrypt token";
		}
		
		try {
		int size = 250;
		Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<EncodeHintType, ErrorCorrectionLevel>();
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix byteMatrix = qrCodeWriter.encode(this.otpURL,BarcodeFormat.QR_CODE, size, size, hintMap);
        int CrunchifyWidth = byteMatrix.getWidth();
        BufferedImage image = new BufferedImage(CrunchifyWidth, CrunchifyWidth,
                BufferedImage.TYPE_INT_RGB);
        image.createGraphics();

        Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, CrunchifyWidth, CrunchifyWidth);
        graphics.setColor(Color.BLACK);

        for (int i = 0; i < CrunchifyWidth; i++) {
            for (int j = 0; j < CrunchifyWidth; j++) {
                if (byteMatrix.get(i, j)) {
                    graphics.fillRect(i, j, 1, 1);
                }
            }
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        ImageIO.write(image, "png", baos);
        
        this.encodedQRCode = new String(Base64.encodeBase64(baos.toByteArray()));
		} catch (Exception e) {
			e.printStackTrace();
			this.error = "Could not encode QR Code";
		}
        
		
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

	public String getLogin() {
		return login;
	}

	public String getDisplayName() {
		return displayName;
	}

	public UserObj getUser() {
		return user;
	}
	
	public String getError() {
		if (logger.isDebugEnabled()) logger.debug("this error : '" + this.error + "'");
		return this.error;
	}


	public String getEncryptedToken() {
		return encryptedToken;
	}
	

	public String getOtpURL() {
		return this.otpURL;
	}


	public String getEncodedQRCode() {
		return encodedQRCode;
	}
	
	
	
}
