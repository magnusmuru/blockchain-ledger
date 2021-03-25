package ee.taltech.ledger.api.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
}
