package ee.taltech.ledger.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.taltech.ledger.api.dto.BlockDTO;
import ee.taltech.ledger.api.model.Block;
import ee.taltech.ledger.api.model.IPAddress;
import ee.taltech.ledger.api.model.Ledger;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BlockService extends BaseService {

  private final HashingService hashingService = new HashingService();


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

    return !blockChain.isEmpty()
        ? blockChain
        : null;
  }

  public Block blockChainTransaction(Ledger ledger, String hash) {
    return ledger.getBlocks().get(hash);
  }

  public void generateNewTransaction(Ledger ledger, BlockDTO blockDTO) throws IOException {
    Block transactionBlock = Block.builder()
        .transaction(blockDTO.getTransaction())
        .message(blockDTO.getMessage())
        .blockHeight(ledger.getBlocks().size()).build();

    transactionBlock.setHash(
        ledger.getLastHash() == null
            ? hashingService.genesisHash()
            : ledger.getLastHash()
    );

    this.shareBlock(ledger, transactionBlock);
  }

  public void shareBlock(Ledger ledger, Block block) throws IOException {
    if (!ledger.getBlocks().containsKey(block.getHash())) {
      ObjectMapper mapper = new ObjectMapper();
      ledger.setLastHash(hashingService.generateSHA256Hash(block));
      ledger.addBlock(block);
      for (IPAddress address : ledger.getIpAddresses()) {
        MediaType json = MediaType.parse("application/json; charset=utf-8");
        sendPostRequest(blockSharingUrl(address), RequestBody.create(mapper.writeValueAsString(block), json));
      }
    }
  }
}
