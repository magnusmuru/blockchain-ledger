package ee.taltech.ledger.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IPAddress {
  private String ip;
  private String port;

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof IPAddress) {
      return (((IPAddress) obj).getIp().equals(this.getIp()) && ((IPAddress) obj).getPort().equals(this.getPort()));
    } else {
      return super.equals(obj);
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(ip, port);
  }
}
