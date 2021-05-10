package ee.taltech.ledger.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import ee.taltech.ledger.api.dto.BlockDTO;
import ee.taltech.ledger.api.model.*;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BlockService extends BaseService {
  private static final Logger LOGGER = Logger.getLogger(BlockService.class.getName());

  private final HashingService hashingService = new HashingService();

  public List<Block> blockChainLedgerFromBlock(Ledger ledger, String hash) {
    List<Block> blockChain = new ArrayList<>();
    String rootHash = hash == null
        ? hashingService.genesisHash()
        : hash;

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
    Block transactionBlock = blockDTO.toBlock();

    transactionBlock.setHash(
        ledger.getLastHash() == null
            ? hashingService.genesisHash()
            : ledger.getLastHash()
    );

    this.shareBlock(ledger, transactionBlock);
  }

  public void shareBlock(Ledger ledger, Block block)  {
    if (block != null) {
      Gson gson = new Gson();
      for (IPAddress address : ledger.getIpAddresses()) {
        MediaType json = MediaType.parse("application/json; charset=utf-8");
        try {
          Response response = sendPostRequest(blockSharingUrl(address), RequestBody.create(gson.toJson(block), json));
          response.close();
        } catch (IOException e) {
          LOGGER.log(Level.WARNING, "Error in BlockService.shareBlock: {0}", e.getMessage());
        }
      }
    }
  }

  public SignedTransaction signTransaction(UnsignedTransaction transaction, PrivateKey pk) {
    try {
      Gson gson = new Gson();
      Signature sgn = Signature.getInstance("SHA256withRSA");
      sgn.initSign(pk);
      sgn.update(gson.toJson(transaction).getBytes(StandardCharsets.UTF_8));
      String signature = new String(sgn.sign());
      LOGGER.log(Level.INFO, "Signed transaction: {0}", signature);
      return new SignedTransaction(signature, transaction);
    } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
      LOGGER.log(Level.SEVERE, "Error signing transaction: {0}", e.getMessage());
    }
    return null;
  }

  public Block addTransaction(Ledger ledger, SignedTransaction transaction) {
    if (transaction == null) return null;
    ledger.addTransaction(transaction);
    if (ledger.getTransactions().size() >= Ledger.MAX_TRANSACTIONS_PER_BLOCK) {
      LOGGER.log(Level.INFO, "Transaction limit for a single block reached, creating a new block.");
      return createNewBlock(ledger);
    }
    return null;

  }

  private Block createNewBlock(Ledger ledger) {
    List<SignedTransaction> transactions = new ArrayList<>(ledger.getTransactions());
    // TODO bloki kontrollid
    if (ledger.getLastHash() == null) ledger.setLastHash(HashingService.GENESIS_HASH);
    Block newBlock = Block.builder()
        .nr(ledger.getBlocks().get(ledger.getLastHash()) != null ? ledger.getBlocks().get(ledger.getLastHash()).getNr() + 1 : 1)
        .previousHash(ledger.getLastHash())
        .timestamp(ZonedDateTime.now().toString())
        .creator(ledger.getKeyPair().getPublic().toString())
        .count(transactions.size())
        .transactions(transactions)
        .merkleRoot(findMerkleRoot(ledger.getTransactions()))
        .build();

    findNonceAndHash(newBlock);
    ledger.addBlock(newBlock);
    ledger.setLastHash(newBlock.getHash());
    LOGGER.log(Level.INFO, "Created a new block with hash {0}", newBlock.getHash());
    return newBlock;
  }

  private String findMerkleRoot(HashSet<SignedTransaction> transactions) {
    List<String> hashes = new ArrayList<>(transactions).stream()
        .map(HashingService::generateSHA256Hash)
        .collect(Collectors.toList());
    while (hashes.size() > 1) {
      List<String> iterationHashes = new ArrayList<>();
      for (int index = 0; index < hashes.size(); index += 2) {
        String first, second;
        first = hashes.get(index);
        second = index + 1 < hashes.size() ? hashes.get(index + 1) : hashes.get(index);
        iterationHashes.add(HashingService.generateSHA256Hash(first + second));
      }
      hashes = iterationHashes;
    }
    LOGGER.log(Level.INFO, "Calculated merkle root: {0}", hashes.get(0));
    return hashes.get(0);
  }

  private void findNonceAndHash(Block block) {
    Gson gson = new Gson();
    String blockString = gson.toJson(block, Block.class);
    int nonce = 0;
    String hash = HashingService.generateSHA256Hash(blockString + nonce);
    while (!hash.startsWith(HashingService.HASH_PREFIX)) {
      nonce++;
      hash = HashingService.generateSHA256Hash(blockString + nonce);
      LOGGER.log(Level.INFO, "Mining, current nonce: {0}, hash: {1}", new String[]{Integer.toString(nonce), hash});
    }
    LOGGER.log(Level.INFO, "Finished mining, found nonce: {0}, hash: {1}", new String[]{Integer.toString(nonce), hash});
    block.setNonce(nonce);
    block.setHash(hash);
  }
}
