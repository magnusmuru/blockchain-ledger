package ee.taltech.ledger.api.dto;

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
