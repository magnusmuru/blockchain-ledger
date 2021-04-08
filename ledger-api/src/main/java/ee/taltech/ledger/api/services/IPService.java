package ee.taltech.ledger.api.services;

import ee.taltech.ledger.api.model.IPAddress;
import ee.taltech.ledger.api.model.Ledger;

import java.io.IOException;

public class IPService {

  private final IPAddress localIp;

  private final FileReadWriteService readWriteService;

  public IPService(IPAddress ip) {
    this.localIp = ip;
    this.readWriteService = new FileReadWriteService(localIp);
  }

  public void updateIPAddressesFromFile(Ledger ledger) throws IOException {
    ledger.setIpAddresses(readWriteService.getIPs());
  }

  public void writeIPAddressesToFileAndLedger(Ledger ledger, IPAddress ipAddress) {
    ledger.addIPAddress(ipAddress);
    readWriteService.writeIPs(ledger.getIpAddresses());
  }
}
