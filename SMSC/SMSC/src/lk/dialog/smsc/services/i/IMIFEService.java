package lk.dialog.smsc.services.i;

import java.io.IOException;

import org.apache.http.HttpResponse;

import lk.dialog.smsc.exception.SMSCException;


/**
 * @author prageeth
 */
public interface IMIFEService {

	public HttpResponse handleRequest( String strURLParam, String strJSONParam, String strAccessToken ) throws SMSCException, UnsupportedOperationException, IOException;

}
