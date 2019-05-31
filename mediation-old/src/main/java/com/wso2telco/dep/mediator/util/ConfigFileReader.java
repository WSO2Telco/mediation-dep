package com.wso2telco.dep.mediator.util;

import java.util.Collections;
import java.util.Map;

import com.google.common.base.Joiner;
import framework.configuration.ConfigFile;
import framework.configuration.impl.ConfigFileReaderServiceImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is to wrap the configuration service.
 * Introduced to preserve existing behaviour of the FileReader class and to reduce maintenance cost.
 */
public final class ConfigFileReader
{

  private static final ConfigFileReader INSTANCE = new ConfigFileReader();

  private Log log = LogFactory.getLog(this.getClass());

  private ConfigFileReader()
  {
  }

  public static ConfigFileReader getInstance()
  {
    return INSTANCE;
  }

  /**
   * Get mediator conf file properties
   *
   * @return
   */
  public Map<String, String> getMediatorConfigMap()
  {
    try
    {
      return ConfigFileReaderServiceImpl.getInstance().readFile(ConfigFile.MEDIATOR_CONF);
    }
    catch (Exception e)
    {
      log.error(Joiner.on(" ").join("Unable to read", ConfigFile.MEDIATOR_CONF.getName(), " file."));

      return Collections.emptyMap();
    }
  }
}
