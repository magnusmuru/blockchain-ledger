package ee.taltech.ledger.api.model;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Block {
  private String hash;
  private int transaction;
  private String message;
  private int blockHeight;
}
