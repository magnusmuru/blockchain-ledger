package ee.taltech.ledger.api.model;

import ee.taltech.ledger.api.dto.IpDTO;
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

  public static IPAddress parseString(String input) {
    int colonIndex = input.lastIndexOf(":");
    return new IPAddress(
        input.substring(0, colonIndex),
        input.substring(colonIndex + 1));
  }

  public static IPAddress dtoToAddress(IpDTO masterIpDto) {
    return new IPAddress(
        masterIpDto.getIp(),
        masterIpDto.getPort()
    );
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof IPAddress) {
      return ((IPAddress) obj).getIp().equals(this.getIp()) && ((IPAddress) obj).getPort().equals(this.getPort());
    } else {
      return super.equals(obj);
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(ip, port);
  }

  public String toPlainString() {
    return ip + ":" + port;
  }
}
