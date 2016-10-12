/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package mediator;

import exception.AxiataException;
import unmashaller.OparatorNotinListException;
import handler.SpendLimitHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import unmashaller.GroupDTO;
import unmashaller.GroupEventUnmarshaller;

import javax.xml.bind.JAXBException;
import java.io.IOException;


public class LimitCheckMediator extends AbstractMediator {

    private static final GroupEventUnmarshaller groupEventUnmarshaller = loadGroupEventUnmashaller();

    public boolean mediate(MessageContext mc) {

        String endUserId = mc.getProperty("endUserId").toString();
        String consumerKey = mc.getProperty("consumerKey").toString();
        String chargeAmount = mc.getProperty("chargeAmount").toString();
        String operator = mc.getProperty("operator").toString();

        String msisdn = endUserId.substring(5);
        try {
            checkSpendLimit(msisdn,operator,Double.parseDouble(chargeAmount),consumerKey);
        } catch (Exception axisFault) {
            handleException("Unexpected error in LimitCheckMediator ", axisFault, mc);
        }

        return true;
    }

    public boolean checkSpendLimit(String msisdn, String operator, Double chargeAmount, String consumerKey) throws
            com.axiata.dialog.dbutils.AxataDBUtilException, IOException, JAXBException {

        Double groupTotalDayAmount = 0.0;
        Double groupTotalMonthAmount = 0.0;



        try {
            GroupDTO groupDTO= groupEventUnmarshaller.getGroupDTO(operator,consumerKey);

            Double groupdailyLimit = Double.parseDouble(groupDTO.getDayAmount());
            Double groupMonlthlyLimit = Double.parseDouble(groupDTO.getMonthAmount());
            SpendLimitHandler spendLimitHandler = new SpendLimitHandler();

            groupTotalDayAmount = spendLimitHandler.getGroupTotalDayAmount(groupDTO.getGroupName(), groupDTO.getOperator(), msisdn);
            groupTotalMonthAmount = spendLimitHandler.getGroupTotalMonthAmount(groupDTO.getGroupName(), groupDTO.getOperator(), msisdn);

            if ((groupdailyLimit > 0.0) && ((groupTotalDayAmount >= groupdailyLimit) || (groupTotalDayAmount + chargeAmount) > groupdailyLimit || chargeAmount > groupdailyLimit)) {
                log.debug("group daily limit exceeded");
                throw new AxiataException("POL1001", "The %1 charging limit for this user has been exceeded", new String[]{"daily"});
            }

            if ((groupMonlthlyLimit) >0.0 && ((groupTotalMonthAmount >= groupMonlthlyLimit) || (groupTotalMonthAmount + chargeAmount) > groupMonlthlyLimit || chargeAmount > groupMonlthlyLimit)) {
                log.debug("group monthly limit exceeded");
                throw new AxiataException("POL1001", "The %1 charging limit for this user has been exceeded", new String[]{"monthly"});
            }
            return true;

        } catch (OparatorNotinListException e){
            return true;
        }
    }


    private static GroupEventUnmarshaller loadGroupEventUnmashaller(){
        try {
            GroupEventUnmarshaller.startGroupEventUnmarshaller();
        } catch (JAXBException e) {
            return null;
        }

        return GroupEventUnmarshaller.getInstance();
    }

}
