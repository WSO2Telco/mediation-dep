package com.wso2telco.dep.mediation.unmashaller;

import com.wso2telco.dep.mediation.cep.Application;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aushani on 6/27/16.
 */
public class ServiceProviderDTO {

    private String spName;
    ServiceProviderDTO(){}
    private ServiceProviderDTO(String spName){
        this.spName = spName;

    }
    private List<Application> applicationList =new ArrayList<Application>();

    public String getSpName() {
        return spName;
    }

    public void setSpName(String spName) {
        this.spName = spName;
    }

    public List<Application> getApplicationList() {
        return applicationList;
    }

    public void setApplicationList(List<Application> applicationList) {
        this.applicationList = applicationList;
    }

    public ServiceProviderDTO clone(){
        return new ServiceProviderDTO(this.spName);
    }
}
