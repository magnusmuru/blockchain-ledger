package ee.taltech.ledger.api;

import com.google.gson.Gson;
import ee.taltech.ledger.api.DTO.BlockDTO;
import ee.taltech.ledger.api.models.Block;
import ee.taltech.ledger.api.models.IPAddress;
import ee.taltech.ledger.api.models.Ledger;
import ee.taltech.ledger.api.models.Status;
import ee.taltech.ledger.api.services.BlockService;
import ee.taltech.ledger.api.services.BootService;
import ee.taltech.ledger.api.services.IPService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static spark.Spark.*;

public class Application {
  public static void main(String[] args) throws IOException {
    Ledger ledger = new Ledger();
    IPService ipService = new IPService();
    BootService bootService = new BootService();
    BlockService blockService = new BlockService();

    /*
    BlockDTO testBlock = BlockDTO.builder()
        .message("I like trains")
        .transaction(50).build();
    BlockDTO testBlock2 = BlockDTO.builder()
        .message("moonboys")
        .transaction(67).build();
    BlockDTO testBlock3 = BlockDTO.builder()
        .message("Viva las vegas")
        .transaction(800).build();
    blockService.generateNewTransaction(ledger, testBlock);
    blockService.generateNewTransaction(ledger, testBlock2);
    blockService.generateNewTransaction(ledger, testBlock3);
    */

    path("/addr", () -> {
      get("", ((request, response) -> {
        response.type("application/json");
        List<IPAddress> ipAddressList = ledger.getIpAddresses();
        return new Gson().toJson(ipAddressList);
      }));
      post("", ((request, response) -> {
        List<String> split = Arrays.asList(request.ip().split(":"));
        IPAddress newAddress = IPAddress.builder().ip(split.get(0)).port(split.get(1)).build();
        if (!ledger.getIpAddresses().contains(newAddress)) {
          ipService.writeIPAddressesToFileAndLedger(ledger, newAddress);
          response.body("Two way binding achieved");
          response.status(200);
        } else {
          response.status(304);
        }
        return response;
      }));
    });

    path("/getblocks", () -> {
      get("", ((request, response) -> {
        response.type("application/json");
        return new Gson().toJsonTree(blockService.blockChainLedgerFromBlock(ledger, null));
      }));
      get("/:hash", ((request, response) -> {
        response.type("application/json");
        return new Gson().toJsonTree(blockService.blockChainLedgerFromBlock(ledger, request.params(":hash")));
      }));
    });

    path("/getdata", () -> {
      get("/:hash", ((request, response) -> {
        response.type("application/json");
        return new Gson().toJsonTree(blockService.blockChainTransaction(ledger, request.params(":hash")));
      }));
    });

    path("/transaction", () -> {
      post("", ((request, response) -> {
        response.type("application/json");
        BlockDTO blockDTO = new Gson().fromJson(request.body(), BlockDTO.class);
        Block block = blockService.generateNewTransaction(ledger, blockDTO);
        response.status(200);
        ipService.shareBlock(block, ledger);
        return new Gson().toJsonTree(Status.builder()
            .statusType("Success")
            .statusMessage("Transaction added to ledger").build());

      }));
    });

    path("/block", () -> {
      post("/:apikey", ((request, response) -> {
        if (ipService.compareAPIKey(request.params(":apikey"))) {
          Block block = new Gson().fromJson(request.body(), Block.class);
          if (blockService.insertNewBlock(ledger, block)) {
            response.status(200);
            return new Gson().toJsonTree(Status.builder()
                .statusType("Success")
                .statusMessage("Transaction added to ledger").build());
          } else {
            response.status(400);
            return new Gson().toJsonTree(Status.builder()
                .statusType("Error")
                .statusMessage("Transaction failed"));
          }
        } else {
          response.status(401);
          return new Gson().toJsonTree(Status.builder()
              .statusType("Error")
              .statusMessage("Bad API Key"));
        }
      }));
    });

    bootService.runStartup(ledger, ipService);
    bootService = null; //after use this is not required so java garbage collection will delete it
  }
}
