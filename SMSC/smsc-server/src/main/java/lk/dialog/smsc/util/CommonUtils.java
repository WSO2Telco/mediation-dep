package lk.dialog.smsc.util;

import java.util.Date;

public class CommonUtils {
  public static long getUniqueNumber() {
    //TODO unique number logic here
    return new Date().getTime();
  }
}
