package ee.taltech.ledger.api.services;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import ee.taltech.ledger.api.model.SignedTransaction;

import java.nio.charset.StandardCharsets;

public class HashingService {

  public static final String GENESIS_HASH = "blockchain-ledger";
  public static final String HASH_PREFIX = "0000";

  public static String generateSHA256Hash(SignedTransaction transaction) {
    Gson gson = new Gson();
    return Hashing.sha256().hashString(gson.toJson(transaction), StandardCharsets.UTF_8).toString();
  }

  public static String generateSHA256Hash(String string) {
    return Hashing.sha256().hashString(string, StandardCharsets.UTF_8).toString();
  }
}
