package com.github.rctcwyvrn.blake3;

import java.io.File;
import java.io.IOException;

public class Examples {
    public static void main(String[] args){

        // Hashing strings
        Blake3 hasher = new Blake3();
        hasher.update("This is a string".getBytes());
        String hexhash = hasher.hexdigest();
        if(!hexhash.equals("718b749f12a61257438b2ea6643555fd995001c9d9ff84764f93f82610a780f2")) throw new AssertionError();
        System.out.println("Success: " + hexhash);

        // Hashing files
        // Warning: Very slow for large files due to lack of optimizations :c
        String filename = "LICENSE";
        try {
            Blake3 fileHasher = new Blake3();
            fileHasher.update(new File(filename));
            String filehash = fileHasher.hexdigest();
            if (!filehash.equals("381f3baeddb0ce5202ac9528ecc787c249901e74528ba2cbc5546567a2e0bd33"))
                throw new AssertionError();
            System.out.println("Success: " + filehash);
        } catch (IOException e) {
            System.err.println("File not found: " + filename);
        }
    }
}
