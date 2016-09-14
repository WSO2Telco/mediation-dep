/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.relying.party.adder;

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.authenticator.stub.LogoutAuthenticationExceptionException;
import org.wso2.carbon.identity.application.common.model.xsd.InboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.xsd.InboundAuthenticationRequestConfig;
import org.wso2.carbon.identity.application.common.model.xsd.Property;
import org.wso2.carbon.identity.application.common.model.xsd.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.stub.IdentityApplicationManagementServiceIdentityApplicationManagementException;
import org.wso2.carbon.identity.application.mgt.stub.IdentityApplicationManagementServiceStub;
import org.wso2.carbon.identity.oauth.stub.OAuthAdminServiceException;
import org.wso2.carbon.identity.oauth.stub.OAuthAdminServiceStub;
import org.wso2.carbon.identity.oauth.stub.dto.OAuthConsumerAppDTO;
import org.wso2.carbon.identity.sso.saml.stub.IdentitySAMLSSOConfigServiceIdentityException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.Properties;

public class RelyingPartyAdder {

    private AuthenticationAdminStub authenticationAdminStub = null;

    //edit the following configuration accordingly
    
    private static String host = "localhost";
    private static String port = "9443";
    private static String ISuserName = "admin";
    private static String ISpassword = "admin";
   
    private static String trustStorePassword = "wso2carbon";
    private static String trustStoreType = "JKS";
    private static String trustStorePath = "/home/yasith/Telco/Core/OS/setup2.0.2/wso2telcoids-2.0.2-SNAPSHOT/repository/resources/security/wso2carbon.jks";
    
    private static String applicationName = "mcxtest4111";
    private static String applicationDesc = "mcxtest41111";
    private static String callbackUrl = "http://www.callback.url/";
    private static String clientId = "12345678901111";
    private static String clientSecret = "12qwertt4211111";
    
    //---------------end of configuration----------------------
    
    /**
     * Initialize
     */
    public void init () {
        System.setProperty("javax.net.ssl.trustStore", trustStorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
        System.setProperty("javax.net.ssl.trustStoreType", trustStoreType);
    }

    /**
     * Login to the system
     *
     * @param username valid username
     * @param password valid password
     * @return
     * @throws RemoteException
     * @throws LoginAuthenticationExceptionException
     */
    public String login () throws RemoteException, LoginAuthenticationExceptionException {

        authenticationAdminStub = new AuthenticationAdminStub("https://"+host+":"+port+"/services/AuthenticationAdmin");

        String sessionCookie = null;

        if (authenticationAdminStub.login(ISuserName, ISpassword, host)) {
            System.out.println("Login Successful");

            ServiceContext serviceContext = authenticationAdminStub.
                    _getServiceClient().getLastOperationContext().getServiceContext();
            sessionCookie = (String) serviceContext.getProperty(HTTPConstants.COOKIE_STRING);
        }

        return sessionCookie;
    }

    /**
     * Add Service Provider and configure
     *
     * @param sessionCookie session obtained from authenticating using login
     * @param name name of the service provider
     * @throws RemoteException
     * @throws IdentityApplicationManagementServiceIdentityApplicationManagementException
     * @throws IdentitySAMLSSOConfigServiceIdentityException
     */
    public void addRelyingParty (String sessionCookie, String name) throws RemoteException,
            IdentityApplicationManagementServiceIdentityApplicationManagementException, IdentitySAMLSSOConfigServiceIdentityException, OAuthAdminServiceException {

        ServiceClient serviceClient;
        Options option;

        IdentityApplicationManagementServiceStub identityAppMgtStub =
                new IdentityApplicationManagementServiceStub("https://"+host+":"+port+"/services/IdentityApplicationManagementService");

        serviceClient = identityAppMgtStub._getServiceClient();
        option = serviceClient.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, sessionCookie);

        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setApplicationName(applicationName);
        serviceProvider.setDescription(applicationDesc); // desc

        // Inbound Authentication Configuration

            identityAppMgtStub.createApplication(serviceProvider);
            System.out.println("Service provider successfully created");


        // Oauth
        OAuthAdminServiceStub oAuthAdminServiceStub = new OAuthAdminServiceStub("https://"+host+":"+port+"/services/OAuthAdminService");
        serviceClient = oAuthAdminServiceStub._getServiceClient();
        option = serviceClient.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, sessionCookie);

        OAuthConsumerAppDTO oAuthConsumerAppDTO = new OAuthConsumerAppDTO();
        oAuthConsumerAppDTO.setApplicationName("");

        oAuthConsumerAppDTO.setApplicationName(name);
        oAuthConsumerAppDTO.setCallbackUrl(callbackUrl);
        oAuthConsumerAppDTO.setOAuthVersion("OAuth-2.0");
        oAuthConsumerAppDTO.setGrantTypes("implicit password authorization_code client_credentials refresh_token");
        oAuthConsumerAppDTO.setOauthConsumerKey(clientId); // key
        oAuthConsumerAppDTO.setOauthConsumerSecret(clientSecret); // secret

        oAuthAdminServiceStub.registerOAuthApplicationData(oAuthConsumerAppDTO);
        System.out.println("OAuth configuration successfully updated");

        serviceProvider = identityAppMgtStub.getApplication(name);
        InboundAuthenticationRequestConfig inboundAuthRequestConfig = new InboundAuthenticationRequestConfig();
        inboundAuthRequestConfig.setInboundAuthKey(clientId); // key again
        inboundAuthRequestConfig.setInboundAuthType("oauth2");

        Property property = new Property();
        property.setName("oauthConsumerSecret");
        property.setValue(clientSecret); // secret again1c7fc8d4d900aa9f0d4977c1
        Property[] properties = { property };
        inboundAuthRequestConfig.setProperties(properties);

        InboundAuthenticationConfig inboundAuthConfig = new InboundAuthenticationConfig();
        inboundAuthConfig.addInboundAuthenticationRequestConfigs(inboundAuthRequestConfig);
        serviceProvider.setInboundAuthenticationConfig(inboundAuthConfig);

        // update the application
        identityAppMgtStub.updateApplication(serviceProvider);
    }

    public void logout () throws RemoteException, LogoutAuthenticationExceptionException {
        authenticationAdminStub.logout();
        System.out.println("Logout successful");
    }

    public static void main (String [] args) {
    	  	    	
    	Properties prop = new Properties();
    	InputStream input = null;

        RelyingPartyAdder relyingPartyAdder = new RelyingPartyAdder();

        try {
        	
        	input = new FileInputStream("config.properties");

    		// load a properties file
    		prop.load(input);

    		
    		host = prop.getProperty("host");;
    		port = prop.getProperty("port");;
    		ISuserName = prop.getProperty("ISuserName");;
    		ISpassword = prop.getProperty("ISpassword");;
    		trustStorePassword = prop.getProperty("trustStorePassword");;
    		trustStoreType = prop.getProperty("trustStoreType");;
    		trustStorePath = prop.getProperty("trustStorePath");;
    		applicationName = prop.getProperty("applicationName");;
    		applicationDesc = prop.getProperty("applicationDesc");;
    		callbackUrl = prop.getProperty("callbackUrl");;
    		clientId = prop.getProperty("clientId");;
    		clientSecret = prop.getProperty("clientSecret");;
    		
    		// get the property value and print it out
    	    relyingPartyAdder.init();
            String sessionid = relyingPartyAdder.login();
            relyingPartyAdder.addRelyingParty(sessionid, applicationName); // app name
            relyingPartyAdder.logout();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally{
        	if (input != null) {
    			try {
    				input.close();
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
    		}
        }
    }
}
