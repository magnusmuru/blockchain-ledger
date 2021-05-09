package ee.taltech.ledger.api.dto;

import com.google.gson.Gson;
import ee.taltech.ledger.api.model.Block;
import ee.taltech.ledger.api.model.Transaction;
import lombok.*;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;

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
        .timestamp(Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(timestamp))))
        .nonce(nonce)
        .hash(hash)
        .creator(creator)
        .merkleRoot(merkleRoot)
        .count(count)
        .transactions(Arrays.asList(gson.fromJson(transactions, Transaction[].class)))
        .build();
  }
}
