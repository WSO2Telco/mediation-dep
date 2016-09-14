package com.wso2telco.core.datamigrator;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.wso2telco.core.datamigrator.dao.RequestDAO;
import com.wso2telco.core.datamigrator.dao.ResultsDAO;
import com.wso2telco.core.datamigrator.dto.RequestDTO;
import com.wso2telco.core.datamigrator.dto.ResultDTO;
import com.wso2telco.core.datamigrator.exception.PCRException;
import com.wso2telco.core.datamigrator.util.JedisUtil;
import com.wso2telco.core.datamigrator.util.OldPcrCreator;
import com.wso2telco.core.datamigrator.util.SectorUtil;

import redis.clients.jedis.Jedis;

public class DataMigrator {

	private static Logger log = Logger.getLogger(DataMigrator.class);
	
	public static void main(String[] args) throws PCRException {		
		setApplicationData();
		setRequestData();
	}
	
	public static void setApplicationData() throws PCRException{
		ResultsDAO resultsDAO = new ResultsDAO();
		ArrayList<ResultDTO> results = resultsDAO.getResults();
		Jedis jedis = JedisUtil.getJedis();
		for (ResultDTO resultDTO : results) {
			String callbackUrl = resultDTO.getCallbackUrl();
			String sector = SectorUtil.getSectorIdFromUrl(callbackUrl);
			jedis.set(sector + ":" + resultDTO.getConsumerKey(), "true");
			log.debug("------------------------------------");
			log.debug("consumer key : " + resultDTO.getConsumerKey());
			log.debug("callback url : " + resultDTO.getCallbackUrl());
			log.debug("sector : " + sector);
			log.debug("application id : " + resultDTO.getApplicationId());
		}
	}
	
	public static void setRequestData() throws PCRException{
		RequestDAO requestDAO = new RequestDAO();
		ArrayList<RequestDTO> requests = requestDAO.getRequests();
		Jedis jedis = JedisUtil.getJedis();
		for (RequestDTO requestDTO : requests) {
			String callbackUrl = requestDTO.getCallbackurl();
			String sector = SectorUtil.getSectorIdFromUrl(callbackUrl);
			String msisdn = requestDTO.getMsisdn();
			String consumerKey = requestDTO.getConsumerKey();
			String applicationName = requestDTO.getApplicationName();
			String pcr = OldPcrCreator.createPcr(msisdn,applicationName);
			
			jedis.set(msisdn + ":" + sector + ":" + consumerKey, pcr);			
			log.debug("consumer key : " + consumerKey);
			log.debug("callback url : " + callbackUrl);
			log.debug("msisdn : " + msisdn);
			log.debug("sector : " + sector);
			log.debug("Application Name : " + applicationName);
			log.debug("pcr : " + pcr);
		}
	}
}
