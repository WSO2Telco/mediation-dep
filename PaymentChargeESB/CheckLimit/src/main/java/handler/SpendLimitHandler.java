package handler;

import com.axiata.dialog.dbutils.AxataDBUtilException;
import com.axiata.dialog.dbutils.AxiataDbService;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class SpendLimitHandler {

    private final boolean isEventsEnabled;
    private AxiataDbService dbservice;
    public static final String MIFE_EVENTS_PROPERTIES_FILE = "mife-events.properties";
    public static final String CEP_SPEND_LIMIT_HANDLER_ENABLED_PROPERTY = "mife.events.spend.limit.handler.enabled";

    public SpendLimitHandler() {
        Properties properties = loadEventProperties();
        String eventsEnabled = properties.getProperty(CEP_SPEND_LIMIT_HANDLER_ENABLED_PROPERTY);

        isEventsEnabled = Boolean.parseBoolean(eventsEnabled);
        dbservice = new AxiataDbService();
    }

    private Properties loadEventProperties(){
        String configPath = CarbonUtils.getCarbonConfigDirPath() + File.separator + MIFE_EVENTS_PROPERTIES_FILE;
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(configPath));
        } catch (IOException e) {
           //log.error("Error while loading mife-events.properties file", e);
        }
        return props;
    }

    public Double getGroupTotalDayAmount(String groupName, String operator, String msisdn) throws AxataDBUtilException {
        return this.isEventsEnabled?this.dbservice.getGroupTotalDayAmount(groupName, operator, msisdn):Double.valueOf(0.0D);
    }

    public Double getGroupTotalMonthAmount(String groupName, String operator, String msisdn) throws AxataDBUtilException {
        return this.isEventsEnabled?this.dbservice.getGroupTotalMonthAmount(groupName, operator, msisdn):Double.valueOf(0.0D);
    }

}
