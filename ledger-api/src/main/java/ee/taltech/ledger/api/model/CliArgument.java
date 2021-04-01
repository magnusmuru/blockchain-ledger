package ee.taltech.ledger.api.model;

import lombok.Data;

@Data
public class CliArgument {
  private String identifier;
  private boolean required;
  private String description;

  public CliArgument(String identifier, boolean required, String description) {
    this.identifier = identifier;
    this.required = required;
    this.description = description;
  }
}
