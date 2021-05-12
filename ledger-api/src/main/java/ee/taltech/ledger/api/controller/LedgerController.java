package ee.taltech.ledger.api.controller;

import com.google.gson.Gson;
import ee.taltech.ledger.api.constant.ResponseTypeConstants;
import ee.taltech.ledger.api.dto.BlockDTO;
import ee.taltech.ledger.api.model.*;
import ee.taltech.ledger.api.services.BlockService;
import ee.taltech.ledger.api.services.BootService;
import ee.taltech.ledger.api.services.IPService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.*;
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

  public LedgerController(IPAddress ipAddress) throws UnknownHostException, NoSuchAlgorithmException {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    kpg.initialize(2048);
    KeyFactory kf = KeyFactory.getInstance("RSA");
    this.ledger = new Ledger();
    this.ledger.setKeyPair(kpg.generateKeyPair());
//    byte[] array = this.ledger.getKeyPair().getPublic().getEncoded();
//    String hexed = new String(Hex.encodeHex(array));
//    byte[] unhexed = Hex.decodeHex(hexed);
//    PublicKey decodedKey = kf.generatePublic(new X509EncodedKeySpec(array));
    this.localPort = ipAddress.getPort();
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
          IPAddress ipAddress = new Gson().fromJson(request.body(), IPAddress.class);
          String ip = ipAddress.getIp();
          String port = ipAddress.getPort();
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
        return new Gson().toJsonTree(blockService.getBlocksAfterHash(ledger, null));
      });
      get("/:hash", (request, response) -> {
        response.type(ResponseTypeConstants.JSON);
        return new Gson().toJsonTree(blockService.getBlocksAfterHash(ledger, request.params(":hash")));
      });
    });
  }

  private void mapGetDataRoutes() {
    path("/getdata", () ->
        get("/:hash", ((request, response) -> {
          response.type(ResponseTypeConstants.JSON);
          return new Gson().toJsonTree(blockService.getBlockByHash(ledger, request.params(":hash")));
        })));
  }

  // remove/rework
  private void mapTransactions() {
    path("/addtransaction", () ->
        post("", (((request, response) -> {
          response.type(ResponseTypeConstants.JSON);
          try {
            UnsignedTransaction transaction = new Gson().fromJson(request.body(), UnsignedTransaction.class);
            SignedTransaction signedTransaction = blockService.signTransaction(transaction, ledger.getKeyPair().getPrivate());
            System.out.println(new Gson().toJson(signedTransaction));;
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
            // TODO verify signature, if valid add else drop
            SignedTransaction transaction = new Gson().fromJson(request.body(), SignedTransaction.class);
            boolean verified = blockService.verifyTransaction(transaction);
            if (verified) ledger.addTransaction(transaction);
            BlockDTO blockDTO = new Gson().fromJson(request.body(), BlockDTO.class); // TODO already in another block etc
            blockService.generateNewTransaction(ledger, blockDTO);
            response.status(200);
            return new Gson().toJsonTree(Status.builder()
                .statusType(verified ? "Success" : "Fail")
                .statusMessage(verified ? "Transaction added to ledger" : "Transaction signature could not be verified")
                .build());
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
