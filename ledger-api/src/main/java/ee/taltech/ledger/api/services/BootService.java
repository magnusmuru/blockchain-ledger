package ee.taltech.ledger.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.taltech.ledger.api.model.Block;
import ee.taltech.ledger.api.model.IPAddress;
import ee.taltech.ledger.api.model.Ledger;
import okhttp3.FormBody;
import okhttp3.Response;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BootService extends BaseService {
  private static final Logger LOGGER = Logger.getLogger(BootService.class.getName());

  private final ObjectMapper mapper = new ObjectMapper();

  public void runStartup(Ledger ledger, IPService ipService) throws IOException {
    IPAddress local = IPAddress.builder().ip(InetAddress.getLocalHost().getHostAddress()).port("4567").build();
    ipService.updateIPAddressesFromFile(ledger);
    List<IPAddress> newIpAddresses = new ArrayList<>(ledger.getIpAddresses());
    for (IPAddress ipAddress : ledger.getIpAddresses()) {
      Response response = sendGetRequest(ipRequestURL(ipAddress));
      if (response.isSuccessful()) {
        addNewIpAddresses(newIpAddresses, response);
      }
    }
    for (IPAddress address : newIpAddresses) {
      if (!ledger.getIpAddresses().contains(address)) {
        ledger.addIPAddress(address);
      }
      if (!address.getIp().equals(local.getIp())) {
        Response postResponse = sendPostRequest(ipRequestURL(address), new FormBody.Builder().build());
        Response blockResponse = sendGetRequest(blockRequestUrl(address));
        if (postResponse.isSuccessful() && blockResponse.isSuccessful()) {
          LOGGER.info("BootService.runStartup: Two way binding successful");
          addNewBlocks(ledger, blockResponse);
        }
      }
    }
    findLastHashOnBootBlockchainIngest(ledger);
  }

  private void addNewBlocks(Ledger ledger, Response blockResponse) throws IOException {
    try {
      List<Block> chainBlocks = new ArrayList<>(mapper.readValue(Objects.requireNonNull(blockResponse.body()).byteStream(),
          mapper.getTypeFactory().constructCollectionType(List.class, Block.class)));
      chainBlocks.stream()
          .filter(block -> !ledger.getBlocks().containsKey(block.getHash()))
          .forEach(ledger::addBlock);
    } catch (NullPointerException e) {
      LOGGER.info("Bootservice.runStartup: ledger has no blocks to ingest");
    }
  }

  private void addNewIpAddresses(List<IPAddress> newIpAddresses, Response response) throws IOException {
    IPAddress local = IPAddress.builder().ip(InetAddress.getLocalHost().getHostAddress()).port("4567").build();
    List<IPAddress> ipAddresses = mapper.readValue(Objects.requireNonNull(response.body()).byteStream(),
        mapper.getTypeFactory().constructCollectionType(List.class, IPAddress.class));
    newIpAddresses.addAll(
        ipAddresses.stream()
            .filter(ip -> !ip.equals(local))
            .collect(Collectors.toList())
    );
  }

  private void findLastHashOnBootBlockchainIngest(Ledger ledger) {
    HashingService hashingService = new HashingService();
    String genesisHash = hashingService.genesisHash();
    if (ledger.getBlocks().containsKey(genesisHash)) {
      String hash = hashingService.generateSHA256Hash(ledger.getBlocks().get(genesisHash));
      while (ledger.getBlocks().containsKey(hash)) {
        hash = hashingService.generateSHA256Hash(ledger.getBlocks().get(hash));
      }
      ledger.setLastHash(hash);
    } else {
      ledger.setLastHash(genesisHash);
    }
  }
}
