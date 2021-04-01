package ee.taltech.ledger.api.constant;

import ee.taltech.ledger.api.model.CliArgument;

import java.util.ArrayList;
import java.util.List;

public final class PropertyConstants {
  public static final String PORT = "port";

  private PropertyConstants() {
    throw new InstantiationError();
  }

  public static List<CliArgument> getCliArguments() {
    List<CliArgument> cliArguments = new ArrayList<>();
    cliArguments.add(new CliArgument(PropertyConstants.PORT, false, "port to bind"));
    return cliArguments;
  }
}
