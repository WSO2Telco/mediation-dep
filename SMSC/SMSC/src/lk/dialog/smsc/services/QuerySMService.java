package lk.dialog.smsc.services;

import java.io.IOException;

import org.apache.http.HttpResponse;

import lk.dialog.smsc.exception.SMSCException;
import lk.dialog.smsc.services.i.IMIFEService;
import lk.dialog.smsc.util.RESTClient;

public class QuerySMService implements IMIFEService{

	@Override
	public HttpResponse handleRequest(String strURLParam, String strJSONParam,
			String strAccessToken) throws SMSCException,
			UnsupportedOperationException, IOException {
		
		RESTClient objREST = new RESTClient();
		HttpResponse objResponse = objREST.get( strURLParam, strJSONParam, strAccessToken );
		
		return objResponse;

	}

}
