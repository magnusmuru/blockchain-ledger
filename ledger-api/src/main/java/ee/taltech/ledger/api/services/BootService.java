package ee.taltech.ledger.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import ee.taltech.ledger.api.model.Block;
import ee.taltech.ledger.api.model.IPAddress;
import ee.taltech.ledger.api.model.Ledger;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.Map.Entry;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BootService extends BaseService {
  private static final Logger LOGGER = Logger.getLogger(BootService.class.getName());

  private final ObjectMapper mapper = new ObjectMapper();
  private final Gson gson = new Gson();

  public void runStartup(Ledger ledger, IPService ipService, String localPort) throws IOException {
    IPAddress local = IPAddress.builder().ip(InetAddress.getLocalHost().getHostAddress()).port(localPort).build();
    LOGGER.log(Level.INFO, "LOCAL: {0}:{1}", new String[]{local.getIp(), local.getPort()});

    List<IPAddress> savedNodes = new ArrayList<>(ipService.loadSavedIPs());
    List<IPAddress> fallbackIPs = IPService.loadFallbackIPs();
    List<IPAddress> knownNodes = new ArrayList<>();

    if (fallbackIPs != null && !fallbackIPs.isEmpty()) savedNodes.addAll(fallbackIPs);

    for (IPAddress ipAddress : savedNodes) {
      try {
        Response response = sendGetRequest(ipRequestURL(ipAddress));
        if (response.isSuccessful() && !local.equals(ipAddress)) {
          ledger.addIPAddress(ipAddress);
          ipService.writeIPAddressesToFileAndLedger(ledger, ipAddress);
          if (!knownNodes.contains(ipAddress)) knownNodes.add(ipAddress);
          addNewIpAddresses(knownNodes, response, local);
        }
        response.close();
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, "Error in BootService.runStartup: {0}", e.getMessage());
      }
    }
    for (IPAddress address : knownNodes) {
      LOGGER.log(Level.INFO, "Local checking blocks from IP {0}", address.toPlainString());
      try {
        Response postResponse = sendPostRequest(ipRequestURL(address),
            RequestBody.create(gson.toJson(local), MediaType.parse("application/json; charset=utf-8")));
        Response blockResponse = sendGetRequest(blockRequestUrl(address));
        if (postResponse.isSuccessful() && blockResponse.isSuccessful()) {
          LOGGER.info("BootService.runStartup: Two way binding successful");
          ledger.addIPAddress(address);
          ipService.writeIPAddressesToFileAndLedger(ledger, address);
          addNewBlocks(ledger, blockResponse);
        }
        postResponse.close();
        blockResponse.close();
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, "Error in BootService.runStartup: {0}", e.getMessage());
      }
    }
    setLedgerLastHash(ledger);
  }

  private void addNewBlocks(Ledger ledger, Response blockResponse) throws IOException {
    if (blockResponse.body() == null) {
      LOGGER.info("Bootservice.runStartup: ledger has no blocks to ingest");
      return;
    }
    try {
      List<Block> chainBlocks = Arrays.asList(gson.fromJson(Objects.requireNonNull(blockResponse.body()).string(), Block[].class));
      chainBlocks.stream()
          .filter(block -> !ledger.getBlocks().containsKey(block.getHash()))
          .forEach(ledger::addBlock);
    } catch (NullPointerException e) {
      LOGGER.info("Bootservice.runStartup: ledger has no blocks to ingest");
    }
  }

  private void addNewIpAddresses(List<IPAddress> newIpAddresses, Response response, IPAddress local) throws IOException {
    List<IPAddress> ipAddresses = mapper.readValue(Objects.requireNonNull(response.body()).byteStream(),
        mapper.getTypeFactory().constructCollectionType(List.class, IPAddress.class));
    newIpAddresses.addAll(
        ipAddresses.stream()
            .filter(ip -> !ip.equals(local))
            .collect(Collectors.toList())
    );
  }

  private void setLedgerLastHash(Ledger ledger) {
    if (ledger.getBlocks().isEmpty()) {
      BlockService.createGenesisBlock(ledger);
      LOGGER.log(Level.INFO, "Created genesis block with hash {0}", ledger.getLastHash());
    } else {
      ledger.setLastHash(ledger.getBlocks().entrySet().stream()
          .max(Comparator.comparingInt((Entry<String, Block> e) -> e.getValue().getNr()))
          .map(entry -> entry.getValue().getHash()).orElse("0"));
      LOGGER.log(Level.INFO, "Set ledgers last hash to {0}", ledger.getLastHash());
    }
  }
}
