/*
 *
 *  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package com.wso2telco.dep.mediator.entity.cep;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name="Group")
public class Group {

    private String groupName;
    private String operator;
    private String dayAmount;
    private String monthAmount;
    private List<ServiceProvider> serviceProviderList;

    public String getGroupName() {
        return groupName;
    }

   @XmlElement(name="GroupName")
    public void setGroupName(String groupName) {
        this.groupName = groupName;
   }

    public String getOperator() {
        return operator;
    }

    @XmlElement(name="Operator")
   public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getDayAmount() {
        return dayAmount;
    }
    @XmlElement(name="DayAmount")
    public void setDayAmount(String dayAmount) {
        this.dayAmount = dayAmount;
    }

    public String getMonthAmount() {
        return monthAmount;
   }

    @XmlElement(name = "MonthAmount")
    public void setMonthAmount(String monthAmount) {
        this.monthAmount = monthAmount;
   }



    public List<ServiceProvider> getServiceProviderList() {
        return serviceProviderList;
    }
    @XmlElement(name="ServiceProvider")
    public void setServiceProviderList(List<ServiceProvider> serviceProviderList) {
        this.serviceProviderList = serviceProviderList;
   }

}
