package ee.taltech.ledger.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UnsignedTransaction {
  private String from;
  private String to;
  private Double sum;
  private String timestamp;

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    UnsignedTransaction other = (UnsignedTransaction) obj;
    if (!from.equals(other.getFrom()))
      return false;
    if (!to.equals(other.getTo()))
      return false;
    if (!sum.equals(other.getSum()))
      return false;
    return timestamp.equals(other.getTimestamp());
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, to, sum, timestamp);
  }
}
