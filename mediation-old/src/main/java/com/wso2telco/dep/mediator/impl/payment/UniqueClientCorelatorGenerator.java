package com.wso2telco.dep.mediator.impl.payment;

import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.json.JSONException;
import org.json.JSONObject;


public class UniqueClientCorelatorGenerator extends AbstractMediator {


	/**class name="org.wso2telco.dep.nashornmediator.NashornMediator">
	 <property name="script" value="
	 var payload= mc.getPayloadJSON();
	 var hubGateway = mc.getProperty('hubGateway').trim();
	 var requestResourceUrl = mc.getProperty('requestResourceUrl').trim();
	 var requestID = mc.getProperty('requestID').trim();
	 var originalClientCorrelator = mc.getProperty('originalClientCorrelator');
	 if (originalClientCorrelator != null &amp;&amp; originalClientCorrelator !='') {
	 payload.amountTransaction.clientCorrelator = originalClientCorrelator;
	 } else {
	 delete payload.amountTransaction.clientCorrelator;
	 }
	 payload.amountTransaction.resourceURL = hubGateway + requestResourceUrl + requestID;
	 mc.setPayloadJSON(payload);
	 "/>
	 </class>
	 */
	public boolean mediate(MessageContext context) { 
		
		try{


			String hubGateway = context.getProperty("hubGateway") == null ? "": context.getProperty("hubGateway").toString().trim();
			String requestResourceUrl = context.getProperty("requestResourceUrl") == null ? "":
					context.getProperty("requestResourceUrl").toString().trim();
			String requestID =  context.getProperty("requestID") == null ? "":context.getProperty("requestID").toString().trim();


			String jsonPayloadToString = JsonUtil.jsonPayloadToString(((Axis2MessageContext) context)
					.getAxis2MessageContext());

			JSONObject jsonBody = new JSONObject(jsonPayloadToString);

			if( context.getProperty("originalClientCorrelator") == null || "".equals(context.getProperty("originalClientCorrelator")))
			{
				((JSONObject)jsonBody.get("amountTransaction")).remove("clientCorrelator");
				log.info("Removing Client correlator : "+jsonBody);
			}else {
				String originalClientCorrelator = context.getProperty("originalClientCorrelator").toString().trim();
				((JSONObject)jsonBody.get("amountTransaction")).put("clientCorrelator",originalClientCorrelator);
				log.info("Adding original client correlator : "+ jsonBody);
			}


			((JSONObject)jsonBody.get("amountTransaction")).put("resourceURL",hubGateway + requestResourceUrl + requestID);


			String transformedJson = jsonBody.toString();
			JsonUtil.newJsonPayload(
				    ((Axis2MessageContext) context).getAxis2MessageContext(),
				    transformedJson, true, true);
		    
		}catch(JSONException e){
			log.error("Error occured while processing JSON : ",e);
			
		}
		
		return true;
	}
}
