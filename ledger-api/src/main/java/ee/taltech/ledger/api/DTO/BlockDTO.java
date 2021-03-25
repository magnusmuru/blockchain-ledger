package ee.taltech.ledger.api.DTO;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BlockDTO {
  private int transaction;
  private String message;
}
