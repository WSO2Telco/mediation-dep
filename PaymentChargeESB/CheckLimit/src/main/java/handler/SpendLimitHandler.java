package handler;

import com.axiata.dialog.dbutils.AxataDBUtilException;
import com.axiata.dialog.dbutils.AxiataDbService;
import com.axiata.dialog.dbutils.dao.SpendLimitDAO;
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

    public SpendLimitDAO getGroupTotalDayAmount(String groupName,String operator,String msisdn) throws AxataDBUtilException {

        SpendLimitDAO spendLimitDAO = null;
        if (isEventsEnabled) {
            //            HazelcastInstance hazelcastInstance = MifeEventsDataHolder.getHazelcastInstance();
            //            IMap<String, Object> cacheMap = hazelcastInstance.getMap(MifeEventsConstants.OPERATOR_HAZELCAST_MAP_NAME);
            //            return cacheMap.containsKey(operatorId);
            return dbservice.getGroupTotalDayAmount(groupName,operator,msisdn);
        }
        return spendLimitDAO;
    }

    public SpendLimitDAO getGroupTotalMonthAmount(String groupName,String operator,String msisdn) throws AxataDBUtilException {

        SpendLimitDAO spendLimitDAO = null;
        if (isEventsEnabled) {

            return dbservice.getGroupTotalMonthAmount(groupName,operator,msisdn);
        }
        return spendLimitDAO;
    }


}
