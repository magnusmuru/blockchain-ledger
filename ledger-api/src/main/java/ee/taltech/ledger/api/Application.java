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
        blockService.generateNewTransaction(ledger, blockDTO);
        {
          response.status(200);
          return new Gson().toJsonTree(Status.builder()
              .statusType("Success")
              .statusMessage("Transaction added to ledger").build());
        }
      }));
    });

    path("/block", () -> {
      post("/:apikey", ((request, response) -> {
        Block block = new Gson().fromJson(request.body(), Block.class);
        blockService.shareBlock(ledger, block);
        return "";
      }));
    });

    bootService.runStartup(ledger, ipService);
    bootService = null; //after use this is not required so java garbage collection will delete it
  }
}
