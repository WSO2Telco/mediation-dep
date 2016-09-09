/*
 * @author shirhan@inovaitsys.com
 * */
package lk.dialog.smsc.util;

import java.nio.charset.Charset;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logica.smscsim.SimulatorPDUProcessor;

public class RESTClient {

 static Logger logger = LoggerFactory.getLogger( SimulatorPDUProcessor.class );	
	
  private HttpClient objRestClient;
  private String strHubEndPoint = null;

  public RESTClient() {
    try {
      objRestClient = HttpClients.custom().setSSLSocketFactory(
          new SSLConnectionSocketFactory(SSLContexts.custom()
              .loadTrustMaterial(null, new TrustSelfSignedStrategy())
              .build()
          )
          ).build();
      strHubEndPoint = PropertyReader.getPropertyValue(SMSCSIMProperties.API_SEND_SMS_PREFIX);
    } catch (Exception e) {
      logger.error("RESTClient::RESTClient", e);
      e.printStackTrace();
    }
  }

  public HttpResponse get(String strRequestParams, String strJSONParam, String strAccessToken) {
    HttpResponse objRestResponse = null;
    try {
      HttpGet objRestRequest = new HttpGet(strHubEndPoint + strRequestParams);
      objRestRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
      objRestRequest.addHeader("Authorization", "Bearer " + strAccessToken);
      objRestRequest.addHeader("Accept", "application/json");
      objRestResponse = objRestClient.execute(objRestRequest);
      return objRestResponse;
    } catch (Exception e) {
      e.printStackTrace();
      logger.error("RESTClient::get", e);
      return objRestResponse;
    }
  }

  public HttpResponse post(String strRequestParams, String strJSONParam, String strAccessToken) {
    HttpResponse objRestResponse = null;
    try {
      HttpPost objRestRequest = new HttpPost(strHubEndPoint + strRequestParams);

      objRestRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
      objRestRequest.addHeader("Authorization", "Bearer " + strAccessToken);
      objRestRequest.addHeader("Accept", "application/json");

      if (strJSONParam != null) {
        StringEntity strJSON = new StringEntity(strJSONParam, Charset.forName("UTF-8"));
        objRestRequest.setEntity(strJSON);
      }
      objRestResponse = objRestClient.execute(objRestRequest);
      return objRestResponse;
    } catch (Exception e) {
      e.printStackTrace();
      logger.error("RESTClient::post", e);
      return objRestResponse;
    }
  }
  /*public HttpResponse post2(String strRequestParams, String strJSONParam, String strAccessToken) {
    HttpResponse objRestResponse = null;
    try {
      HttpPost objRestRequest = new HttpPost(strRequestParams);
      
      objRestRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
      objRestRequest.addHeader("Authorization", "Bearer " + strAccessToken);
      objRestRequest.addHeader("Accept", "application/json");
      
      if (strJSONParam != null) {
        StringEntity strJSON = new StringEntity(strJSONParam);
        objRestRequest.setEntity(strJSON);
      }
      objRestResponse = objRestClient.execute(objRestRequest);
      return objRestResponse;
    } catch (Exception e) {
      e.printStackTrace();
      return objRestResponse;
    }
  }*/

  public HttpClient getObjRestClient() {
    return objRestClient;
  }

  public void setObjRestClient(HttpClient objRestClient) {
    this.objRestClient = objRestClient;
  }

  public String getStrHubEndPoint() {
    return strHubEndPoint;
  }

  public void setStrHubEndPoint(String strHubEndPoint) {
    this.strHubEndPoint = strHubEndPoint;
  }
}