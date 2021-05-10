package ee.taltech.ledger.api.controller;

import com.google.gson.Gson;
import ee.taltech.ledger.api.constant.ResponseTypeConstants;
import ee.taltech.ledger.api.dto.BlockDTO;
import ee.taltech.ledger.api.dto.IpDTO;
import ee.taltech.ledger.api.model.*;
import ee.taltech.ledger.api.services.BlockService;
import ee.taltech.ledger.api.services.BootService;
import ee.taltech.ledger.api.services.IPService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
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

  public LedgerController(IpDTO master) throws UnknownHostException, NoSuchAlgorithmException {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    kpg.initialize(2048);
    this.ledger = new Ledger();
    this.ledger.setKeyPair(kpg.generateKeyPair());
    this.localPort = master.getPort();
    IPAddress localIp = IPAddress.builder().ip(InetAddress.getLocalHost().getHostAddress()).port(localPort).build();
    this.ipService = new IPService(localIp);
    this.blockService = new BlockService();
    this.bootService = new BootService(); //master);
  }

  public void initialize() {
    port(Integer.parseInt(this.localPort));
    LOGGER.log(Level.INFO, "LedgerController.initialize - initializing node on port {}", localPort);
    mapAddrRoutes();
    mapGetBlocksRoutes();
    mapGetDataRoutes();
    mapTransactionRoutes();
    mapBlockRoutes();
    mapTransactions();
    startupBootService();
  }

  private void mapAddrRoutes() {
    path("/addr", () -> {
      get("", ((request, response) -> {
        response.type(ResponseTypeConstants.JSON);
        HashSet<IPAddress> ipAddressList = ledger.getIpAddresses();
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

  private void mapTransactions() {
    path("/addtransaction", () ->
        post("", (((request, response) -> {
          response.type(ResponseTypeConstants.JSON);
          try {
            UnsignedTransaction transaction = new Gson().fromJson(request.body(), UnsignedTransaction.class);
            SignedTransaction signedTransaction = blockService.signTransaction(transaction, ledger.getKeyPair().getPrivate());
            Block block = blockService.addTransaction(ledger, signedTransaction);
            if (block != null) blockService.shareBlock(ledger, block); // TODO fix block sharing
            response.status(200);// TODO mine block AFTER sending 200
            return new Gson().toJsonTree(Status.builder()
                .statusType("Success")
                .statusMessage("Transaction added to ledger").build());
          } catch (Exception e) {
            response.status(400);
            return new Gson().toJsonTree(Status.builder()
                .statusType("Fail")
                .statusMessage("Transaction addition failed").build());
          }
        })))
    );
  }

  private void mapTransactionRoutes() {
    path("/transaction", () ->
        post("", ((request, response) -> {
          response.type(ResponseTypeConstants.JSON);
          try {
            // check if signed or unsigned transaction
            //


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
        }))
    );
  }

  private void mapBlockRoutes() {
    path("/block", () ->
        post("/:apikey", ((request, response) -> {
          response.type(ResponseTypeConstants.JSON);
          try {
            Block block = new Gson().fromJson(request.body(), Block.class);
            if (!ledger.getBlocks().containsKey(block.getHash())) {
              ledger.addBlock(block);
              blockService.shareBlock(ledger, block);
            }
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
