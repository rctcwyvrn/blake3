package com.github.rctcwyvrn.blake3java;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Translation of the Blake3 reference implemenetation from Rust/C to Java
 * BLAKE3 Source: https://github.com/BLAKE3-team/BLAKE3
 * Translator: rctcwyvrn
 */
public class Blake3 {
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    private static final int OUT_LEN = 32;
    private static final int KEY_LEN = 32;
    private static final int BLOCK_LEN = 64;
    private static final int CHUNK_LEN = 1024;

    private static final long CHUNK_START = 1L;
    private static final long CHUNK_END = 2L;
    private static final long PARENT = 4L;
    private static final long ROOT = 8L;
    private static final long KEYED_HASH = 16L;
    private static final long DERIVE_KEY_CONTEXT = 32L;
    private static final long DERIVE_KEY_MATERIAL = 64L;

    private static final long[] IV = {
            0x6A09E667L, 0xBB67AE85L, 0x3C6EF372L, 0xA54FF53AL, 0x510E527FL, 0x9B05688CL, 0x1F83D9ABL, 0x5BE0CD19L
    };

    private static final int[] MSG_PERMUTATION = {
            2, 6, 3, 10, 7, 0, 4, 13, 1, 11, 12, 5, 9, 14, 15, 8
    };

    private static long wrappingAdd(long a, long b){
        return (a + b) % 0x100000000L;
    }

    private static long rotateRight(long x, int len){
        return ((x >> len) | (x << (32 - len))) & 0xffffffffL;
    }

    private static void g(long[] state, int a, int b, int c, int d, long mx, long my){
        state[a] = wrappingAdd(wrappingAdd(state[a], state[b]), mx);
        state[d] = rotateRight((state[d] ^ state[a]), 16);
        state[c] = wrappingAdd(state[c], state[d]);
        state[b] = rotateRight((state[b] ^ state[c]), 12);
        state[a] = wrappingAdd(wrappingAdd(state[a], state[b]), my);
        state[d] = rotateRight((state[d] ^ state[a]), 8);
        state[c] = wrappingAdd(state[c], state[d]);
        state[b] = rotateRight((state[b] ^ state[c]), 7);
    }

    private static void roundFn(long[] state, long[] m){
        // Mix columns
        g(state,0,4,8,12,m[0],m[1]);
        g(state,1,5,9,13,m[2],m[3]);
        g(state,2,6,10,14,m[4],m[5]);
        g(state,3,7,11,15,m[6],m[7]);

        // Mix diagonals
        g(state,0,5,10,15,m[8],m[9]);
        g(state,1,6,11,12,m[10],m[11]);
        g(state,2,7,8,13,m[12],m[13]);
        g(state,3,4,9,14,m[14],m[15]);
    }

    private static long[] permute(long[] m){
        long[] permuted = new long[16];
        for(int i = 0;i<16;i++){
            permuted[i] = m[MSG_PERMUTATION[i]];
        }
        return permuted;
    }

    private static long[] compress(long[] chainingValue, long[] blockWords, long counter, long blockLen, long flags){
        long counterInt =  counter & 0xffffffffL;
        long counterShift = (counter >> 32) & 0xffffffffL;
        long[] state = {
                chainingValue[0],
                chainingValue[1],
                chainingValue[2],
                chainingValue[3],
                chainingValue[4],
                chainingValue[5],
                chainingValue[6],
                chainingValue[7],
                IV[0],
                IV[1],
                IV[2],
                IV[3],
                counterInt,
                counterShift,
                blockLen,
                flags
        };
        roundFn(state, blockWords);         // Round 1
        blockWords = permute(blockWords);
        roundFn(state, blockWords);         // Round 2
        blockWords = permute(blockWords);
        roundFn(state, blockWords);         // Round 3
        blockWords = permute(blockWords);
        roundFn(state, blockWords);         // Round 4
        blockWords = permute(blockWords);
        roundFn(state, blockWords);         // Round 5
        blockWords = permute(blockWords);
        roundFn(state, blockWords);         // Round 6
        blockWords = permute(blockWords);
        roundFn(state, blockWords);         // Round 7

        for(int i = 0; i<8; i++){
            state[i] ^= state[i+8];
            state[i+8] ^= chainingValue[i];
        }
        return state;
    }

    private static long[] wordsFromLEBytes(short[] bytes){
        if ((bytes.length != 64)) throw new AssertionError();
        long[] words = new long[16];

        for(int i=0; i<64; i+=4) {
            words[i / 4] = (bytes[i + 3] << 24) +
                    (bytes[i + 2] << 16) +
                    (bytes[i + 1] << 8) +
                    (bytes[i]);
        }
        return words;
    }

    // Node of the Blake3 hash tree
    // Is either chained into the next node using chainingValue()
    // Or used to calculate the hash digest using rootOutputBytes()
    private static class Node {
        long[] inputChainingValue;
        long[] blockWords;
        long counter;
        long blockLen;
        long flags;

        private Node(long[] inputChainingValue, long[] blockWords, long counter, long blockLen, long flags) {
            this.inputChainingValue = inputChainingValue;
            this.blockWords = blockWords;
            this.counter = counter;
            this.blockLen = blockLen;
            this.flags = flags;
        }

        // Return the 8 int CV
        private long[] chainingValue(){
            long blockLenLong = blockLen;
            return Arrays.copyOfRange(
                    compress(inputChainingValue, blockWords, counter, blockLenLong, flags),
                    0,8);
        }

        private short[] rootOutputBytes(int outLen){
            int outputCounter = 0;
            int outputsNeeded = Math.floorDiv(outLen,(2*OUT_LEN)) + 1;
            short[] hash = new short[outLen];
            int i = 0;
            while(outputCounter < outputsNeeded){
                long[] words = compress(inputChainingValue, blockWords, outputCounter, blockLen & 0xffffffffL,flags | ROOT );

                for(long word: words){
                    for(byte b: ByteBuffer.allocate(4)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .putInt((int) (word & 0xffffffffL))
                            .array()){
                        hash[i] = (short) (b & 0xff);
                        i+=1;
                        if(i == outLen){
                            return hash;
                        }
                    }
                }
                outputCounter+=1;
            }
            System.out.println("Uhoh, pretty sure this is never supposed to get here");
            System.exit(1);
            return null;
        }
    }

    // Helper object for creating new Nodes and chaining them
    private class ChunkState {
        long[] chainingValue;
        int chunkCounter;
        short[] block = new short[BLOCK_LEN];
        byte blockLen = 0;
        byte blocksCompressed = 0;
        long flags;

        public ChunkState(long[] key, int chunkCounter, long flags){
            this.chainingValue = key;
            this.chunkCounter = chunkCounter;
            this.flags = flags;
        }

        public int len(){
            return BLOCK_LEN * blocksCompressed + blockLen;
        }

        private long startFlag(){
            return blocksCompressed == 0? CHUNK_START: 0;
        }

        private void update(short[] input) {
            while (input.length != 0) {

                // Chain the next 64 byte block into this chunk/node
                if (blockLen == BLOCK_LEN) {
                    long[] blockWords = wordsFromLEBytes(block);
                    this.chainingValue = Arrays.copyOfRange(
                            compress(this.chainingValue, blockWords, this.chunkCounter, BLOCK_LEN,this.flags | this.startFlag()),
                            0, 8);
                    blocksCompressed += 1;
                    this.block = new short[BLOCK_LEN];
                    this.blockLen = 0;
                }

                // Take bytes out of the input and update
                int want = BLOCK_LEN - this.blockLen; // How many bytes we need to fill up the current block
                int canTake = Math.min(want, input.length);
                for(int i = 0; i < canTake; i++){
                    block[blockLen + i] = input[i];
                }

                blockLen += canTake;
                input = Arrays.copyOfRange(input, canTake, input.length);
            }
        }

        private Node createNode(){
            return new Node(chainingValue, wordsFromLEBytes(block), chunkCounter, blockLen, flags | startFlag() | CHUNK_END);
        }
    }

    // Hasher
    private ChunkState chunkState;
    private long[] key;
    private long[][] cvStack = new long[54][];
    private byte cvStackLen = 0;
    private long flags;

    public Blake3(){
        this(IV,0);
    }

    public Blake3(long[] key, long flags){
        this.chunkState = new ChunkState(key, 0, flags);
        this.key = key;
        this.flags = flags;
    }

    public Blake3(short[] key){
        this(wordsFromLEBytes(key), KEYED_HASH);
    }

    public Blake3(String context){
        Blake3 contextHasher = new Blake3(IV, DERIVE_KEY_CONTEXT);
    }

    private void pushStack(long[] cv){
        this.cvStack[this.cvStackLen] = cv;
        cvStackLen+=1;
    }

    private long[] popStack(){
        this.cvStackLen-=1;
        return cvStack[cvStackLen];
    }

    // Combines the chaining values of two children to create the parent node
    private static Node parentNode(long[] leftChildCV, long[] rightChildCV, long[] key, long flags){
        long[] blockWords = new long[16];
        int i = 0;
        for(long x: leftChildCV){
            blockWords[i] = x;
            i+=1;
        }
        for(long x: rightChildCV){
            blockWords[i] = x;
            i+=1;
        }
        return new Node(key, blockWords, 0, BLOCK_LEN, PARENT | flags);
    }

    private static long[] parentCV(long[] leftChildCV, long[] rightChildCV, long[] key, long flags){
        return parentNode(leftChildCV, rightChildCV, key, flags).chainingValue();
    }

    private void addChunkChainingValue(long[] newCV, long totalChunks){
        while((totalChunks & 1) == 0){
            newCV = parentCV(popStack(), newCV, key, flags);
            totalChunks >>=1;
        }
        pushStack(newCV);
    }

    public void update(String input){
        byte[] inputBytes = input.getBytes();
        short[] converted = new short[inputBytes.length];
        for(int i = 0; i < inputBytes.length; i++){
            converted[i] = (short) (0xff & inputBytes[i]);
        }
        updateRaw(converted);
    }
    public void updateRaw(short[] input){
        while(input.length != 0) {

            // If this chunk has chained in 16 64 bytes of input, add it's CV to the stack
            if (chunkState.len() == CHUNK_LEN) {
                long[] chunkCV = chunkState.createNode().chainingValue();
                int totalChunks = chunkState.chunkCounter + 1;
                addChunkChainingValue(chunkCV, totalChunks);
                chunkState = new ChunkState(key, totalChunks, flags);
            }

            int want = CHUNK_LEN - chunkState.len();
            int take = Math.min(want, input.length);
            chunkState.update(Arrays.copyOfRange(input, 0, take));
            input = Arrays.copyOfRange(input, take, input.length);
        }
    }

    public short[] digest(int hashLen){
        Node node = this.chunkState.createNode();
        int parentNodesRemaining = cvStackLen;
        while(parentNodesRemaining > 0){
            parentNodesRemaining -=1;
            node = parentNode(
                    cvStack[parentNodesRemaining],
                    node.chainingValue(),
                    key,
                    flags
            );
        }
        return node.rootOutputBytes(hashLen);
    }

    public String hexdigest(int hashLen){
        return bytesToHex(digest(hashLen));
    }

    public String hexdigest(){
        return hexdigest(32);
    }

    private static String bytesToHex(short[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
