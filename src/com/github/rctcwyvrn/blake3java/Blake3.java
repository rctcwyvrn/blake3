package com.github.rctcwyvrn.blake3java;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

/**
 * Translation of the Blake3 reference implemenetation from Rust/C to Java
 * Author: rctcwyvrn
 */
public class Blake3 {
    private static final int OUT_LEN = 32;
    private static final int KEY_LEN = 32;
    private static final int BLOCK_LEN = 64;
    private static final int CHUNK_LEN = 1024;

    private static final int CHUNK_START = 1;
    private static final int CHUNK_END = 2;
    private static final int PARENT = 4;
    private static final int ROOT = 8;
    private static final int KEYED_HASH = 16;
    private static final int DERIVE_KEY_CONTEXT = 32;
    private static final int DERIVE_KEY_MATERIAL = 64;

    private static final int[] IV = {
            0x6A09E667, 0xBB67AE85, 0x3C6EF372, 0xA54FF53A, 0x510E527F, 0x9B05688C, 0x1F83D9AB, 0x5BE0CD19
    };

    private static final int[] MSG_PERMUTATION = {
            2, 6, 3, 10, 7, 0, 4, 13, 1, 11, 12, 5, 9, 14, 15, 8
    };

    private static int wrappingAdd(int a, int b){
        return (a + b); //Should be mod something, ill figure it out
    }

    private static int rotateRight(int x, int len){
        return (x >> len) | (x << (32 - len));
    }

    private static void g(int[] state, int a, int b, int c, int d, int mx, int my){
        state[a] = wrappingAdd(wrappingAdd(state[a], state[b]), mx);
        state[d] = rotateRight((state[d] ^ state[a]), 16);
        state[c] = wrappingAdd(state[c], state[d]);
        state[b] = rotateRight((state[b] ^ state[c]), 12);
        state[a] = wrappingAdd(wrappingAdd(state[a], state[b]), my);
        state[d] = rotateRight((state[d] ^ state[a]), 8);
        state[c] = wrappingAdd(state[c], state[d]);
        state[b] = rotateRight((state[b] ^ state[c]), 7);
    }

    private static void roundFn(int[] state, int[] m){
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

    private static int[] permute(int[] m){
        int[] permuted = new int[16];
        for(int i = 0;i<16;i++){
            permuted[i] = m[MSG_PERMUTATION[i]];
        }
        return permuted;
    }

    private static int[] compress(int[] chainingValue, int[] blockWords, long counter, int blockLen, int flags){
        int counterInt = (int) counter;
        int counterShift = (int) (counter >> 32);
        int[] state = {
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
        // Round 1
        roundFn(state, blockWords);
        blockWords = permute(blockWords);
        // Round 2
        roundFn(state, blockWords);
        blockWords = permute(blockWords);
        // Round 3
        roundFn(state, blockWords);
        blockWords = permute(blockWords);
        // Round 4
        roundFn(state, blockWords);
        blockWords = permute(blockWords);
        // Round 5
        roundFn(state, blockWords);
        blockWords = permute(blockWords);
        // Round 6
        roundFn(state, blockWords);
        blockWords = permute(blockWords);
        // Round 7
        roundFn(state, blockWords);

        return state;
    }

    // FIXME: i can probably do this more cleanly with the other bytebuffer methods (one buf, specify different start/stop indicies?)
    private static int[] wordsFromLEBytes(byte[] bytes){
        int[] words = new int[bytes.length/4];
        for(int i = 0; i< bytes.length/4; i++){
            byte[] arr = Arrays.copyOfRange(bytes, i, i+5);
            ByteBuffer buf = ByteBuffer.wrap(arr);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            words[i] = buf.getInt();
        }
        return words;
    }

    private static class Output {
        int[] inputChainingValue;
        int[] blockWords;
        long counter;
        int blockLen;
        int flags;

        private Output(int[] inputChainingValue, int[] blockWords, long counter, int blockLen, int flags) {
            this.inputChainingValue = inputChainingValue;
            this.blockWords = blockWords;
            this.counter = counter;
            this.blockLen = blockLen;
            this.flags = flags;
        }

        private int[] chainingValue(){
            return Arrays.copyOfRange(
                    compress(
                            inputChainingValue,
                            blockWords,
                            counter,
                            blockLen,
                            flags
                    ),0,9
            );
        }

        private byte[] rootOutputBytes(int outLen){
            int outputCounter = 0;
            int outputsNeeded = outLen/(2*OUT_LEN);
            byte[] hash = new byte[outLen];
            int i = 0;
            while(outputCounter < outputsNeeded){
                int[] words = compress(
                        inputChainingValue,
                        blockWords,
                        outputCounter,
                        blockLen,
                        flags | ROOT
                );
                for(int word: words){
                    for(byte b: ByteBuffer.allocate(4).putInt(word).array()){
                        hash[i] = b;
                        i+=1;
                    }
                }
                outputCounter+=1;
            }
            return hash;
        }
    }

    private class ChunkState {
        int[] chainingValue;
        int chunkCounter;
        byte[] block = new byte[BLOCK_LEN];
        byte blockLen = 0;
        byte blocksCompressed = 0;
        int flags;

        public ChunkState(int[] key, int chunkCounter, int flags){
            this.chainingValue = key;
            this.chunkCounter = chunkCounter;
            this.flags = flags;
        }

        public int len(){
            return BLOCK_LEN * blocksCompressed + blockLen;
        }

        private int startFlag(){
            return blocksCompressed == 0? CHUNK_START: 0;
        }

        private void update(byte[] input) {
            while (input.length != 0) {
                if (blockLen == BLOCK_LEN) {
                    int[] blockWords = wordsFromLEBytes(block);
                    this.chainingValue = Arrays.copyOfRange(
                            compress(
                                    this.chainingValue,
                                    blockWords,
                                    this.chunkCounter,
                                    BLOCK_LEN,
                                    this.flags | this.startFlag()
                            ), 0, 9);
                    blocksCompressed += 1;
                    this.block = new byte[BLOCK_LEN];
                    this.blockLen = 0;
                }

                // Take bytes out of the input and update
                int want = BLOCK_LEN - this.blockLen; // How many bytes we need to fill up the current block
                int canTake = Math.min(want, input.length);
                block = Arrays.copyOfRange(input, 0, canTake + 1);
                blockLen += canTake;
                input = Arrays.copyOfRange(input, canTake + 1, input.length); //TODO: check with debugger for off by one stuff here
            }
        }

        private Output createOutput(){
            int[] blockWords = wordsFromLEBytes(block);
            return new Output(
                chainingValue,
                    blockWords,
                    blockLen,
                    chunkCounter,
                    flags | startFlag() | CHUNK_END
            );
        }
    }

    // This is disgusting, there has to be a better way
    private static Output parentOutput(int[] leftChild, int[] rightChild, int[] key, int flags){
        int[] blockWords = new int[16];
        int i = 0;
        for(int x: leftChild){
            blockWords[i] = x;
            i+=1;
        }
        for(int x: rightChild){
            blockWords[i] = x;
            i+=1;
        }
        return new Output(
                key,
                blockWords,
                0,
                BLOCK_LEN,
                PARENT | flags
        );
    }

    private static int[] parentCV(int[] leftChild, int[] rightChild, int[] key, int flags){
        return parentOutput(leftChild, rightChild, key, flags).chainingValue();
    }

    // Hasher
    private ChunkState chunkState;
    private int[] key;
    private int[][] cvStack = new int[54][];
    private byte cvStackLen = 0;
    private int flags;

    public Blake3(){
        this(IV,0);
    }

    public Blake3(int[] key, int flags){
        this.chunkState = new ChunkState(key, 0, flags);
        this.key = key;
        this.flags = flags;
    }

    public Blake3(byte[] key){
        this(wordsFromLEBytes(key), KEYED_HASH);
    }

    public Blake3(String context){
        Blake3 contextHasher = new Blake3(IV, DERIVE_KEY_CONTEXT);
    }

    private void pushStack(int[] cv){
        this.cvStack[this.cvStackLen] = cv;
        cvStackLen+=1;
    }

    private int[] popStack(){
        this.cvStackLen-=1;
        return cvStack[cvStackLen];
    }

    private void addChunkChainingValue(int[] newCV, long totalChunks){
        while((totalChunks & 1) == 0){
            newCV = parentCV(popStack(), newCV, key, flags);
            totalChunks >>=1;
        }
        pushStack(newCV);
    }

    public void update(byte[] input){
        if(chunkState.len() == CHUNK_LEN){
            int[] chunkCV = chunkState.createOutput().chainingValue();
            int totalChunks = chunkState.chunkCounter + 1;
            addChunkChainingValue(chunkCV, totalChunks);
        }

        int want = CHUNK_LEN - chunkState.len();
        int take = Math.min(want, input.length);
        chunkState.update(Arrays.copyOfRange(input, 0, take + 1));
        input = Arrays.copyOfRange(input, take + 1, input.length);
    }

    public byte[] digest(int hashLen){
        Output output = this.chunkState.createOutput();
        int parentNodesRemaining = cvStackLen;
        while(parentNodesRemaining > 0){
            parentNodesRemaining -=1;
            output = parentOutput(
                    cvStack[parentNodesRemaining],
                    output.chainingValue(),
                    key,
                    flags
            );
        }
        return output.rootOutputBytes(hashLen);
    }
}
