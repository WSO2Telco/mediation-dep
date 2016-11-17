package com.wso2telco.dep.mediation.unmashaller;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aushani on 6/27/16.
 */
public class GroupDTO {

    private String groupName;
    private String operator;
    private String dayAmount;
    private String monthAmount;
    private String userInfoEnabled;
    private List<ServiceProviderDTO> serviceProviderList =new ArrayList<ServiceProviderDTO>();

    GroupDTO(){

    }
    private GroupDTO( String groupName,String operator, String dayAmount,String monthAmount, String useInfoEnabled){
        this.groupName=groupName;
        this.operator =operator;
        this.dayAmount=dayAmount;
        this.monthAmount=monthAmount;
        this.userInfoEnabled=useInfoEnabled;
    }
    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getDayAmount() {
        return dayAmount;
    }

    public void setDayAmount(String dayAmount) {
        this.dayAmount = dayAmount;
    }

    public String getMonthAmount() {
        return monthAmount;
    }

    public void setMonthAmount(String monthAmount) {
        this.monthAmount = monthAmount;
    }

    public String getUserInfoEnabled() {
        return userInfoEnabled;
    }

    public void setUserInfoEnabled(String userInfoEnabled) {
        this.userInfoEnabled = userInfoEnabled;
    }

    public List<ServiceProviderDTO> getServiceProviderList() {
        return serviceProviderList;
    }

    public void setServiceProviderList(List<ServiceProviderDTO> serviceProviderList) {
        this.serviceProviderList = serviceProviderList;


    }


    public GroupDTO clone(){
        return new GroupDTO(this.groupName,this.operator, this.dayAmount,this.monthAmount, this.userInfoEnabled);
    }
}
