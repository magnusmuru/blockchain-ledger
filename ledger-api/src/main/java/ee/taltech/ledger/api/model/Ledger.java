package ee.taltech.ledger.api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Ledger {

  @Setter
  private List<IPAddress> ipAddresses = new ArrayList<>();

  private final Map<String, Block> blocks = new HashMap<>();

  @Setter
  private String lastHash;

  public void addIPAddress(IPAddress address) {
    ipAddresses.add(address);
  }

  public void addBlock(Block block) {
    blocks.put(block.getHash(), block);
  }
}
