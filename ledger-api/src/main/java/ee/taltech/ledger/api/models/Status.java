package ee.taltech.ledger.api.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Status {
  private String statusType;
  private String statusMessage;
}
