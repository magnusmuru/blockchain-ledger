package ee.taltech.ledger.api.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IpDTO {
  private String ip;
  private String port;
}
