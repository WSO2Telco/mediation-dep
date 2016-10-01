package entity.cep;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by aushani on 6/27/16.
 */

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
