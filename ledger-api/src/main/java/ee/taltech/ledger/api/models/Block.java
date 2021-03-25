package ee.taltech.ledger.api.models;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Block {
  @Setter
  private String hash;
  private int transaction;
  private String message;
  private int blockHeight;
}
