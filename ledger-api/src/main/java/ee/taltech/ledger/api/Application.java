package ee.taltech.ledger.api;

import ee.taltech.ledger.api.controller.LedgerController;
import ee.taltech.ledger.api.model.IPAddress;

import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Application {
  //java -jar ledger-api\build\libs\ledger-api-all.jar 10.90.2.129:5000
  private static final Logger LOGGER = Logger.getLogger(Application.class.getName());

  public static void main(String[] args) throws UnknownHostException {
    try {
      if (args.length != 0) {
        String[] parts = args[0].split(":");
        IPAddress master = new IPAddress(parts[0], parts[1]);
        LedgerController controller = new LedgerController(master);
        controller.initialize();
      } else {
        LOGGER.log(Level.SEVERE, "Argument ip:port missing");
      }
    } catch (IndexOutOfBoundsException e) {
      LOGGER.log(Level.SEVERE, "Error parsing ip:port {0}", args[0]);
    } catch (NoSuchAlgorithmException e) {
      LOGGER.log(Level.SEVERE, "Error creating a keypair: {0}", e.getMessage());
    }
  }
}
