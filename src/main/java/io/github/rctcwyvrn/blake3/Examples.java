package io.github.rctcwyvrn.blake3;

import java.io.File;
import java.io.IOException;

public class Examples {
    public static void main(String[] args){

        // Hashing strings
        Blake3 hasher = new Blake3();
        hasher.update("This is a string".getBytes());
        String hexhash = hasher.hexdigest();
        System.out.println("Hash of 'This is a string' = " + hexhash);

        // Hashing files
        String filename = "LICENSE";
        try {
            Blake3 fileHasher = new Blake3();
            fileHasher.update(new File(filename));
            String filehash = fileHasher.hexdigest();
            if (!filehash.equals("381f3baeddb0ce5202ac9528ecc787c249901e74528ba2cbc5546567a2e0bd33"))
                throw new AssertionError(filehash);
            System.out.println("Hash of the License file = " + filehash);
        } catch (IOException e) {
            System.err.println("File not found: " + filename);
        }
    }
}
