package com.github.rctcwyvrn.blake3java;

import java.io.IOException;

public class Main {
    public static void main(String[] args){

        // Hashing strings
        Blake3 hasher = new Blake3();
        hasher.update("This is a string");
        String hexhash = hasher.hexdigest();
        if(!hexhash.equals("718b749f12a61257438b2ea6643555fd995001c9d9ff84764f93f82610a780f2")) throw new AssertionError();
        System.out.println("Success: " + hexhash);

        // Hashing files
        // Warning: Very slow due to lack of optimizations :c
        String filename = "src/com/github/rctcwyvrn/blake3java/Blake3.java";
        try {
            Blake3 fileHasher = new Blake3();
            fileHasher.updateFile(filename);
            String filehash = fileHasher.hexdigest();
            if (!filehash.equals("86894a68f93452be698f7ec97bc932df093f9905a52df73226cee2d713bf5323"))
                throw new AssertionError();
            System.out.println("Success: " + filehash);
        } catch (IOException e) {
            System.err.println("File not found: " + filename);
        }
    }
}
