package ee.taltech.ledger.api.services;

import ee.taltech.ledger.api.DTO.BlockDTO;
import ee.taltech.ledger.api.models.Block;
import ee.taltech.ledger.api.models.Ledger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BlockService {
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

    if (blockChain.size() > 0) {
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

  public Block generateNewTransaction(Ledger ledger, BlockDTO blockDTO) {
    Block transactionBlock = Block.builder()
        .transaction(blockDTO.getTransaction())
        .message(blockDTO.getMessage())
        .blockHeight(ledger.getBlocks().size()).build();

    if (ledger.getLastHash() == null) {
      transactionBlock.setHash(hashingService.genesisHash());
    } else {
      transactionBlock.setHash(ledger.getLastHash());
    }
    ledger.setLastHash(hashingService.generateSHA256Hash(transactionBlock));
    ledger.addBlock(transactionBlock);
    return transactionBlock;
  }

  public boolean insertNewBlock(Ledger ledger, Block block) {
    if (ledger.getLastHash().equals(block.getHash()) || hashingService.genesisHash().equals(block.getHash())) {
      ledger.addBlock(block);
      return true;
    }
    return false;
  }
}
