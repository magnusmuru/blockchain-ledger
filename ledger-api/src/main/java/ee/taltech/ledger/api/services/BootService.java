package ee.taltech.ledger.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.taltech.ledger.api.model.Block;
import ee.taltech.ledger.api.model.IPAddress;
import ee.taltech.ledger.api.model.Ledger;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BootService extends BaseService {
  private static final Logger LOGGER = Logger.getLogger(BootService.class.getName());

  private final ObjectMapper mapper = new ObjectMapper();

  public void runStartup(Ledger ledger, IPService ipService, String localPort) throws IOException {
    IPAddress local = IPAddress.builder().ip(InetAddress.getLocalHost().getHostAddress()).port(localPort).build();
    LOGGER.log(Level.INFO, "LOCAL: {0}:{1}", new String[]{local.getIp(), local.getPort()});

    ipService.updateIPAddressesFromFile(ledger);
    List<IPAddress> newIpAddresses = new ArrayList<>(ledger.getIpAddresses());

    List<IPAddress> fallbackIPs = FileReadWriteService.getFallbackIPs();
    if (fallbackIPs != null && !fallbackIPs.isEmpty()) newIpAddresses.addAll(fallbackIPs);

    for (IPAddress ipAddress : !ledger.getIpAddresses().isEmpty() ? ledger.getIpAddresses()
        : fallbackIPs != null ? fallbackIPs : new ArrayList<IPAddress>()) {
      try {
        Response response = sendGetRequest(ipRequestURL(ipAddress));
        if (response.isSuccessful() && !local.equals(ipAddress)) {
          ledger.addIPAddress(ipAddress);
          ipService.writeIPAddressesToFileAndLedger(ledger, ipAddress);
          addNewIpAddresses(newIpAddresses, response, localPort);
        }
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, "Error in BootService.runStartup: {0}", e.getMessage());
      }
    }
    for (IPAddress address : newIpAddresses) {
      if (!(address.getIp().equals(local.getIp()) && address.getPort().equals(local.getPort()))) {
        LOGGER.log(Level.INFO, "Local checking blocks from IP {0}", address.toPlainString());
        MediaType jsonMedia = MediaType.parse("application/json; charset=utf-8");
        String json = "{\"ip\":\"" +
            local.getIp() +
            "\",\"port\":\"" +
            local.getPort() +
            "\"}";
        try {
          Response postResponse = sendPostRequest(ipRequestURL(address),
              RequestBody.create(json, jsonMedia));
          Response blockResponse = sendGetRequest(blockRequestUrl(address));
          if (postResponse.isSuccessful() && blockResponse.isSuccessful()) {
            LOGGER.info("BootService.runStartup: Two way binding successful");
            ledger.addIPAddress(address);
            ipService.writeIPAddressesToFileAndLedger(ledger, address);
            addNewBlocks(ledger, blockResponse);
          }
        } catch (IOException e) {
          LOGGER.log(Level.WARNING, "Error in BootService.runStartup: {0}", e.getMessage());
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

  private void addNewIpAddresses(List<IPAddress> newIpAddresses, Response response, String localPort) throws IOException {
    IPAddress local = IPAddress.builder().ip(InetAddress.getLocalHost().getHostAddress()).port(localPort).build();
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
