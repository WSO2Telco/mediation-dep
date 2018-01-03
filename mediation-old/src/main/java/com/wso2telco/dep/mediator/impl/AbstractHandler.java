package com.wso2telco.dep.mediator.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.synapse.MessageContext;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.utils.CarbonUtils;

import com.wso2telco.core.dbutils.fileutils.FileReader;
import com.wso2telco.dep.mediator.impl.smsmessaging.SMSHandler;
import com.wso2telco.dep.mediator.util.FileNames;
import com.wso2telco.dep.subscriptionvalidator.exceptions.ValidatorException;
import com.wso2telco.dep.subscriptionvalidator.services.MifeValidator;
import com.wso2telco.dep.subscriptionvalidator.util.ValidatorClassDTO;
import com.wso2telco.dep.subscriptionvalidator.util.ValidatorDBUtils;

public abstract class AbstractHandler implements SMSHandler {
	private static Map<String, String> mediatorConfMap;
	private static List<ValidatorClassDTO> validatorList = Collections.emptyList();

	@SuppressWarnings({ "deprecation", "static-access" })
	protected AbstractHandler() throws ValidatorException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		if (mediatorConfMap == null) {
			FileReader fileReader = new FileReader();
			String file = CarbonUtils.getCarbonConfigDirPath() + File.separator+ FileNames.MEDIATOR_CONF_FILE.getFileName();
			this.mediatorConfMap = fileReader.readPropertyFile(file);
		}
		if(validatorList.isEmpty()){
			validatorList = new ArrayList<ValidatorClassDTO>();
			loadValidatorList();
		}
	}
	protected void loadValidatorList() throws ValidatorException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		List<ValidatorClassDTO>	tempList =  ValidatorDBUtils.getValidatorClassForSMSSubscription();
		for (ValidatorClassDTO validatorClassDTO : tempList) {
			 if(!validatorList.contains(validatorClassDTO)){
				MifeValidator validator = (MifeValidator) Class.forName(validatorClassDTO.getClassName()).newInstance();
				 validatorClassDTO.setValidator(validator);
				 validatorList.add(validatorClassDTO);
			 }
		}

	}
	protected String getProperty(String key) {
		return mediatorConfMap.get(key);
	}

	protected boolean subscriptionValidate(MessageContext context) throws APIManagementException, ValidatorException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		int applicationId = Integer.parseInt((String)context.getProperty("APPLICATION_ID"));
		int apiId = Integer.parseInt((String) context.getProperty("API_ID"));
		if (isValidSubscription( applicationId, apiId,context)) {
			return true;
		}else{

		/**
		 * check for newly added subscriptions
		 */
			loadValidatorList();
			if (isValidSubscription(applicationId, apiId,context)) {
				return true;
			}
            throw new ValidatorException("No Validator class defined for the subscription with appID: " +applicationId + " apiID: " + apiId);
		}
	}

	protected boolean isValidSubscription(int applicationId,int apiId,MessageContext context){
		ValidatorClassDTO dto=new ValidatorClassDTO();
		dto.setApi(apiId);
		dto.setApp(applicationId);
		if (validatorList.contains(dto)) {
			ValidatorClassDTO temp= validatorList.get(validatorList.indexOf(dto));
			return temp.getValidator().validate(context);
		}
		return false;
	}
}
