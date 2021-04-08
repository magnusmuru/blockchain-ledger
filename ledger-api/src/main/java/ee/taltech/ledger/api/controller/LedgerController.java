package ee.taltech.ledger.api.controller;

import com.google.gson.Gson;
import ee.taltech.ledger.api.constant.ResponseTypeConstants;
import ee.taltech.ledger.api.dto.BlockDTO;
import ee.taltech.ledger.api.dto.IpDTO;
import ee.taltech.ledger.api.model.Block;
import ee.taltech.ledger.api.model.IPAddress;
import ee.taltech.ledger.api.model.Ledger;
import ee.taltech.ledger.api.model.Status;
import ee.taltech.ledger.api.services.BlockService;
import ee.taltech.ledger.api.services.BootService;
import ee.taltech.ledger.api.services.IPService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static spark.Spark.*;

public class LedgerController {
  private static final Logger LOGGER = Logger.getLogger(LedgerController.class.getName());

  private final Ledger ledger;
  private final String localPort;
  private final IPService ipService;
  private final BlockService blockService;
  private BootService bootService;

  public LedgerController(IpDTO master) throws UnknownHostException {
    this.ledger = new Ledger();
    this.localPort = master.getPort();
    IPAddress localIp = IPAddress.builder().ip(InetAddress.getLocalHost().getHostAddress()).port(localPort).build();
    this.ipService = new IPService(localIp);
    this.blockService = new BlockService();
    this.bootService = new BootService(master);
  }

  public void initialize() {
    port(Integer.parseInt(this.localPort));
    LOGGER.log(Level.INFO, "LedgerController.initialize - initializing node on port {0}", localPort);
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
        LOGGER.log(Level.INFO, "GET /addr - Request IP - {0}:{1}", new String[]{request.ip(), String.valueOf(request.port())});
        return new Gson().toJson(ipAddressList);
      }));
      post("", ((request, response) -> {
        response.type(ResponseTypeConstants.JSON);
        try {
          IpDTO ipDTO = new Gson().fromJson(request.body(), IpDTO.class);
          String ip = ipDTO.getIp();
          String port = ipDTO.getPort();
          LOGGER.log(Level.INFO, "POST /addr - Request IP - {0}:{1}", new String[]{ip, port});
          IPAddress newAddress = IPAddress.builder().ip(ip).port(port).build();
          if (!ledger.getIpAddresses().contains(newAddress)) {
            ipService.writeIPAddressesToFileAndLedger(ledger, newAddress);
            LOGGER.log(Level.INFO, "POST /addr - Added new IP {0}", newAddress.toPlainString());
          }
          return new Gson().toJsonTree(Status.builder()
              .statusType("Success")
              .statusMessage("Two way binding successful").build());
        } catch (Exception e) {
          return new Gson().toJsonTree(Status.builder()
              .statusType("Failed")
              .statusMessage("Invalid IP or Port").build());
        }
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
          try {
            BlockDTO blockDTO = new Gson().fromJson(request.body(), BlockDTO.class);
            blockService.generateNewTransaction(ledger, blockDTO);
            response.status(200);
            return new Gson().toJsonTree(Status.builder()
                .statusType("Success")
                .statusMessage("Transaction added to ledger").build());
          } catch (Exception e) {
            response.status(400);
            return new Gson().toJsonTree(Status.builder()
                .statusType("Fail")
                .statusMessage("Transaction addition failed").build());
          }
        })));
  }

  private void mapBlockRoutes() {
    path("/block", () ->
        post("/:apikey", ((request, response) -> {
          response.type(ResponseTypeConstants.JSON);
          try {
            Block block = new Gson().fromJson(request.body(), Block.class);
            blockService.shareBlock(ledger, block);
            return new Gson().toJsonTree(Status.builder()
                .statusType("Success")
                .statusMessage("Block added successfully").build());
          } catch (Exception e) {
            response.status(200);
            return new Gson().toJsonTree(Status.builder()
                .statusType("Fail")
                .statusMessage("Block addition failed").build());
          }
        })));
  }

  private void startupBootService() {
    try {
      bootService.runStartup(ledger, ipService, localPort);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Error in startupBootService: {0}", e.getMessage());
    }
    bootService = null; //after use this is not required so java garbage collection will delete it
  }
}
