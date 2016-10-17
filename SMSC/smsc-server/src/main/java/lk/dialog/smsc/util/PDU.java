package lk.dialog.smsc.util;

public enum PDU {
  SUBMIT_SM(1),
  SUBMIT_SM_MULTI(2),
  QUERY_SM(3),
  DELIVER_SM(4);

  private int value = 0;

  private PDU(int i) {
    this.value = i;
  }

  public int value() {
    return value;
  }
}
