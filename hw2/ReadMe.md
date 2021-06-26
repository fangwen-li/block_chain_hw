# Block Chain
@author: Li Fangwen 1901212601

TIn this article, there will be 2 parts, the 1st part is about how this block chain works while the 2nd part is about the test cases and corresponding explanation.


## 1.Mechanism of Block Chain
The block chain in this assignment is organized in the form of a tree, every block is enclosed in a node and transations in each node is stored in a list instead of a Merkle tree. The data structure of  each block and its block node contains is set as follows:

```java
public class Block{ private Transaction coinbase;// coinbase transaction
  public static final int COINBASE = 25;//blook reward
  private byte[] prevBlockHash;//Hash value of the previous block
  private ArrayList<Transaction> txs;// list of transaction will be included in the block
  
  /**constructor*/
  private Block(byte[] prevBlockHash, PublicKey address){ 
      this.coinbase = new Transaction(COINBASE, address);
      this.prevBlockHash = prevBlockHash;
      this.txs = new ArrayList<Transactions>();
  }
}

public class BlockNode{
  private BlockNode parentNode; // parent node
  private UTXOPool utxoPool; // pool of utxo
  private Block block;// block of the node
  private ArrayList<BlockNode> childNodes; // the list of child node
  private int height; // height of the node/block
  
  /**constructor*/
  private BlockNode(Block block){
    this.block = block;
    this.utxoPool = new UTXOPool();
    this.parentNode = new BlockNode();
    this.childNodes = new ArrayList<BlockNode>();
    this.height = height;
  }
}
```

When block chain are constructed, they're connected with *BlockNode* object, and it must been ensured that ```parentNode.block.getHash()``` is equal to ```prevBlockHash```; 
and all Transaction in ```ArrayList<Transaction>``` are valid, this is confirmed by the validation process in Homework1.

Common rules here is different that of Bitcoin's block chain, including:
1. COINBASE can be used immediately in the next valid block;
2. The miner can not specify *Tx* to be included in a block, all valid transactions will be included in the most recent available block.

Especially, when forking, we set the following rules:
1. Manual forking allows users to make a new fork appended to any block, but the constraint is  height is not smaller than *height of current longest side branch - 10*;
2. Adding block will be attached to the longest side branch in default;
3. When a fork has same-length side branches, we use the oldest one(here realized by BFS);
4. Since only one global transaction pool is maintained, thus forking may produce irreversable changes;

## 2.Test cases and  explanation

The following result are printed in the test:
1. The number of child nodes of current node's parent node;
2. The height of the block;
3. The hash code of the block;
4. The #Txs in the global Tx pool;
5. The #UTXOs in the block's utxo Pool.

### How to init a new transaction for a block

In this homework ,we set 2 ways to let the block accept a transaction:

1. Wrap transaction to block directly, use ```Block.addTransaction()```

2. Add transaction to the global transaction pool(risky, may produce transaction used in a side branch, not reversible), use ```BlockChain.addTransaction()``` or ```BlockHandler.processTx()```.However, if the program is presented in an API, only operations in ```BlockHandler``` will be feasible for users.


### Avoid overflow
In the assignment, we use a special ```BlockChainClip``` class to store a little part of the block chain:
```java
public class BlockChainClip {
    //...
    public BlockChainClip(BlockChain.BlockNode blockNode){
        this.block = blockNode.getNode_block();
        this.height = blockNode.getNode_height();
        this.utxoPool = blockNode.getNode_UTXOPool();
        this.parentNode = blockNode.getNode_parentNode();
        this.childNodes = blockNode.getNode_childNodes();
        this.blockNode = blockNode;
    }

    public void printBlockChainClip(){...}

    private void printDFSSearch(BlockChain.BlockNode blockNode){...}
        
    //...
}
```


### Test case 1
**We want to test a valid block chain and store limited length of block chain**

the following feature will be verified in this test case:
1. The block chain can produce forks when these forks are legal;
2. Coinbase transaction can be consumed in the next coming block;
3. One can add valid block to valid positions of the block chain with either Automatic Forking or Manual Forking;
4. The longest side branch can be selected automatically;
5. Can only store a small part of the block chain to avoid overflow.

### Test case 2
**Test illegal coinbase**

1. try to consume an excessed coinbase number; since other invalid transactions has been tested in Homework1, they won't be tested again here; 
3. create a block trying to use current block's coinbase in current block.

### Test case 3
**Test illegal manual fork**

try to create a fork on the block whose height is smaller than *maxHeight - CUT_OFF_AGE*.

### Tets case 4
**Test illegal previous block hash**

1. insert a new genesis block when there exists a genesis block in the block chain;
2. incorrect previous hash when using automatic forking.

