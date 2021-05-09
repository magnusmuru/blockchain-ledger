package ee.taltech.ledger.api.model;

import com.google.gson.Gson;
import ee.taltech.ledger.api.dto.BlockDTO;
import lombok.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Block {
  private int nr;
  private String previousHash;
  private Date timestamp;
  private String nonce;
  private String hash;
  private String creator;
  private String merkleRoot;
  private int count;
  private List<Transaction> transactions;

  public BlockDTO toDto() {
    Gson gson = new Gson();
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    return BlockDTO.builder()
        .nr(nr)
        .previousHash(previousHash)
        .timestamp(df.format(timestamp))
        .nonce(nonce)
        .hash(hash)
        .creator(creator)
        .merkleRoot(merkleRoot)
        .count(count)
        .transactions(gson.toJson(transactions))
        .build();
  }
}
