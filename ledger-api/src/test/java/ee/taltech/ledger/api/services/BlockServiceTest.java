package ee.taltech.ledger.api.services;

import ee.taltech.ledger.api.model.Block;
import ee.taltech.ledger.api.model.IPAddress;
import ee.taltech.ledger.api.model.Ledger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class BlockServiceTest {

  BlockService blockService;
  HashingService hashingService;
  IPAddress ipAddress;
  Block block;
  Ledger ledger;

  @Before
  public void setUp() {
    blockService = new BlockService();
    hashingService = new HashingService();
    ipAddress = new IPAddress("1.1.1.1", "9999");
    block = new Block();
    ledger = new Ledger();
  }

  @Test
  public void testBlockChainLedgerFromBlockExistingHash() {
    ledger.addIPAddress(ipAddress);
    ledger.addBlock(block);
    Block blockChainBlock = blockService.getBlockByHash(ledger, block.getHash());
    Assert.assertNotNull(blockChainBlock);
    Block[] blockChain = new Block[]{blockChainBlock};
    Assert.assertArrayEquals(blockChain, new Block[]{block});
  }

  @Test
  public void testBlockChainLedgerFromBlockNullHash() {
    ledger.addIPAddress(ipAddress);
    ledger.addBlock(block);
    Assert.assertNull(blockService.getBlockByHash(ledger, null));
  }

  @Test
  public void testBlockChainLedgerFromBlockNoBlocks() {
    ledger.addIPAddress(ipAddress);
    blockService.getBlockByHash(ledger, block.getHash());
    Assert.assertNull(blockService.getBlockByHash(ledger, block.getHash()));
  }
}
