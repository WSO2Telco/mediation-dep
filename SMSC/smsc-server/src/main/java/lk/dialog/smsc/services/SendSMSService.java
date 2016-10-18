package lk.dialog.smsc.services;

import java.io.IOException;
import java.net.URI;

import lk.dialog.smsc.exception.SMSCException;
import lk.dialog.smsc.mife.response.sendsms.OutboundSMSMessageRequest;
import lk.dialog.smsc.services.i.IMIFEService;
import lk.dialog.smsc.util.RESTClient;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;

public class SendSMSService implements IMIFEService {

	@Override
	public HttpResponse handleRequest(String strURLParam, String strJSONParam,
			String strAccessToken) throws SMSCException,
			UnsupportedOperationException, IOException {

		RESTClient objREST = new RESTClient();
		HttpResponse objResponse = objREST.post( strURLParam, strJSONParam, strAccessToken );
		
		return objResponse;
	}

	//Below method is for test purposes only :: callDummyMethod
	private HttpResponse callDummyMethod() {
		try {
			HttpClient client = HttpClientBuilder.create().build();
			HttpPost post = new HttpPost("http://localhost:8081/DummyMIFEServices/mife/balance");
			post.setHeader("Content-Type", "application/json");
			URI uri = new URIBuilder(post.getURI())
					.addParameter("grant_type", "password")
					.addParameter("username", "inova")
					.addParameter("password", "inova123").build();
			post.setURI(uri);
			HttpResponse resp = client.execute(post);
			String json = EntityUtils.toString(resp.getEntity());
//			SendSMSResponseEntity s = new Gson().fromJson(json, SendSMSResponseEntity.class);
//			System.out.println(s.getOutboundSMSMessageRequest().getOutboundSMSTextMessage().getMessage());
			OutboundSMSMessageRequest s = new Gson().fromJson(json, OutboundSMSMessageRequest.class);
			System.out.println(s.getOutboundSMSMessageRequest().getOutboundSMSTextMessage().getMessage());
			return resp;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
