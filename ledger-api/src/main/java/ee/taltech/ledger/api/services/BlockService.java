package ee.taltech.ledger.api.services;

import com.google.gson.Gson;
import ee.taltech.ledger.api.model.*;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.ZonedDateTime;
import java.util.*;
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
        .filter(block -> hash == null || (ledger.getBlocks().containsKey(hash) && block.getNr() >= ledger.getBlocks().get(hash).getNr()))
        .sorted(Comparator.comparingInt(Block::getNr))
        .collect(Collectors.toList());
  }

  public void shareTransaction(Ledger ledger, SignedTransaction transaction) {
    if (transaction != null) {
      Gson gson = new Gson();
      for (IPAddress address : ledger.getIpAddresses()) {
        try {
          Response response = sendPostRequest(transactionSharingUrl(address), RequestBody.create(gson.toJson(transaction),
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
      PublicKey publicKey = kf.generatePublic(new X509EncodedKeySpec(Hex.decodeHex(transaction.getTransaction().getFrom())));
      sg.initVerify(publicKey);
      sg.update(gson.toJson(transaction.getTransaction()).getBytes(StandardCharsets.UTF_8));
      verified = sg.verify(Hex.decodeHex(transaction.getSignature()));
      LOGGER.log(Level.INFO, "Transfer verification completed with outcome verified == {0}", verified);
    } catch (NoSuchAlgorithmException | DecoderException | SignatureException | InvalidKeySpecException | InvalidKeyException e) {
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

  public boolean isTransactionInPreviousBlocks(Ledger ledger, SignedTransaction transaction) {
    return ledger.getBlocks().values().stream()
        .flatMap(block -> block.getTransactions().stream())
        .anyMatch(tr -> tr.equals(transaction));
  }

  public boolean areAmountsOkay(Ledger ledger, List<SignedTransaction> transactions) {
    // TODO summade kontroll
    HashMap<String, Double> currentBalances = new HashMap<>();
    transactions.stream()
        .sorted(Comparator.comparing(st -> ZonedDateTime.parse(st.getTransaction().getTimestamp())))
        .forEach(transaction -> {
          if (currentBalances.containsKey(transaction.getTransaction().getFrom())) {
            currentBalances.put(transaction.getTransaction().getFrom(),
                currentBalances.get(transaction.getTransaction().getFrom()) - transaction.getTransaction().getSum());
          } else {
            currentBalances.put(transaction.getTransaction().getFrom(),
                -1.0 * transaction.getTransaction().getSum());
          }

          if (currentBalances.containsKey(transaction.getTransaction().getTo())) {
            currentBalances.put(transaction.getTransaction().getTo(),
                currentBalances.get(transaction.getTransaction().getTo()) + transaction.getTransaction().getSum());
          } else {
            currentBalances.put(transaction.getTransaction().getTo(),
                transaction.getTransaction().getSum());
          }
        });
    HashMap<String, Double> blockchainBalances = new HashMap<>();
    List<SignedTransaction> all = ledger.getBlocks().values().stream()
        .flatMap(block -> block.getTransactions().stream())
        .collect(Collectors.toList());
    return true;
  }


  private Block createNewBlock(Ledger ledger) {
    List<SignedTransaction> transactions = ledger.getTransactions().stream()
        .filter(this::verifyTransaction) //
        .filter(transaction -> !isTransactionInPreviousBlocks(ledger, transaction))
        //.filter(this::areAmountsOkay) // verifyAmounts()
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
        .nr(ledger.getBlocks().get(ledger.getLastHash()) != null ? ledger.getBlocks().get(ledger.getLastHash()).getNr() + 1 : 1)
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
    LOGGER.log(Level.INFO, "Finished mining, found nonce: {0}, hash: {1}", new String[]{Integer.toString(nonce), hash});
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
    // TODO õige paralleelbloki valimine
    ledger.addBlock(block);
    return true;
  }
}
