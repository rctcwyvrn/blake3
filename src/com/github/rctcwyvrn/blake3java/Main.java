package com.github.rctcwyvrn.blake3java;

import java.util.Arrays;

public class Main {
    public static void main(String[] args){
        Blake3 hasher = new Blake3();
        hasher.update("abc".getBytes());
        hasher.update("def".getBytes());
        byte[] hash = hasher.digest(32);
        System.out.println(Arrays.toString(hash));
    }
}
