package ee.taltech.ledger.api.services;

import ee.taltech.ledger.api.model.IPAddress;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileReadWriteService {
  private static final Logger LOGGER = Logger.getLogger(FileReadWriteService.class.getName());

  private final String portIpFile;

  public FileReadWriteService(IPAddress ip) {
    String path = "./data/";
    String fileName = ip.getPort() + "-" + ip.getIp() + ".txt";
    this.portIpFile = path + fileName;
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

  public void writeIPs(List<IPAddress> ipList) {
    try (FileWriter writer = new FileWriter(portIpFile)) {
      for (IPAddress ip : ipList) {
        writer.write(ip.getIp() + ":" + ip.getPort() + "\n");
      }
    } catch (IOException e) {
      LOGGER.severe("Error in writeIPs: " + Arrays.toString(e.getStackTrace()));
    }
  }
}
