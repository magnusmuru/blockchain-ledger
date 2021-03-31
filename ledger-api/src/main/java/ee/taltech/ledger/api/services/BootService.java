package ee.taltech.ledger.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.taltech.ledger.api.model.Block;
import ee.taltech.ledger.api.model.IPAddress;
import ee.taltech.ledger.api.model.Ledger;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class BootService {
  private static final Logger LOGGER = Logger.getLogger(BootService.class.getName());

  private final OkHttpClient client = new OkHttpClient();
  private final ObjectMapper mapper = new ObjectMapper();

  private String ipRequestURL(IPAddress ipAddress) {
    return String.format("http://%s:%s/addr", ipAddress.getIp(), ipAddress.getPort());
  }

  private String blockRequestUrl(IPAddress ipAddress) {
    return String.format("http://%s:%s/getblocks", ipAddress.getIp(), ipAddress.getPort());
  }

  public void runStartup(Ledger ledger, IPService ipService) throws IOException {
    IPAddress local = IPAddress.builder().ip(InetAddress.getLocalHost().getHostAddress()).port("4567").build();
    ipService.updateIPAddressesFromFile(ledger);
    List<IPAddress> newIpAddresses = new ArrayList<>(ledger.getIpAddresses());
    for (IPAddress ipAddress : ledger.getIpAddresses()) {
      Request request = new Request.Builder().url(ipRequestURL(ipAddress)).build();
      Response response = client.newCall(request).execute();
      if (response.isSuccessful()) {
        newIpAddresses.addAll(mapper.readValue(Objects.requireNonNull(response.body()).byteStream(),
            mapper.getTypeFactory().constructCollectionType(List.class, IPAddress.class)));
      }
    }
    for (IPAddress address : newIpAddresses) {
      if (!address.equals(local)) {
        ledger.addIPAddress(address);
        Request postRequest = new Request.Builder().url(ipRequestURL(address))
            .post(new FormBody.Builder().build()).build();
        Response postResponse = client.newCall(postRequest).execute();
        if (postResponse.isSuccessful()) {
          LOGGER.info("Two way binding successful");
          Request blockRequest = new Request.Builder().url(blockRequestUrl(address)).build();
          Response blockResponse = client.newCall(blockRequest).execute();
          if (blockResponse.isSuccessful()) {
            List<Block> chainBlocks = new ArrayList<>();
            chainBlocks.addAll(mapper.readValue(Objects.requireNonNull(blockResponse.body()).byteStream(),
                mapper.getTypeFactory().constructCollectionType(List.class, Block.class)));
            for (Block block : chainBlocks) {
              if (!ledger.getBlocks().containsKey(block.getHash())) {
                ledger.addBlock(block);
              }
            }
          }
        }
      }

    }
  }
}
