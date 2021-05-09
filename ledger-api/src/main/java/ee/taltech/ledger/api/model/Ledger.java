package ee.taltech.ledger.api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Ledger {

  @Setter
  private HashSet<IPAddress> ipAddresses = new HashSet<>();

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
