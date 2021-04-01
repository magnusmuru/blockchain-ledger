package ee.taltech.ledger.api.controller;

import com.google.gson.Gson;
import ee.taltech.ledger.api.constant.PropertyConstants;
import ee.taltech.ledger.api.constant.ResponseTypeConstants;
import ee.taltech.ledger.api.dto.BlockDTO;
import ee.taltech.ledger.api.model.*;
import ee.taltech.ledger.api.services.BlockService;
import ee.taltech.ledger.api.services.BootService;
import ee.taltech.ledger.api.services.CommandLineService;
import ee.taltech.ledger.api.services.IPService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static spark.Spark.*;

public class LedgerController {
  private static final Logger LOGGER = Logger.getLogger(LedgerController.class.getName());

  private final Ledger ledger;
  private final IPService ipService;
  private final BlockService blockService;
  private final CommandLineService commandLineService;
  private BootService bootService;

  public LedgerController() {
    this.ledger = new Ledger();
    this.ipService = new IPService();
    this.blockService = new BlockService();
    this.commandLineService = new CommandLineService();
    this.bootService = new BootService();
  }

  public void initialize(String[] args) {
    List<CliArgument> cliArguments = PropertyConstants.getCliArguments();
    Map<String, Object> parameters = commandLineService.getParsedArgs(args, cliArguments);

    mapAddrRoutes();
    mapGetBlocksRoutes();
    mapGetDataRoutes();
    mapTransactionRoutes();
    mapBlockRoutes();
    startupBootService();
  }

  private void mapAddrRoutes() {
    path("/addr", () -> {
      get("", ((request, response) -> {
        response.type(ResponseTypeConstants.JSON);
        List<IPAddress> ipAddressList = ledger.getIpAddresses();
        return new Gson().toJson(ipAddressList);
      }));
      post("", ((request, response) -> {
        String ip = String.valueOf(request.ip());
        String port = String.valueOf(request.port());
        IPAddress newAddress = IPAddress.builder().ip(ip).port(port).build();
        if (!ledger.getIpAddresses().contains(newAddress)) {
          ipService.writeIPAddressesToFileAndLedger(ledger, newAddress);
        }
        response.body("Two way binding achieved");
        response.status(200);
        return response;
      }));
    });
  }

  private void mapGetBlocksRoutes() {
    path("/getblocks", () -> {
      get("", (request, response) -> {
        response.type(ResponseTypeConstants.JSON);
        return new Gson().toJsonTree(blockService.blockChainLedgerFromBlock(ledger, null));
      });
      get("/:hash", (request, response) -> {
        response.type(ResponseTypeConstants.JSON);
        return new Gson().toJsonTree(blockService.blockChainLedgerFromBlock(ledger, request.params(":hash")));
      });
    });
  }

  private void mapGetDataRoutes() {
    path("/getdata", () ->
        get("/:hash", ((request, response) -> {
          response.type(ResponseTypeConstants.JSON);
          return new Gson().toJsonTree(blockService.blockChainTransaction(ledger, request.params(":hash")));
        })));
  }

  private void mapTransactionRoutes() {
    path("/transaction", () ->
        post("", ((request, response) -> {
          response.type(ResponseTypeConstants.JSON);
          BlockDTO blockDTO = new Gson().fromJson(request.body(), BlockDTO.class);
          blockService.generateNewTransaction(ledger, blockDTO);
          response.status(200);
          return new Gson().toJsonTree(Status.builder()
              .statusType("Success")
              .statusMessage("Transaction added to ledger").build());
        })));
  }

  private void mapBlockRoutes() {
    path("/block", () ->
        post("/:apikey", ((request, response) -> {
          Block block = new Gson().fromJson(request.body(), Block.class);
          blockService.shareBlock(ledger, block);
          return "";
        })));
  }

  private void startupBootService() {
    try {
      bootService.runStartup(ledger, ipService);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Error in startupBootService: {0}", e.getMessage());
    }
    bootService = null; //after use this is not required so java garbage collection will delete it
  }
}
