package com.wso2telco.dep.mediator.impl.payment;

import org.apache.synapse.MessageContext; 
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.commons.json.JsonUtil;
import org.json.JSONException;
import org.json.JSONObject;


public class ClientCorelatorMediator extends AbstractMediator { 

	public boolean mediate(MessageContext context) { 
		
		try{

				    if(context.getProperty("clientCorrelator") == null){
				    	return true;
					}

			String jsonPayloadToString = JsonUtil.jsonPayloadToString(((Axis2MessageContext) context)
					.getAxis2MessageContext());

			JSONObject jsonBody = new JSONObject(jsonPayloadToString);
			String clientCorrelatorNew = context.getProperty("clientCorrelator").toString() ;
			log.debug("Client Correlator New : " + clientCorrelatorNew);
			//jsonBody.put("amountTransaction.clientCorrelator", clientCorrelatorNew);
			((JSONObject)jsonBody.get("amountTransaction")).put("clientCorrelator",clientCorrelatorNew);

				    String transformedJson = jsonBody.toString();
			log.info("New transformed JSON : " + transformedJson);
			JsonUtil.newJsonPayload(
				    ((Axis2MessageContext) context).getAxis2MessageContext(),
				    transformedJson, true, true);
		    
		}catch(JSONException e){
			log.error("Error occured while processing JSON : ",e);
			
		}
		
		return true;
	}
}
