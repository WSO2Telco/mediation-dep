package com.wso2telco.core.datamigrator.util;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.wso2telco.core.datamigrator.DataMigrator;
import com.wso2telco.core.datamigrator.exception.PCRException;

public class OldPcrCreator {

	private static Logger log = Logger.getLogger(DataMigrator.class);

	public static String createPcr(String id, String applicationName) throws PCRException {
		String msisdn = "tel:+" + id;
		String keyText = "cY4L3dBf@mifenew";
		byte[] keyValue = keyText.getBytes();
		SecretKey key = new SecretKeySpec(keyValue, "AES");
		String encryptedText = "";
		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance("AES");
			byte[] plainTextByte = (applicationName + msisdn).getBytes();
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte[] encryptedByte = null;
			encryptedByte = cipher.doFinal(plainTextByte);
			encryptedText = Base64.encodeBase64String(encryptedByte);
		} catch (NoSuchAlgorithmException e) {
			log.error("No Such Algorithm Exception",e);
			throw new PCRException("No Such Algorithm Exception");
		} catch (InvalidKeyException e) {
			log.error("Invalid Key Exception",e);
			throw new PCRException("Invalid Key Exception");
		} catch (IllegalBlockSizeException e) {
			log.error("Illegal Block Size Exception",e);
			throw new PCRException("Illegal Block Size Exception");
		} catch (BadPaddingException e) {
			log.error("Bad Padding Exception",e);
			throw new PCRException("Bad Padding Exception");
		} catch (NoSuchPaddingException e) {
			log.error("No Such Padding Exception",e);
			throw new PCRException("No Such Padding Exception");
		}
		log.debug("------------------------------------");
		log.debug("Encrypted Text:" + encryptedText);
		String encryptedPcr = encryptedText.replaceAll("\r", "").replaceAll("\n", "");
		return encryptedPcr;
	}
}
