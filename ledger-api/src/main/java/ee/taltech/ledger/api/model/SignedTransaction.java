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
public class SignedTransaction {
  private String signature;
  private UnsignedTransaction transaction;

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SignedTransaction other = (SignedTransaction) obj;
    if (!signature.equals(other.getSignature()))
      return false;
    return transaction.equals(other.getTransaction());
  }

  @Override
  public int hashCode() {
    return Objects.hash(signature, transaction);
  }
}
