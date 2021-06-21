package ee.taltech.ledger.api.services;

import ee.taltech.ledger.api.model.IPAddress;
import ee.taltech.ledger.api.model.Ledger;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

public class IPService {

  private final FileReadWriteService readWriteService;

  public IPService(IPAddress ip) {
    this.readWriteService = new FileReadWriteService(ip);
  }

  public void writeIPAddressesToFileAndLedger(Ledger ledger, IPAddress ipAddress) {
    ledger.addIPAddress(ipAddress);
    readWriteService.writeIPs(new HashSet<>(ledger.getIpAddresses()));
  }

  public List<IPAddress> loadSavedIPs() throws IOException {
    return readWriteService.getIPs();
  }

  public static List<IPAddress> loadFallbackIPs() {
    return FileReadWriteService.getFallbackIPs();
  }
}
