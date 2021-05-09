package ee.taltech.ledger.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {
  private String from;
  private String to;
  private BigDecimal sum;
  private Date timestamp;
}
