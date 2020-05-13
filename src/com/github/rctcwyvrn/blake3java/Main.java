package com.github.rctcwyvrn.blake3java;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args){
        Blake3 hasher = new Blake3();
        hasher.update("This is a string");
        String hexhash = hasher.hexdigest();
        if(!hexhash.equals("718b749f12a61257438b2ea6643555fd995001c9d9ff84764f93f82610a780f2")) throw new AssertionError();
        System.out.println("Success");

        if(args.length != 0){
            String filename = args[0];
            Blake3 fileHasher = new Blake3();
            try {
                byte[] fileContent = Files.readAllBytes(Paths.get(filename));
                short[] converted = new short[fileContent.length];
                for(int i = 0; i<fileContent.length; i++){
                    converted[i] = fileContent[i];
                }
                fileHasher.updateRaw(converted);
                System.out.println(fileHasher.hexdigest() + "  " + filename);
            } catch (IOException e) {
                System.err.println("File not found: " + filename);
            }
        }
    }
}
