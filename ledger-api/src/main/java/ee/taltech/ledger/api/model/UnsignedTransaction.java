package ee.taltech.ledger.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UnsignedTransaction {
  private String from;
  private String to;
  private Double sum;
  private String timestamp;
}
