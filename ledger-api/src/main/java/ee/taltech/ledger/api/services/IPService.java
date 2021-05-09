package ee.taltech.ledger.api.services;

import ee.taltech.ledger.api.model.IPAddress;
import ee.taltech.ledger.api.model.Ledger;

import java.io.IOException;
import java.util.HashSet;

public class IPService {

  private final FileReadWriteService readWriteService;

  public IPService(IPAddress ip) {
    this.readWriteService = new FileReadWriteService(ip);
  }

  public void updateIPAddressesFromFile(Ledger ledger) throws IOException {
    ledger.setIpAddresses(new HashSet<>(readWriteService.getIPs()));
  }

  public void writeIPAddressesToFileAndLedger(Ledger ledger, IPAddress ipAddress) {
    ledger.addIPAddress(ipAddress);
    readWriteService.writeIPs(new HashSet<IPAddress>(ledger.getIpAddresses()));
  }
}
