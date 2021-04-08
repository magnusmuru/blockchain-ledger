package ee.taltech.ledger.api;

import ee.taltech.ledger.api.controller.LedgerController;
import ee.taltech.ledger.api.dto.IpDTO;

import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Application {

  private static final Logger LOGGER = Logger.getLogger(Application.class.getName());

  public static void main(String[] args) throws UnknownHostException {
    try {
      if (args.length != 0) {
        String[] parts = args[0].split(":");
        IpDTO master = new IpDTO(parts[0], parts[1]);
        LedgerController controller = new LedgerController(master);
        controller.initialize();
      } else {
        LOGGER.log(Level.SEVERE, "Argument ip:port missing");
      }
    } catch (IndexOutOfBoundsException e) {
      LOGGER.log(Level.SEVERE, "Error parsing ip:port {0}", args[0]);
    }
  }
}
