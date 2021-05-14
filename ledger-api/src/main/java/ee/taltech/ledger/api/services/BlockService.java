package ee.taltech.ledger.api.services;

import com.google.gson.Gson;
import ee.taltech.ledger.api.model.*;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BlockService extends BaseService {
  private static final Logger LOGGER = Logger.getLogger(BlockService.class.getName());

  private static final Double COINBASE = 1.0;

  public Block getBlockByHash(Ledger ledger, String hash) {
    return ledger.getBlocks().get(hash);
  }

  public List<Block> getBlocksAfterHash(Ledger ledger, String hash) {
    return ledger.getBlocks().values().stream()
        .filter(block -> hash == null
            || (ledger.getBlocks().containsKey(hash)
            && block.getNr() >= ledger.getBlocks().get(hash).getNr()))
        .sorted(Comparator.comparingInt(Block::getNr))
        .collect(Collectors.toList());
  }

  public void shareTransaction(Ledger ledger, SignedTransaction transaction) {
    if (transaction != null) {
      Gson gson = new Gson();
      for (IPAddress address : ledger.getIpAddresses()) {
        try {
          Response response = sendPostRequest(transactionSharingUrl(address),
              RequestBody.create(gson.toJson(transaction),
                  MediaType.parse("application/json; charset=utf-8")));
          response.close();
        } catch (IOException e) {
          LOGGER.log(Level.WARNING, "Error in BlockService.shareTransaction: {0}", e.getMessage());
        }
      }
    }
  }

  public void shareBlock(Ledger ledger, Block block) {
    if (block != null) {
      Gson gson = new Gson();
      for (IPAddress address : ledger.getIpAddresses()) {
        try {
          Response response = sendPostRequest(blockSharingUrl(address), RequestBody.create(gson.toJson(block),
              MediaType.parse("application/json; charset=utf-8")));
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
      byte[] signature = sgn.sign();
      LOGGER.log(Level.INFO, "Signed transaction: {0}", signature);
      return new SignedTransaction(Hex.encodeHexString(signature), transaction);
    } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
      LOGGER.log(Level.SEVERE, "Error signing transaction: {0}", e.getMessage());
    }
    return null;
  }

  public boolean verifyTransaction(SignedTransaction transaction) {
    boolean verified = false;
    try {
      Gson gson = new Gson();
      Signature sg = Signature.getInstance("SHA256withRSA");
      KeyFactory kf = KeyFactory.getInstance("RSA");
      PublicKey publicKey = kf.generatePublic(new X509EncodedKeySpec(
          Hex.decodeHex(transaction.getTransaction().getFrom())));
      sg.initVerify(publicKey);
      sg.update(gson.toJson(transaction.getTransaction()).getBytes(StandardCharsets.UTF_8));
      verified = sg.verify(Hex.decodeHex(transaction.getSignature()));
      //LOGGER.log(Level.INFO, "Transfer verification completed with outcome verified == {0}", verified);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "An error occured during transaction verification: {0}", e.getMessage());
    }
    return verified;
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

  public boolean isTransactionNotInPreviousBlocks(Ledger ledger, SignedTransaction transaction) {
    return ledger.getBlocks().values().stream()
        .flatMap(block -> block.getTransactions().stream())
        .noneMatch(tr -> tr.equals(transaction));
  }

  public boolean areAmountsOkay(Ledger ledger, SignedTransaction transaction) {
    AtomicReference<Double> balance = new AtomicReference<>(0.0);
    ledger.getBlocks().values().stream()
        .flatMap(block -> block.getTransactions().stream())
        .filter(tx -> tx.getTransaction().getFrom().equals(transaction.getTransaction().getFrom())
            || tx.getTransaction().getTo().equals(transaction.getTransaction().getFrom()))
        .forEach(tx -> {
          if (tx.getTransaction().getFrom().equals(transaction.getTransaction().getFrom())) {
            balance.updateAndGet(v -> v - transaction.getTransaction().getSum());
          }
          if (tx.getTransaction().getTo().equals(transaction.getTransaction().getFrom())) {
            balance.updateAndGet(v -> v + transaction.getTransaction().getSum());
          }
        });
    return balance.get() >= transaction.getTransaction().getSum();
  }


  private Block createNewBlock(Ledger ledger) {
    List<SignedTransaction> transactions = ledger.getTransactions().stream()
        .filter(this::verifyTransaction)
        .filter(transaction -> areAmountsOkay(ledger, transaction))
        .filter(transaction -> isTransactionNotInPreviousBlocks(ledger, transaction))
        .collect(Collectors.toList());
    transactions.add(SignedTransaction.builder()
        .signature("")
        .transaction(UnsignedTransaction.builder()
            .from("0")
            .to(new String(Hex.encodeHex(ledger.getKeyPair().getPublic().getEncoded())))
            .sum(COINBASE)
            .timestamp(ZonedDateTime.now().toString())
            .build())
        .build());
    if (ledger.getLastHash() == null) ledger.setLastHash(HashingService.GENESIS_HASH);
    Block newBlock = Block.builder()
        .nr(ledger.getBlocks().get(ledger.getLastHash()) != null
            ? ledger.getBlocks().get(ledger.getLastHash()).getNr() + 1
            : 1)
        .previousHash(ledger.getLastHash())
        .timestamp(ZonedDateTime.now().toString())
        .creator(new String(Hex.encodeHex(ledger.getKeyPair().getPublic().getEncoded())))
        .count(transactions.size())
        .transactions(transactions)
        .merkleRoot(findMerkleRoot(ledger.getTransactions()))
        .build();

    findNonceAndHash(newBlock);
    ledger.addBlock(newBlock);
    ledger.setLastHash(newBlock.getHash());
    ledger.clearTransactions();
    LOGGER.log(Level.INFO, "Created a new block with hash {0}", newBlock.getHash());
    return newBlock;
  }

  private static String findMerkleRoot(HashSet<SignedTransaction> transactions) {
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

  private static void findNonceAndHash(Block block) {
    Gson gson = new Gson();
    String blockString = gson.toJson(block, Block.class);
    int nonce = 0;
    String hash = HashingService.generateSHA256Hash(blockString + nonce);
    LOGGER.log(Level.INFO, "Mining...");
    while (!hash.startsWith(HashingService.HASH_PREFIX)) {
      nonce++;
      hash = HashingService.generateSHA256Hash(blockString + nonce);
      //LOGGER.log(Level.INFO, "Mining, current nonce: {0}, hash: {1}", new String[]{Integer.toString(nonce), hash});
    }
    LOGGER.log(Level.INFO,
        "Finished mining, found nonce: {0}, hash: {1}",
        new String[]{Integer.toString(nonce), hash});
    block.setNonce(nonce);
    block.setHash(hash);
  }

  public static void createGenesisBlock(Ledger ledger) {
    SignedTransaction reward = SignedTransaction.builder()
        .signature("")
        .transaction(UnsignedTransaction.builder()
            .from(String.valueOf(0))
            .to(new String(Hex.encodeHex(ledger.getKeyPair().getPublic().getEncoded())))
            .sum(COINBASE * 10.0)
            .timestamp(ZonedDateTime.now().toString())
            .build())
        .build();
    Block genesisBlock = Block.builder()
        .nr(0)
        .previousHash(String.valueOf(0))
        .timestamp(ZonedDateTime.now().toString())
        .creator(new String(Hex.encodeHex(ledger.getKeyPair().getPublic().getEncoded())))
        .count(1)
        .transactions(List.of(reward))
        .merkleRoot(findMerkleRoot(new HashSet<>(List.of(reward))))
        .build();
    findNonceAndHash(genesisBlock);
    ledger.addBlock(genesisBlock);
    ledger.setLastHash(genesisBlock.getHash());
  }

  public boolean addBlock(Ledger ledger, Block block) {
    Map<String, Block> ledgerBlocks = ledger.getBlocks();
    if (ledgerBlocks.containsKey(block.getHash())) {
      return false;
    }
    Block blockByLastHash = ledgerBlocks.get(ledger.getLastHash());
    if (blockByLastHash.getNr() > block.getNr()) {
      return false;
    } else if ((blockByLastHash.getNr() < block.getNr())) {
      ledger.setLastHash(block.getHash());
      ledger.addBlock(block);
      return true;
    } else {
      // vali kus rohkem transaktsioone VÕI
      if (blockByLastHash.getTransactions().size() > block.getTransactions().size()) {
        return false;
      } else if (blockByLastHash.getTransactions().size() < block.getTransactions().size()) {
        removeLastHashBlockAndAddNew(ledger, block);
        return true;
      } else {
        return extracted(ledger, block, ledgerBlocks);
      }
    }
  }

  private boolean extracted(Ledger ledger, Block block, Map<String, Block> ledgerBlocks) {
    Block blockByLastHash = ledgerBlocks.get(ledger.getLastHash());
    ZonedDateTime lastHashTimestamp = ZonedDateTime.parse(blockByLastHash.getTimestamp());
    ZonedDateTime blockTimestamp = ZonedDateTime.parse(block.getTimestamp());
    // vali kus uuem timestamp (või eelistada vanema timestampiga) VÕI
    if (lastHashTimestamp.isBefore(blockTimestamp)) {
      return false;
    } else if (lastHashTimestamp.isAfter(blockTimestamp)) {
      removeLastHashBlockAndAddNew(ledger, block);
      return true;
    } else {
      // vali hashide stringivõrdlus, vali väiksem
      if (blockByLastHash.getHash().compareTo(block.getHash()) < 0) {
        return false;
      } else {
        removeLastHashBlockAndAddNew(ledger, block);
        return true;
      }
    }
  }

  private void removeLastHashBlockAndAddNew(Ledger ledger, Block block) {
    ledger.getBlocks().remove(ledger.getLastHash());
    ledger.setLastHash(block.getHash());
    ledger.addBlock(block);
  }
}
