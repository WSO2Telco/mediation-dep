package com.wso2telco.refresh;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.codec.binary.Base64;
import org.apache.cxf.helpers.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Refresher {
	static Logger logger = LoggerFactory.getLogger( Refresher.class );
	private static AtomicReference<String> accessToken = new AtomicReference<String>();
	private static String refreshToken = null;
	private static int validityPeriod; // in seconds
	private int refreshOffset = 2500; // in seconds
	private String tokenEndpoint = "";
	private String tokenData = "";
	private String tokenRefreshData = "";
	private Gson gson;
	private String bearerToken = "";
	private String username;
	private String password;
	private static long tokenExpiryTime = 0; // in milliseconds

	/**
	 * 
	 */
	public Refresher() {
		try {
			gson = new GsonBuilder().serializeNulls().create();
			
			Properties settings = new Properties();
			
			String defaultPath = new File("./conf/settings.xml").getCanonicalPath();
			System.out.println("Looking for file in default location..." + new File("./conf").getCanonicalPath());
			File file = new File(defaultPath);
			file.getAbsoluteFile();
			InputStream is = new FileInputStream(file);
			
			settings.loadFromXML(is/*Refresher.class.getResourceAsStream("settings.xml")*/);

			tokenEndpoint = settings.getProperty("token_endpoint");
			tokenData = settings.getProperty("token_data");
			tokenRefreshData = settings.getProperty("token_refresh");
			String consumerKey = settings.getProperty("app_key");
			String consumerSecret = settings.getProperty("app_secret");
			username = settings.getProperty("username");
			password = settings.getProperty("password");
			refreshOffset = Integer.parseInt(settings.getProperty("refresh_offset"));
			bearerToken = new String(Base64.encodeBase64((consumerKey+":"+consumerSecret).getBytes()));

			int validTime = Integer.valueOf(settings.getProperty("validity_period"));
			tokenExpiryTime = Long.valueOf(settings.getProperty("token_created_timestamp")) + (validTime * 1000L) -
					(refreshOffset * 1000L);
			if (accessToken.get() == null && (tokenExpiryTime > System.currentTimeMillis())) {
				accessToken.set(settings.getProperty("access_token"));//get access token from properties file
				refreshToken = settings.getProperty("refresh_token");
				validityPeriod = validTime;
			} else if(accessToken.get() == null) {
				logger.info("HHHHHHHHHHHHHHHHHHH		Calling generateToken...");
				generateToken();//generate access token
			} else {
				generateTokenWithRefreshToken();
			}

			logger.info("HHHHHHHHHHHHHHHHHHH		tokenEndpoint : "+tokenEndpoint);
			logger.info("HHHHHHHHHHHHHHHHHHH		tokenData : "+tokenData);
			logger.info("HHHHHHHHHHHHHHHHHHH		tokenRefreshData : "+tokenRefreshData);
			logger.info("HHHHHHHHHHHHHHHHHHH		consumerKey : "+consumerKey);
			logger.info("HHHHHHHHHHHHHHHHHHH		consumerSecret : "+consumerSecret);
			logger.info("HHHHHHHHHHHHHHHHHHH		username : "+username);
			logger.info("HHHHHHHHHHHHHHHHHHH		password : "+password);
			logger.info("HHHHHHHHHHHHHHHHHHH		refreshOffset : "+refreshOffset);
			logger.info("HHHHHHHHHHHHHHHHHHH		bearerToken : "+bearerToken);
			logger.info("HHHHHHHHHHHHHHHHHHH		accessToken : "+accessToken);
			logger.info("HHHHHHHHHHHHHHHHHHH		refreshToken : "+refreshToken);
			logger.info("HHHHHHHHHHHHHHHHHHH		validityPeriod : "+validityPeriod);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 */
	private void generateToken() {
			try {
				HttpPost post = new HttpPost(tokenEndpoint);
				post.addHeader("Authorization", "Basic "+bearerToken);				
				Object[] args = {username, password};
				String tokenCreatePostData = MessageFormat.format(tokenData, args);
				logger.info("HHHHHHHHHHHHHHHHHHH		tokenCreatePostData : "+tokenCreatePostData);
				StringEntity strEntity = new StringEntity(tokenCreatePostData, "UTF-8");
				strEntity.setContentType("application/x-www-form-urlencoded");
				post.setEntity(strEntity);
				CloseableHttpResponse response = null;
				SSLContextBuilder builder = new SSLContextBuilder();
			    builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
			    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build());
			    CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
				response = httpclient.execute(post);
				HttpEntity entity = response.getEntity();
		      
				if (entity != null) {
			        InputStream instream = entity.getContent();
			        StringWriter writer = new StringWriter();
					IOUtils.copy(new InputStreamReader(instream), writer, 1024);
			        String body = writer.toString();
					logger.info("HHHHHHHHHHHHHHHHHHH		body : " + body);
			        TokenResponse resp = gson.fromJson(body, TokenResponse.class);
			        accessToken.set(resp.getAccessToken());
			        System.out.println("RECEIVED TOKEN(1)>" + resp.getAccessToken());
			        refreshToken = resp.getRefreshToken();
			        validityPeriod = resp.getExpiresIn();
					tokenExpiryTime = System.currentTimeMillis() + ((validityPeriod - refreshOffset) * 1000L);
			        
			        logger.info("HHHHHHHHHHHHHHHHHHH		accessToken_II : "+accessToken.toString());
			        logger.info("HHHHHHHHHHHHHHHHHHH		refreshToken_II : "+refreshToken);
			        logger.info("HHHHHHHHHHHHHHHHHHH		validityPeriod_II : "+String.valueOf(validityPeriod));
			        
				}		
			} catch (UnknownHostException e) {
			  e.printStackTrace();
			  System.out.println("Please check your internet connection.");
			  System.exit(0);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	/**
	 * 
	 * @return
	 */
	public static String getToken() {
		logger.info("HHHHHHHHHHHHHHHHHHH	Calling getToken");
		if(accessToken.get()  == null ||  tokenExpiryTime < System.currentTimeMillis()) {
			logger.info("HHHHHHHHHHHHHHHHHHH	Calling  Refresher");
			synchronized (Refresher.class) {
				if(accessToken.get()  == null ||  tokenExpiryTime < System.currentTimeMillis()) {
					new Refresher();
				}
			}

		}
		logger.info("HHHHHHHHHHHHHHHHHHH	accessTokenIII : "+accessToken.get());
		return accessToken.get();
	}

	public void generateTokenWithRefreshToken() {
		try {
			HttpPost post = new HttpPost(tokenEndpoint);
			post.addHeader("Authorization", "Basic "+bearerToken);

			Object[] args = {refreshToken, refreshToken};
			String tokenRefreshPostData = MessageFormat.format(tokenRefreshData, args);

			StringEntity strEntity = new StringEntity(tokenRefreshPostData, "UTF-8");
			strEntity.setContentType("application/x-www-form-urlencoded");
			post.setEntity(strEntity);

			CloseableHttpResponse response = null;

			SSLContextBuilder builder = new SSLContextBuilder();
			builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build());
			CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
			response = httpclient.execute(post);
			HttpEntity entity = response.getEntity();

			if (entity != null) {
				InputStream instream = entity.getContent();
				StringWriter writer = new StringWriter();
				IOUtils.copy(new InputStreamReader(instream), writer, 1024);
				String body = writer.toString();

				TokenResponse resp = gson.fromJson(body, TokenResponse.class);
				accessToken.set(resp.getAccessToken());
				System.out.println("RECEIVED TOKEN(2)>" + resp.getAccessToken());
				refreshToken = resp.getRefreshToken();
				validityPeriod = resp.getExpiresIn();
				tokenExpiryTime = System.currentTimeMillis() + (validityPeriod * 1000L) - (refreshOffset * 1000L);
				logger.info("HHHHHHHHHHHHHHHHHHH		accessToken_IV : "+accessToken.toString());
				logger.info("HHHHHHHHHHHHHHHHHHH		refreshToken_IV : "+refreshToken);
				logger.info("HHHHHHHHHHHHHHHHHHH		validityPeriod_IV : "+String.valueOf(validityPeriod));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
