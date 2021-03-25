package ee.taltech.ledger.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.taltech.ledger.api.models.IPAddress;
import ee.taltech.ledger.api.models.Ledger;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BootService {

  private final OkHttpClient client = new OkHttpClient();
  private final ObjectMapper mapper = new ObjectMapper();

  private String ipRequestURL(IPAddress ipAddress) {
    return String.format("http://%s:%s/addr", ipAddress.getIp(), ipAddress.getPort());
  }

  public void runStartup(Ledger ledger, IPService ipService) throws IOException {
    ipService.updateIPAddressesFromFile(ledger);
    List<IPAddress> newIpAddresses = new ArrayList<>();
    for (IPAddress ipAddress : ledger.getIpAddresses()) {
      Request request = new Request.Builder().url(ipRequestURL(ipAddress)).build();
      Response response = client.newCall(request).execute();
      if (response.isSuccessful()) {
        newIpAddresses.addAll(mapper.readValue(response.body().byteStream(), mapper.getTypeFactory().constructCollectionType(List.class, IPAddress.class)));
      }
    }
    for (IPAddress address : newIpAddresses) {
      if (!ledger.getIpAddresses().contains(address)) {
        ledger.addIPAddress(address);
        Request postRequest = new Request.Builder().url(ipRequestURL(address)).post(new FormBody.Builder().build()).build();
        Response postResponse = client.newCall(postRequest).execute();
        if (postResponse.isSuccessful()) {
          System.out.println("Two way binding successful");
        }
      }
    }
  }
}
