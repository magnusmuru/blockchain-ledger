package ee.taltech.ledger.api;

import ee.taltech.ledger.api.controller.LedgerController;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Application {

  private static final Logger LOGGER = Logger.getLogger(Application.class.getName());

  public static void main(String[] args) {
    int port = 4567;
    try {
      port = Integer.parseInt(args[0]);
    } catch (NumberFormatException e) {
      LOGGER.log(Level.SEVERE, "Error parsing port {0}, defaulting to port 4567", args[0]);
    }
    LedgerController controller = new LedgerController(Integer.toString(port));
    controller.initialize();
  }
}
