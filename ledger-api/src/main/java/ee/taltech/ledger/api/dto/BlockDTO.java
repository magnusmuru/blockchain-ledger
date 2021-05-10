package ee.taltech.ledger.api.dto;

import com.google.gson.Gson;
import ee.taltech.ledger.api.model.Block;
import ee.taltech.ledger.api.model.SignedTransaction;
import lombok.*;

import java.util.Arrays;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BlockDTO {
  private int nr;
  private String previousHash;
  private String timestamp;
  private String nonce;
  private String hash;
  private String creator;
  private String merkleRoot;
  private int count;
  private String transactions;

  public Block toBlock() {
    Gson gson = new Gson();
    return Block.builder()
        .nr(nr)
        .previousHash(previousHash)
        .timestamp(timestamp)
        .nonce(Integer.parseInt(nonce))
        .hash(hash)
        .creator(creator)
        .merkleRoot(merkleRoot)
        .count(count)
        .transactions(Arrays.asList(gson.fromJson(transactions, SignedTransaction[].class)))
        .build();
  }
}
