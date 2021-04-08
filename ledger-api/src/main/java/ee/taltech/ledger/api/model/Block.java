package ee.taltech.ledger.api.model;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Block {
  private String hash;
  private double transaction;
  private String message;
  private int blockHeight;
}
