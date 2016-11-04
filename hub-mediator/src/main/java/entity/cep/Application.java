package entity.cep;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by aushani on 6/27/16.
 */

@XmlRootElement(name = "Application")
public class Application {

    private String applicationName;
    private String consumerKey;

    Application(){}
    private Application(String applicationName,String consumerKey){
        this.applicationName=applicationName;
        this.consumerKey =consumerKey;
    }

    public String getApplicationName() {
        return applicationName;
    }

   @XmlElement(name = "ApplicationName")
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    @XmlElement(name="ConsumerKey")
    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public Application clone(){
        return  new Application(this.applicationName,this.consumerKey);
    }

}
