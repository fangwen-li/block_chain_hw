import org.junit.Test;

import java.security.*;

import static org.junit.Assert.assertTrue;


public class TxHandlerTest {
    /**
     *In this test, we set five test to test
     * 1. valid transactions to show basic functions
     * 2.Test double-spent
     * 3.invalid output number(wrong number of coins to transfer)
     * 4.invalid signature
     * 5.Test the input not exist in the UtxoPool
     * In this part, I use the function "assertTrue", and if there is not error showed,
     * it means that the test is success and those function is correct.
     * And in each test, it will print that if each transaction is valid
     *
     */

    // test case 1: valid transactions to show basic functions
    //tx0: Scrooge --> Scrooge 25coins [Create Coins]
    //tx1: Scrooge --> Scrooge 4coins  [Divide Coins]
    //	 Scrooge --> Scrooge 5coins
    //	 Scrooge --> Scrooge 6coins
    //tx2: Scrooge --> Alice   4coins  [Pay separately]
    //	 Scrooge --> Alice   5coins
    //     Scrooge --> Bob     6coins
    //tx3: Alice --> Alice     2coins  [Divide Coins]
    //     Alice --> Alice     2coins
    //tx4: Alice --> Bob       2coins  [Pay jointly]
    //     Alice --> Bob       5coins
    @Test
    public void TxHandlerTestFirst() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        // generate key pairs, simulate initial tx from scrooge to alice
        System.out.println("-----------------------------------------------------------------");
        System.out.println("Test some basic functions of TxHandler");
        KeyPair pkScrooge = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyPair pkAlice = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyPair pkBob = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        // tx0: Scrooge --> Scrooge 25coins [Create Coins]
        Transaction tx0 = new Transaction();
        tx0.addOutput(25, pkScrooge.getPublic());

        byte[] initHash = null;
        tx0.addInput(initHash, 0);
        tx0.signTx( pkScrooge.getPrivate(), 0);

        UTXOPool utxoPool = new UTXOPool();
        UTXO utxo = new UTXO(tx0.getHash(), 0);
        utxoPool.addUTXO(utxo, tx0.getOutput(0));


        // tx1: Scrooge --> Scrooge 4coins  [Divide Coins]
        //	 Scrooge --> Scrooge 5coins
        //	 Scrooge --> Scrooge 6coins
        Transaction tx1 = new Transaction();
        tx1.addInput(tx0.getHash(),0);

        tx1.addOutput(4,pkScrooge.getPublic());
        tx1.addOutput(5,pkScrooge.getPublic());
        tx1.addOutput(6,pkScrooge.getPublic());

        tx1.signTx(pkScrooge.getPrivate(), 0);

        TxHandler txHandler = new TxHandler(utxoPool);
        System.out.println("txHandler.isValidTx(tx1) returns: " + txHandler.isValidTx(tx1));

        assertTrue("tx1:One valid transaction", txHandler.handleTxs(new Transaction[]{tx1}).length == 1);
        // update utxo pool
        utxoPool = txHandler.getHandledUtxoPool();
        assertTrue("tx1:Three UTXO's are created", utxoPool.getAllUTXO().size() == 3);


        // tx2: Scrooge --> Alice   4coins  [Pay separately]
        //	 Scrooge --> Alice   5coins
        //   Scrooge --> Bob     6coins
        Transaction tx2 = new Transaction();
        tx2.addInput(tx1.getHash(), 0);
        tx2.addInput(tx1.getHash(), 1);
        tx2.addInput(tx1.getHash(), 2);

        tx2.addOutput(4, pkAlice.getPublic());
        tx2.addOutput(5, pkAlice.getPublic());
        tx2.addOutput(6, pkBob.getPublic());

        tx2.signTx(pkScrooge.getPrivate(), 0);
        tx2.signTx(pkScrooge.getPrivate(), 1);
        tx2.signTx(pkScrooge.getPrivate(), 2);

        txHandler = new TxHandler(utxoPool);
        System.out.println("txHandler.isValidTx(tx2) returns: " + txHandler.isValidTx(tx2));
        assertTrue("tx2:One valid transaction", txHandler.handleTxs(new Transaction[]{tx2}).length == 1);
        // update utxo pool
        utxoPool = txHandler.getHandledUtxoPool();
        assertTrue("tx2:Three UTXO's are created", utxoPool.getAllUTXO().size() == 3);


        // tx3:Alice --> Alice     2coins  [Divide Coins]
        //     Alice --> Alice     2coins
        Transaction tx3 = new Transaction();
        tx3.addInput(tx2.getHash(),0);

        tx3.addOutput(2, pkAlice.getPublic());
        tx3.addOutput(2, pkAlice.getPublic());

        tx3.signTx(pkAlice.getPrivate(),0);

        txHandler = new TxHandler(utxoPool);

        System.out.println("txHandler.isValidTx(tx3) returns: " + txHandler.isValidTx(tx3));
        assertTrue("tx3:One valid transaction", txHandler.handleTxs(new Transaction[]{tx3}).length == 1);
        // update utxo pool
        utxoPool = txHandler.getHandledUtxoPool();
        assertTrue("tx3:Two UTXO's are created", utxoPool.getAllUTXO().size() == 4);

        // tx4: Alice --> Bob       2coins  [Pay jointly]
        //      Alice --> Bob       5coins
        Transaction tx4 = new Transaction();
        tx4.addInput(tx3.getHash(),0);
        tx4.addInput(tx2.getHash(),1);

        tx4.addOutput(7, pkBob.getPublic());

        tx4.signTx(pkAlice.getPrivate(), 0);
        tx4.signTx(pkAlice.getPrivate(),1);


        txHandler = new TxHandler(utxoPool);

        System.out.println("txHandler.isValidTx(tx4) returns: " + txHandler.isValidTx(tx4));
        assertTrue("tx4:One valid transaction", txHandler.handleTxs(new Transaction[]{tx4}).length == 1);
        // update utxo pool
        utxoPool = txHandler.getHandledUtxoPool();
        assertTrue("tx4:Two UTXO's are created", utxoPool.getAllUTXO().size() == 3);

//        System.out.println("tx1.hashCode returns: " + tx1.hashCode());
//        System.out.println("tx2.hashCode returns: " + tx2.hashCode());
//        System.out.println("tx3.hashCode returns: " + tx3.hashCode());
//        System.out.println("tx4.hashCode returns: " + tx4.hashCode());
    }


    // test case 2: Test double-spent
    //tx0: Scrooge --> Scrooge 25coins [Create Coins]
    //tx1: Scrooge --> Alice   20coins [Pay Coins]
    //tx2: Scrooge --> Bob     20coins [*Double-spending*]
    @Test
    public void TxHandlerTestSecond() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        // generate key pairs, simulate initial tx from scrooge to alice
        System.out.println("-----------------------------------------------------------------");
        System.out.println("Test if the TxHandler can reject the double spending");
        KeyPair pkScrooge = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyPair pkAlice = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyPair pkBob = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        Transaction tx0 = new Transaction();
        tx0.addOutput(25, pkScrooge.getPublic());

        byte[] initHash = null;
        tx0.addInput(initHash, 0);
        tx0.signTx(pkScrooge.getPrivate(), 0);

        UTXOPool utxoPool = new UTXOPool();
        UTXO utxo = new UTXO(tx0.getHash(), 0);
        utxoPool.addUTXO(utxo, tx0.getOutput(0));

        // tx 1
        Transaction tx1 = new Transaction();
        tx1.addInput(tx0.getHash(), 0);
        tx1.addOutput(25, pkAlice.getPublic());
        tx1.signTx(pkScrooge.getPrivate(), 0);

        TxHandler txHandler = new TxHandler(utxoPool);

        System.out.println("txHandler.isValidTx(tx1) returns: " + txHandler.isValidTx(tx1));
        assertTrue("tx1:One valid transaction", txHandler.handleTxs(new Transaction[]{tx1}).length == 1);
        // update utxo pool
        utxoPool = txHandler.getHandledUtxoPool();
        assertTrue("tx1: one UTXO's created.", utxoPool.getAllUTXO().size() == 1);

        // tx 2:Scrooge --> Bob     20coins [*Double-spending*]
        Transaction tx2 = new Transaction();
        tx2.addInput(tx0.getHash(), 0);
        tx2.addOutput(25, pkBob.getPublic());
        tx2.signTx(pkScrooge.getPrivate(), 0);

        txHandler = new TxHandler(utxoPool);
        System.out.println("txHandler.isValidTx(tx2) returns: " + txHandler.isValidTx(tx2));
        assertTrue("tx2:no valid transaction", txHandler.handleTxs(new Transaction[]{tx2}).length == 0);
        // update utxo pool
        utxoPool = txHandler.getHandledUtxoPool();
        assertTrue("tx2:no UTXO's created.", utxoPool.getAllUTXO().size() == 1);

//        System.out.println("tx1.hashCode returns: " + tx1.hashCode());
//        System.out.println("tx2.hashCode returns: " + tx2.hashCode());

    }

    // test case 3: invalid output number(wrong number of coins to transfer)
    // tx0: Scrooge --> Scrooge 25coins [Create Coins]
    //tx1: Scrooge --> Alice   20coins [Pay Coins]
    //tx2: Alice --> Bob       -5coins [*Invalid output number*]
    //tx3: Alice --> Bob       90coins [*Invalid output number*]
    @Test
    public void TxHandlerTestThird() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        // generate key pairs, simulate initial tx from scrooge to alice
        System.out.println("-----------------------------------------------------------------");
        System.out.println("Test if the TxHandler can reject invalid output number(wrong number of coins to transfer)");
        KeyPair pkScrooge = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyPair pkAlice = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyPair pkBob = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        Transaction tx0 = new Transaction();
        tx0.addOutput(25, pkScrooge.getPublic());

        byte[] initHash = null;
        tx0.addInput(initHash, 0);
        tx0.signTx(pkScrooge.getPrivate(), 0);

        UTXOPool utxoPool = new UTXOPool();
        UTXO utxo = new UTXO(tx0.getHash(), 0);
        utxoPool.addUTXO(utxo, tx0.getOutput(0));

        // tx 1 scrooge to alice 20coins
        Transaction tx1 = new Transaction();
        tx1.addInput(tx0.getHash(), 0);
        tx1.addOutput(20, pkAlice.getPublic());
        tx1.signTx(pkScrooge.getPrivate(), 0);


        TxHandler txHandler = new TxHandler(utxoPool);

        System.out.println("txHandler.isValidTx(tx1) returns: " + txHandler.isValidTx(tx1));
        assertTrue("tx1:add one valid transaction", txHandler.handleTxs(new Transaction[]{tx1}).length == 1);
        // update utxo pool
        utxoPool = txHandler.getHandledUtxoPool();
        assertTrue("tx1:one UTXO's created.", utxoPool.getAllUTXO().size() == 1);

        // tx2: Alice --> Bob       -5coins [*Invalid output number*]
        Transaction tx2 = new Transaction();
        tx2.addInput(tx1.getHash(), 0);
        tx2.addOutput(-5, pkBob.getPublic());
        tx2.signTx(pkAlice.getPrivate(), 0);

        txHandler = new TxHandler(utxoPool);

        System.out.println("txHandler.isValidTx(tx2) returns: " + txHandler.isValidTx(tx2));
        assertTrue("tx2:add one invalid transaction", txHandler.handleTxs(new Transaction[]{tx2}).length == 0);
        // update utxo pool
        utxoPool = txHandler.getHandledUtxoPool();
        assertTrue("tx2:one UTXO's remained", utxoPool.getAllUTXO().size() == 1);

        // tx3: Alice --> Bob       90coins [*Invalid output number*]
        Transaction tx3 = new Transaction();
        tx3.addInput(tx1.getHash(), 0);
        tx3.addOutput(90, pkBob.getPublic());
        tx3.signTx(pkAlice.getPrivate(), 0);

        txHandler = new TxHandler(utxoPool);

        System.out.println("txHandler.isValidTx(tx3) returns: " + txHandler.isValidTx(tx3));
        assertTrue("tx3:add one invalid transaction", txHandler.handleTxs(new Transaction[]{tx3}).length == 0);
        // update utxo pool
        utxoPool = txHandler.getHandledUtxoPool();
        assertTrue("tx3:one UTXO's remained", utxoPool.getAllUTXO().size() == 1);

//        System.out.println("tx1.hashCode returns: " + tx1.hashCode());
//        System.out.println("tx2.hashCode returns: " + tx2.hashCode());
//        System.out.println("tx3.hashCode returns: " + tx3.hashCode());
    }


    // test case 4: invalid signature
    // signature is not valid
    //tx0: Scrooge --> Scrooge 25coins [Create Coins]
    //tx1: Scrooge --> Alice   20coins [Pay Coins]
    //tx2: Alice --> Bob       20coins [*Signed by Bob*]
    @Test
    public void TxHandlerTestForth() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        // generate key pairs, simulate initial tx from scrooge to alice
        System.out.println("-----------------------------------------------------------------");
        System.out.println("Test if the TxHandler can reject invalid signature");
        KeyPair pkScrooge = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyPair pkAlice = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyPair pkBob = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        Transaction tx0 = new Transaction();
        tx0.addOutput(25, pkScrooge.getPublic());

        byte[] initHash = null;
        tx0.addInput(initHash, 0);
        tx0.signTx(pkScrooge.getPrivate(), 0);

        UTXOPool utxoPool = new UTXOPool();
        UTXO utxo = new UTXO(tx0.getHash(), 0);
        utxoPool.addUTXO(utxo, tx0.getOutput(0));

        // tx1: scrooge to alice 20 coins
        Transaction tx1 = new Transaction();
        tx1.addInput(tx0.getHash(), 0);
        tx1.addOutput(20, pkAlice.getPublic());
        tx1.signTx(pkScrooge.getPrivate(), 0);

        TxHandler txHandler = new TxHandler(utxoPool);

        System.out.println("txHandler.isValidTx(tx1) returns: " + txHandler.isValidTx(tx1));
        assertTrue("tx1:add one valid transaction", txHandler.handleTxs(new Transaction[]{tx1}).length == 1);
        // update utxo pool
        utxoPool = txHandler.getHandledUtxoPool();
        assertTrue("tx1:one UTXO's created.", utxoPool.getAllUTXO().size() == 1);

        // tx2: alice to bob 20coins[signed by bob]
        Transaction tx2 = new Transaction();
        tx2.addInput(tx1.getHash(), 0);
        tx2.addOutput(20, pkBob.getPublic());
        tx2.signTx(pkBob.getPrivate(), 0);

        txHandler = new TxHandler(utxoPool);

        System.out.println("txHandler.isValidTx(tx2) returns: " + txHandler.isValidTx(tx2));
        assertTrue("tx2:add one invalid transaction", txHandler.handleTxs(new Transaction[]{tx2}).length == 0);
        // update utxo pool
        utxoPool = txHandler.getHandledUtxoPool();
        assertTrue("tx2:one UTXO's remained.", utxoPool.getAllUTXO().size() == 1);

//        System.out.println("tx1.hashCode returns: " + tx1.hashCode());
//        System.out.println("tx2.hashCode returns: " + tx2.hashCode());
    }

    //test case 5: Test the input not exist in the UtxoPool
    //tx0: Scrooge --> Scrooge 25coins [Create Coins]
    //tx1: Scrooge --> Alice   20coins [Pay Coins *NOT added to UTXO pool* due to accidents]
    //tx2: Alice --> Bob       15coins [Pay Coins *Previous Tx NOT in UTXO pool*]
    @Test
    public void TxHandlerTestFifth() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        // generate key pairs, simulate initial tx from scrooge to alice
        System.out.println("-----------------------------------------------------------------");
        System.out.println("Test if the TxHandler can reject the input not in the UtxoPool");
        KeyPair pkScrooge = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyPair pkAlice = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyPair pkBob = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        Transaction tx0 = new Transaction();
        tx0.addOutput(25, pkScrooge.getPublic());

        byte[] initHash = null;
        tx0.addInput(initHash, 0);
        tx0.signTx(pkScrooge.getPrivate(), 0);

        UTXOPool utxoPool = new UTXOPool();
        UTXO utxo = new UTXO(tx0.getHash(), 0);
        utxoPool.addUTXO(utxo, tx0.getOutput(0));

        //tx1: Scrooge --> Alice   20coins [Pay Coins *NOT added to UTXO pool* due to accidents]
        Transaction tx1 = new Transaction();
        tx1.addInput(tx0.getHash(), 0);
        tx1.addOutput(20, pkAlice.getPublic());
        tx1.signTx(pkScrooge.getPrivate(), 0);

        utxo = new UTXO(tx1.getHash(),0);
        TxHandler txHandler = new TxHandler(utxoPool);

        System.out.println("txHandler.isValidTx(tx1) returns: " + txHandler.isValidTx(tx1));
        assertTrue("tx1:no valid transaction", txHandler.handleTxs(new Transaction[]{tx1}).length == 1);
        // update utxo pool
        utxoPool = txHandler.getHandledUtxoPool();
        utxoPool.removeUTXO(utxo);
        assertTrue("tx1:UTXO has been removed.", utxoPool.getAllUTXO().size() == 0);


        // tx2: Alice --> Bob       15coins [Pay Coins *Previous Tx NOT in UTXO pool*]
        Transaction tx2 = new Transaction();
        tx2.addInput(tx1.getHash(), 0);
        tx2.addOutput(15, pkBob.getPublic());
        tx2.signTx(pkAlice.getPrivate(), 0);

        System.out.println("txHandler.isValidTx(tx2) returns: " + txHandler.isValidTx(tx2));
        assertTrue("tx2:no valid transaction", txHandler.handleTxs(new Transaction[]{tx2}).length == 0);
        // update utxo pool
        utxoPool = txHandler.getHandledUtxoPool();
        assertTrue("tx2:no UTXO's created.", utxoPool.getAllUTXO().size() == 0);

    }
}
