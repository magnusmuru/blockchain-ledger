package ee.taltech.ledger.api.services;

import ee.taltech.ledger.api.model.CliArgument;
import org.apache.commons.cli.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


public class CommandLineService {
  private static final Logger LOGGER = Logger.getLogger(CommandLineService.class.getName());

  private final Options options;
  private final CommandLineParser parser;
  private final HelpFormatter formatter;
  private final Map<String, Object> parsedArguments;

  public CommandLineService() {
    this.options = new Options();
    this.parser = new DefaultParser();
    this.formatter = new HelpFormatter();
    this.parsedArguments = new HashMap<>();
  }

  public Map<String, Object> getParsedArgs(String[] args, List<CliArgument> cliArguments) {
    parseArgs(args, cliArguments);
    return parsedArguments;
  }

  private void parseArgs(String[] args, List<CliArgument> cliArguments) {
    cliArguments.forEach(this::parseArgument);

    CommandLine cmd = null;

    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      LOGGER.severe(e.getMessage());
      formatter.printHelp("utility-name", options);

      System.exit(1);
    }

    for (CliArgument cliArgument : cliArguments) {
      parsedArguments.put(cliArgument.getIdentifier(), cmd.getOptionValue(cliArgument.getIdentifier()));
    }
  }

  private void parseArgument(CliArgument cliArgument) {
    parseArgument(cliArgument.getIdentifier(), cliArgument.isRequired(), cliArgument.getDescription());
  }

  public void parseArgument(String identifier, boolean required, String description) {
    Option input = new Option(identifier, identifier, true, description);
    input.setRequired(required);
    options.addOption(input);
  }

}
