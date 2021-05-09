package ee.taltech.ledger.api.services;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import ee.taltech.ledger.api.model.Block;

import java.nio.charset.StandardCharsets;

public class HashingService {
  public String generateSHA256Hash(Block block) {
    Gson gson = new Gson();
    return Hashing.sha256().hashString(gson.toJson(block.toDto()), StandardCharsets.UTF_8).toString();
  }

  public String genesisHash() {
    final String origin = "blockchain-ledger";
    return Hashing.sha256().hashString(origin, StandardCharsets.UTF_8).toString();
  }
}
