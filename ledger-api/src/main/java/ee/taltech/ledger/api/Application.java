package ee.taltech.ledger.api;

import ee.taltech.ledger.api.controller.LedgerController;

public class Application {
  public static void main(String[] args) {
    LedgerController controller = new LedgerController();
    controller.initialize();
  }
}
