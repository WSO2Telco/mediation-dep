package com.wso2telco.dep.mediator.delegator;

import org.wso2.carbon.utils.CarbonUtils;

/**
 * Delegator class for {@link CarbonUtils}
 */
public class CarbonUtilsDelegator
{
  /**
   * @TODO This class wll remove after Salinda release the framework
   */
  private static final CarbonUtilsDelegator INSTANCE = new CarbonUtilsDelegator();

  public static CarbonUtilsDelegator getInstance()
  {
    return INSTANCE;
  }

  private CarbonUtilsDelegator()
  {
  }

  /**
   * Get carbon home
   *
   * @return
   */
  public String getCarbonHome()
  {
    return CarbonUtils.getCarbonHome();
  }

  /**
   * Get Carbon configuration directory path
   *
   * @return
   */
  public String getCarbonConfigDirPath()
  {
    return CarbonUtils.getCarbonConfigDirPath();
  }
}
