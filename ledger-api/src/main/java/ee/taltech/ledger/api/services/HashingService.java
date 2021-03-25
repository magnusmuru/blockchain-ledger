package ee.taltech.ledger.api.services;

import com.google.common.hash.Hashing;
import ee.taltech.ledger.api.models.Block;

import java.nio.charset.StandardCharsets;

public class HashingService {
  public String generateSHA256Hash(Block block) {
    String blockString = block.getHash() + block.getTransaction() + block.getMessage();
    return Hashing.sha256().hashString(blockString, StandardCharsets.UTF_8).toString();
  }

  public String genesisHash() {
    final String origin = "blockchain-ledger";
    return Hashing.sha256().hashString(origin, StandardCharsets.UTF_8).toString();
  }
}
