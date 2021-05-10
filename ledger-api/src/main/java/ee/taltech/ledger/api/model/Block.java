package ee.taltech.ledger.api.model;

import com.google.gson.Gson;
import ee.taltech.ledger.api.dto.BlockDTO;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Block {
  private int nr;
  private String previousHash;
  private String timestamp;
  private String creator;
  private String merkleRoot;
  private int count;
  private List<SignedTransaction> transactions;

  @Setter
  private int nonce;
  private String hash;

  public BlockDTO toDto() {
    Gson gson = new Gson();
    return BlockDTO.builder()
        .nr(nr)
        .previousHash(previousHash)
        .timestamp(timestamp)
        .nonce(Integer.toString(nonce))
        .hash(hash)
        .creator(creator)
        .merkleRoot(merkleRoot)
        .count(count)
        .transactions(gson.toJson(transactions))
        .build();
  }
}
