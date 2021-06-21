package ee.taltech.ledger.api.model;

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

}
