package com.github.rctcwyvrn.blake3java;

import javax.swing.*;
import java.nio.charset.Charset;
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
//        hasher.update("AAAAAAAAAAAAAAAA");

        //hasher.update("abc");
        //hasher.update("def");

        hasher.update("This is a string"); //Should be 718b749f12a61257438b2ea6643555fd995001c9d9ff84764f93f82610a780f2

        String hexhash = hasher.hexdigest();
        System.out.println(hexhash);
    }
}
