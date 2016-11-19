package com.wso2telco.dep.spend.limit.mediation.entity.cep;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by aushani on 6/27/16.
 */

@XmlRootElement(name="ServiceProvider")
public class ServiceProvider {

    private String spName;
    private List<Application> applicationList;

    public String getSpName() {
        return spName;
    }

    @XmlElement(name="SPName")
    public void setSpName(String spName) {
        this.spName = spName;
    }


    public List<Application> getApplicationList() {
        return applicationList;
    }

    @XmlElement(name="Application")
    public void setApplicationList(List<Application> applicationList) {
        this.applicationList = applicationList;
    }
}
