package ee.taltech.ledger.api.services;

import ee.taltech.ledger.api.model.IPAddress;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

public class BaseService {
  private static final String API_KEY = "84c1226e-8718-4ba7-8088-d6d3b2640d9d";
  private final OkHttpClient client;

  public BaseService() {
    this.client = new OkHttpClient();
  }

  protected Response sendGetRequest(String url) throws IOException {
    Request request = new Request.Builder().url(url).build();
    return client.newCall(request).execute();
  }

  protected Response sendPostRequest(String url, RequestBody body) throws IOException {
    Request postRequest = new Request.Builder().url(url)
        .post(body).build();
    return client.newCall(postRequest).execute();
  }

  protected String ipRequestURL(IPAddress ipAddress) {
    return String.format("http://%s:%s/addr", ipAddress.getIp(), ipAddress.getPort());
  }

  protected String blockRequestUrl(IPAddress ipAddress) {
    return String.format("http://%s:%s/getblocks", ipAddress.getIp(), ipAddress.getPort());
  }

  protected String blockSharingUrl(IPAddress ipAddress) {
    return String.format("http://%s:%s/block/%s", ipAddress.getIp(), ipAddress.getPort(), API_KEY);
  }
}
