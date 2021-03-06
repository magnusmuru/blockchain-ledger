package ee.taltech.ledger.api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Ledger {
  public static final int MAX_TRANSACTIONS_PER_BLOCK = 7;

  @Setter
  private HashSet<IPAddress> ipAddresses = new HashSet<>();

  @Setter
  private KeyPair keyPair;

  @Setter
  private HashSet<SignedTransaction> transactions = new HashSet<>();

  @Setter
  private String lastHash;

  private final Map<String, Block> blocks = new HashMap<>();

  public void addIPAddress(IPAddress address) {
    ipAddresses.add(address);
  }

  public void addBlock(Block block) {
    blocks.put(block.getHash(), block);
  }

  public void addTransaction(SignedTransaction transaction) {
    transactions.add(transaction);
  }

  public void clearTransactions() {
    transactions.clear();
  }
}
