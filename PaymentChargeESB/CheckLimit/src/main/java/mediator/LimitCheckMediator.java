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

import com.axiata.dialog.dbutils.AxataDBUtilException;
import com.axiata.dialog.dbutils.AxiataDbService;
import com.axiata.dialog.dbutils.dao.SpendLimitDAO;
import exception.AxiataException;
import org.apache.synapse.SynapseException;
import unmashaller.OparatorNotinListException;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import unmashaller.GroupDTO;
import unmashaller.GroupEventUnmarshaller;

import javax.xml.bind.JAXBException;


public class LimitCheckMediator extends AbstractMediator {

    private static final GroupEventUnmarshaller groupEventUnmarshaller = loadGroupEventUnmashaller();
    private AxiataDbService dbservice = new AxiataDbService();;

    public boolean mediate(MessageContext mc) throws AxiataException {
        try {
            String endUserId = mc.getProperty("endUserId").toString();
            String consumerKey = mc.getProperty("consumerKey").toString();
            String chargeAmount = mc.getProperty("chargeAmount").toString();
            String operator = mc.getProperty("operator").toString();
            String msisdn = endUserId.substring(5);
            checkSpendLimit(msisdn,operator,Double.parseDouble(chargeAmount),consumerKey);
        } catch (AxiataException e) {
            mc.setProperty("FAULT_CODE", e.getErrcode());
            mc.setProperty("FAULT_MSG", e.getErrmsg());
            mc.setProperty("FAULT_VARIABLE", e.getErrvar()[0]);
            throw e;
        } catch (AxataDBUtilException e) {
            mc.setProperty("FAULT_CODE", "500");
            mc.setProperty("FAULT_MSG", e.getMessage());
            throw new SynapseException(e.getMessage());
        }
        return true;
    }

    public boolean checkSpendLimit(String msisdn, String operator, Double chargeAmount, String consumerKey) throws
            AxataDBUtilException , AxiataException{
        try {
            GroupDTO groupDTO= groupEventUnmarshaller.getGroupDTO(operator,consumerKey);
            Double groupdailyLimit = Double.parseDouble(groupDTO.getDayAmount());
            Double groupMonlthlyLimit = Double.parseDouble(groupDTO.getMonthAmount());
            SpendLimitDAO daySpendLimitObj = null;
            SpendLimitDAO monthSpendLimitObj = null;



            if(groupdailyLimit > 0.0) {

                if(chargeAmount <= groupdailyLimit) {
                    daySpendLimitObj = getGroupTotalDayAmount(groupDTO.getGroupName(), groupDTO.getOperator(), msisdn);
                    if(daySpendLimitObj!=null && ((daySpendLimitObj.getAmount() >= groupdailyLimit) || (daySpendLimitObj.getAmount() + chargeAmount) > groupdailyLimit  )){
                        log.debug("group daily limit exceeded");
                        throw new AxiataException("POL1001", "The %1 charging limit for this user has been exceeded", new String[]{"daily"});
                    }

                } else {
                    log.debug("Charge Amount exceed the limit");
                    throw new AxiataException("POL1001", "The %1 charging limit for this user has been exceeded", new String[]{"daily"});
                }
            }

            if(groupMonlthlyLimit > 0.0) {

                if(chargeAmount < groupMonlthlyLimit) {
                    monthSpendLimitObj = getGroupTotalMonthAmount(groupDTO.getGroupName(), groupDTO.getOperator(), msisdn);
                    if(monthSpendLimitObj!=null && (monthSpendLimitObj.getAmount() >= groupMonlthlyLimit || monthSpendLimitObj.getAmount() + chargeAmount > groupMonlthlyLimit) ){
                        log.debug("group monthly limit exceeded");
                        throw new AxiataException("POL1001", "The %1 charging limit for this user has been exceeded", new String[]{"monthly"});
                    }

                } else {
                    log.debug("group monthly limit exceeded");
                    throw new AxiataException("POL1001", "The %1 charging limit for this user has been exceeded", new String[]{"monthly"});
                }

            }

        }catch (OparatorNotinListException e){
            return true;
        }catch (AxataDBUtilException e) {
            throw new AxataDBUtilException("Data retreving error");
        }

        return true;
    }


    private static GroupEventUnmarshaller loadGroupEventUnmashaller(){
        try {
            GroupEventUnmarshaller.startGroupEventUnmarshaller();
        } catch (JAXBException e) {
            return null;
        }

        return GroupEventUnmarshaller.getInstance();
    }

    private SpendLimitDAO getGroupTotalDayAmount(String groupName,String operator,String msisdn) throws
            AxataDBUtilException {
        return dbservice.getGroupTotalDayAmount(groupName,operator,msisdn);
    }

    private SpendLimitDAO getGroupTotalMonthAmount(String groupName,String operator,String msisdn) throws AxataDBUtilException {
        return dbservice.getGroupTotalMonthAmount(groupName,operator,msisdn);
    }


}
