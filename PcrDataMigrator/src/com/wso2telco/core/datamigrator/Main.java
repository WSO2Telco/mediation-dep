package com.wso2telco.core.datamigrator;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Main {

	public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {		
		
		  	String pcr = "pQlHgUYwrATleytGHlvF6macgPj+lHYyhNvFuiwFqeI=";
	        String keyText = "cY4L3dBf@mifenew";
	        byte[] keyValue = keyText.getBytes();
	        SecretKey key = new SecretKeySpec(keyValue, "AES");
	        Cipher cipher = null;

	        try {
	            cipher = Cipher.getInstance("AES");
	        } catch (NoSuchPaddingException ex) {

	        }

	        byte[] plainTextByte = pcr.getBytes();
	        cipher.init(Cipher.DECRYPT_MODE, key);
	        byte[] decryptedByte = null;
	        try {
	            decryptedByte = cipher.doFinal(plainTextByte);
	        } catch (IllegalBlockSizeException ex) {

	        } catch (BadPaddingException ex) {

	        }

	        String msisdn = new String(decryptedByte, "UTF-8");
	        msisdn = msisdn.substring(msisdn.indexOf(":"));
	        System.out.println(msisdn);

	}
}
