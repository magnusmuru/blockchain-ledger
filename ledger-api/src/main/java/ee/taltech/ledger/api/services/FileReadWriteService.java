package ee.taltech.ledger.api.services;

import com.google.gson.Gson;
import ee.taltech.ledger.api.model.IPAddress;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileReadWriteService {
  private static final Logger LOGGER = Logger.getLogger(FileReadWriteService.class.getName());

  private static final String FALLBACK_IP_FILE = "./data/fallback-ips.json";
  private final String portIpFile;

  public FileReadWriteService(IPAddress ip) {
    String path = "./data/";
    this.portIpFile = path + ip.getPort() + "-" + ip.getIp() + ".txt";;
    try {
      Files.createDirectories(Paths.get(path));
      File output = new File(portIpFile);
      if (output.createNewFile()) {
        LOGGER.log(Level.INFO, "Successfully created a new IP file ");
      }
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "FileReadWriteService - Error creating new ip file with path {0}", portIpFile);
    }
  }

  public List<IPAddress> getIPs() throws IOException {
    Path path = Paths.get(portIpFile);
    List<IPAddress> output = new ArrayList<>();

    if (path.toFile().isFile()) {
      List<String> ips = Files.readAllLines(path, StandardCharsets.UTF_8);
      if (!ips.isEmpty() && !ips.get(0).equals("")) {
        for (String ipAddress : ips) {
          LOGGER.log(Level.INFO, "FileReadWriteService.getIPs - adding IP to output: {0}", ipAddress);
          IPAddress address = IPAddress.parseString(ipAddress);
          output.add(IPAddress.builder().ip(address.getIp()).port(address.getPort()).build());
        }
      }
    }
    return output;
  }

  public void writeIPs(HashSet<IPAddress> ipList) {
    try (FileWriter writer = new FileWriter(portIpFile)) {
      for (IPAddress ip : ipList) {
        writer.write(ip.getIp() + ":" + ip.getPort() + "\n");
      }
    } catch (IOException e) {
      LOGGER.severe("Error in writeIPs: " + Arrays.toString(e.getStackTrace()));
    }
  }

  public static List<IPAddress> getFallbackIPs() {
    Gson gson = new Gson();
    try (Reader reader = new FileReader(FALLBACK_IP_FILE)) {
      IPAddress[] addresses = gson.fromJson(reader, IPAddress[].class);
      return List.of(addresses);
    } catch (IOException e) {
      LOGGER.severe("Could not load fallback IPs: " + Arrays.toString(e.getStackTrace()));
    }
    return null;
  }
}
