package com.github.rctcwyvrn.blake3java;

import java.util.Arrays;

public class Main {
    public static void main(String[] args){
//        String[] input = new String[]{
//                "a","b","c","d","e","f",
//        };
//
//        System.out.println(Arrays.toString(Arrays.copyOfRange(input, 0, 4 + 1)));
//        System.out.println(Arrays.toString(Arrays.copyOfRange(input, 4 + 1, input.length)));

        Blake3 hasher = new Blake3();
//        hasher.update("AAAAAAAAAAAAAAAA".getBytes());
//        hasher.update("AAAAAAAAAAAAAAAA".getBytes());
//        hasher.update("AAAAAAAAAAAAAAAA".getBytes());
//        hasher.update("AAAAAAAAAAAAAAAA".getBytes());
//        hasher.update("AAAAAAAAAAAAAAAA".getBytes());

        hasher.update("abc".getBytes());
        hasher.update("def".getBytes());

        String hexhash = hasher.hexdigest();
        System.out.println(hexhash);
    }
}
