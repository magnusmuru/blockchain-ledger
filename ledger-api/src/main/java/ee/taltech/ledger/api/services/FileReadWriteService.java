package ee.taltech.ledger.api.services;

import ee.taltech.ledger.api.models.IPAddress;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileReadWriteService {
  private final String IPFile = "./data/ip.txt";

  public List<IPAddress> getIPs() throws IOException {
    Path path = Paths.get(IPFile);
    List<IPAddress> output = new ArrayList<>();

    if (path.toFile().isFile()) {
      List<String> ips = Files.readAllLines(path, StandardCharsets.UTF_8);
      for (String ipAddress : ips) {
        List<String> split = Arrays.asList(ipAddress.split(":"));
        output.add(IPAddress.builder().ip(split.get(0)).port(split.get(1)).build());
      }

    }
    return output;
  }

  public void writeIPs(List<IPAddress> ipList) {
    try {
      FileWriter writer = new FileWriter(IPFile);
      for (IPAddress ip : ipList) {
        writer.write(ip.getIp() + ":" + ip.getPort() + "\n");
      }
      writer.close();
    } catch (Exception e) {
      System.out.println(Arrays.toString(e.getStackTrace()));
    }
  }
}