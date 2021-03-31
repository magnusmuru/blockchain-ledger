package ee.taltech.ledger.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.taltech.ledger.api.dto.BlockDTO;
import ee.taltech.ledger.api.model.Block;
import ee.taltech.ledger.api.model.IPAddress;
import ee.taltech.ledger.api.model.Ledger;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BlockService {
  private static final String API_KEY = "84c1226e-8718-4ba7-8088-d6d3b2640d9d";

  private final HashingService hashingService = new HashingService();
  private final OkHttpClient client = new OkHttpClient();


  public List<Block> blockChainLedgerFromBlock(Ledger ledger, String hash) {
    List<Block> blockChain = new ArrayList<>();
    String rootHash;

    if (hash == null) {
      rootHash = hashingService.genesisHash();
    } else {
      rootHash = hash;
    }

    Map<String, Block> ledgerBlocks = ledger.getBlocks();
    while (ledgerBlocks.containsKey(rootHash)) {
      Block block = ledgerBlocks.get(rootHash);
      blockChain.add(block);
      rootHash = hashingService.generateSHA256Hash(block);
    }

    if (!blockChain.isEmpty()) {
      return blockChain;
    } else {
      return null;
    }
  }

  public Block blockChainTransaction(Ledger ledger, String hash) {
    if (ledger.getBlocks().containsKey(hash)) {
      return ledger.getBlocks().get(hash);
    }
    return null;
  }

  public void generateNewTransaction(Ledger ledger, BlockDTO blockDTO) throws IOException {
    Block transactionBlock = Block.builder()
        .transaction(blockDTO.getTransaction())
        .message(blockDTO.getMessage())
        .blockHeight(ledger.getBlocks().size()).build();

    if (ledger.getLastHash() == null) {
      transactionBlock.setHash(hashingService.genesisHash());
    } else {
      transactionBlock.setHash(ledger.getLastHash());
    }
    this.shareBlock(ledger, transactionBlock);
  }

  public void shareBlock(Ledger ledger, Block block) throws IOException {
    if (!ledger.getBlocks().containsKey(block.getHash())) {
      ObjectMapper mapper = new ObjectMapper();
      ledger.setLastHash(hashingService.generateSHA256Hash(block));
      ledger.addBlock(block);
      for (IPAddress address : ledger.getIpAddresses()) {
        MediaType json = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(json, mapper.writeValueAsString(block));
        Request postRequest = new Request.Builder()
            .url(blockSharingUrl(address))
            .post(requestBody)
            .build();

        client.newCall(postRequest).execute();
      }
    }
  }

  private String blockSharingUrl(IPAddress ipAddress) {
    return String.format("http://%s:%s/block/%s", ipAddress.getIp(), ipAddress.getPort(), API_KEY);
  }
}
