package ee.taltech.ledger.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import ee.taltech.ledger.api.models.Block;
import ee.taltech.ledger.api.models.IPAddress;
import ee.taltech.ledger.api.models.Ledger;
import okhttp3.*;

import java.io.IOException;

public class IPService {

  private final String apiKey = "84c1226e-8718-4ba7-8088-d6d3b2640d9d";
  private final FileReadWriteService readWriteService = new FileReadWriteService();
  private final OkHttpClient client = new OkHttpClient();
  private final ObjectMapper mapper = new ObjectMapper();

  private String blockPassUrl(IPAddress ipAddress) {
    return String.format("http://%s:%s/block/%s", ipAddress.getIp(), ipAddress.getPort(), apiKey);
  }

  public void updateIPAddressesFromFile(Ledger ledger) throws IOException {
    ledger.setIpAddresses(readWriteService.getIPs());
  }

  public void writeIPAddressesToFileAndLedger(Ledger ledger, IPAddress ipAddress) {
    ledger.addIPAddress(ipAddress);
    readWriteService.writeIPs(ledger.getIpAddresses());
  }

  public boolean compareAPIKey(String string) {
    return (string.equals(apiKey));
  }

  public void shareBlock(Block block, Ledger ledger) throws IOException {
    for (IPAddress ipAddress : ledger.getIpAddresses()) {
      String json = new Gson().toJson(block);

      RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);

      Request postRequest = new Request.Builder().url(blockPassUrl(ipAddress)).post(body).build();
      Response postResponse = client.newCall(postRequest).execute();
    }
  }
}
