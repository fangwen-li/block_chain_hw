import java.util.*;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */

    private UTXOPool utxoPool;

    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * This method just return the utxoPool of the TxHandler, it is used for the test.
     * @return the UtxoPool
     */
    public UTXOPool getHandledUtxoPool(){
        return this.utxoPool;
    }

    /**
     * The function of this method is to verify if the Transaction is valid or not
     * Input is the Transaction tx, and it will
     * @param : a transaction
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        int i = 0; //index
        double sumInput = 0; //sum of all the input value
        double sumOutput = 0; // sum of all the output value

        //This set is used to ensure one UTXO can only be used once in a transaction.
        Set<UTXO> spentUtxo = new HashSet<UTXO>(); //spent transaction outputs, avoid repeats

        //loop all the input to check if they are correct
        for (Transaction.Input input:tx.getInputs()){
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            //(1) all outputs claimed by {@code tx} are in the current UTXO pool,
            if (!utxoPool.contains(utxo)){
                return false;
            }

            //(2) the signatures on each input of {@code tx} are valid,
            if (!Crypto.verifySignature(utxoPool.getTxOutput(utxo).address, tx.getRawDataToSign(i), input.signature)){
                return false;
            }

            //(3) no UTXO is claimed multiple times by {@code tx},
            if (spentUtxo.contains(utxo)){
                return false;
            }
            // to now, if this input is valid, then we regard it will be spent and need to be removed
            spentUtxo.add(utxo);
            // add its value to the sumInput
            sumInput = sumInput + utxoPool.getTxOutput(utxo).value;
            i++;
        }

        // (4) all of {@code tx}s output values are non-negative, and
        for (Transaction.Output output : tx.getOutputs()){
            if (output.value < 0){
                return false;
            }else {
                sumOutput = sumOutput + output.value;
            }
        }
        // (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output value
        if(sumOutput>sumInput){
            return false;
        }
        return true;

    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     * @param: a list of possibleTxs
     * @return: a list of validTxs
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        Set<Transaction> acceptedTx = new HashSet<>();//store the valid transaction in the possibleTxs
        Set<Transaction> invalidTx = new HashSet<>();//store the invalid transactions in the possibleTxs
        Set<Transaction> acceptedTxThisLoop = new HashSet<>(); //store the valid transaction in one loop

        //loop for all possible Txs
        for (Transaction tx:possibleTxs){
            if (isValidTx(tx)){
                handleValidTx(acceptedTx,acceptedTxThisLoop,tx);
            }else{
                invalidTx.add(tx);
            }
        }

        //because some transactions are dependent with other
        // so we will go on research the tx in the invalidTx until no one will be added into the validTx
        // which means the acceptedTxThisLoop is Empty
        while (!acceptedTxThisLoop.isEmpty()){
            acceptedTxThisLoop.clear();
            for (Transaction invalidOne:invalidTx){
                if (isValidTx(invalidOne)){
                    handleValidTx(acceptedTx,acceptedTxThisLoop,invalidOne);
                }
            }
            invalidTx.removeAll(acceptedTxThisLoop);
        }
        Transaction[] Arr_acceptedTx = acceptedTx.toArray(new Transaction[acceptedTx.size()]);
        return Arr_acceptedTx;
    }


    /**
     * When a Transaction is valid, the coins of the input need to be removed from the UtxoPool
     * and the output will be added into the UtxoPool
     * @param acceptedTx: the set of valid transaction
     * @param acceptedTxThisLoop: the set of valid transaction in this loop
     * @param tx:a tranction
     */
    public void handleValidTx(Set<Transaction> acceptedTx,  Set<Transaction> acceptedTxThisLoop,Transaction tx){
        //loop all the input in a
        for (Transaction.Input oneDeal : tx.getInputs()){
            // check inputs in transaction, remove them from UTXOPool
            UTXO possibleDeal = new UTXO(oneDeal.prevTxHash,oneDeal.outputIndex);
            if (utxoPool.contains(possibleDeal)){
                utxoPool.removeUTXO(possibleDeal);
            }
        }
        //add the output into the UTXOPool
        List<Transaction.Output> outputs = tx.getOutputs();
        for (int i =0;i<outputs.size();i++){
            UTXO utxo = new UTXO(tx.getHash(),i);
            utxoPool.addUTXO(utxo,outputs.get(i));
        }
        acceptedTxThisLoop.add(tx);
        acceptedTx.add(tx);
    }

}
