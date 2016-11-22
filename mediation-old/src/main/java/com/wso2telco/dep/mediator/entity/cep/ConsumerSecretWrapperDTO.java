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

import com.wso2telco.dep.mediator.unmarshaler.GroupDTO;
import com.wso2telco.dep.mediator.unmarshaler.ServiceProviderDTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ConsumerSecretWrapperDTO implements Serializable{

    private  String consumerKey;

    private List< GroupDTO> consumerKeyVsGroup = new ArrayList<GroupDTO>();
    private List<ServiceProviderDTO> consumerKeyVsSp = new ArrayList<ServiceProviderDTO>();

    public String getConsumerKey() {
        return consumerKey;
    }
    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public List<GroupDTO> getConsumerKeyVsGroup() {
        return consumerKeyVsGroup;
    }

    public void setConsumerKeyVsGroup(List<GroupDTO> consumerKeyVsGroup) {
        this.consumerKeyVsGroup = consumerKeyVsGroup;
    }

    public List<ServiceProviderDTO> getConsumerKeyVsSp() {
        return consumerKeyVsSp;
    }

   public void setConsumerKeyVsSp(List<ServiceProviderDTO> consumerKeyVsSp) {
        this.consumerKeyVsSp = consumerKeyVsSp;
    }
}
