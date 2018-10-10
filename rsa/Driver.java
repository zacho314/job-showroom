package rsa;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Created by Martin Z on 12/04/2017.
 * 
 * Class for testing RSACryptoSystem - in the absence of a good Java unit-testing
 * framework like JUnit.
 */
public class Driver {
    public static void main(String[] args) {

        int keylength = 2000;
        confidentialityTest(keylength);
        authenticityTest(keylength);
    
    }

    private static void confidentialityTest(int keylength) {
        System.out.println("Generating " + keylength + " bit key for confidentiality testing...");
        RSAKeyPair keyPair = RSACryptoSystem.keyGen(keylength);
        RSAPublicKey pk = keyPair.getRSAPublicKey();
        RSASecretKey sk = keyPair.getSecretKey();

        ArrayList<BigInteger> messages = new ArrayList<>(Arrays.asList(
                new BigInteger("250250"),
                new BigInteger("250250250250"),
                new BigInteger("1000"),
                new BigInteger("10000"),
                new BigInteger("100000"),
                new BigInteger("1000000"),
                new BigInteger("10000000"),
                new BigInteger("100000000"),
                new BigInteger("1000000000"),
                new BigInteger("10000000000"),
                new BigInteger("100000000000"),
                new BigInteger("123456789123456789"),
                new BigInteger("314159265358979323646"),
                new BigInteger("2222444466668888")
        ));

        System.out.println("Message/ Encrypted message/ Decrypted message:");
        System.out.println();

        ArrayList<BigInteger> decryptedMessages = new ArrayList<>();
        for(BigInteger m : messages) {
            BigInteger encryptedMessage = RSACryptoSystem.encrypt(pk,m);
            BigInteger decryptedMessage = RSACryptoSystem.decrypt(sk,encryptedMessage);

            decryptedMessages.add(decryptedMessage);
            System.out.println("m:      " + m);
            System.out.println("c:      " + encryptedMessage);
            System.out.println("Dsk(c): " + decryptedMessage);
            System.out.println();
        }

        boolean success = messages.equals(decryptedMessages);
        System.out.println("Dsk(Epk(m)) for all m: " + success);
    }

    private static void authenticityTest(int keylength) {
        System.out.println("Generating " + keylength + " bit key for confidentiality testing...");
        RSAKeyPair keyPair = RSACryptoSystem.keyGen(keylength);
        RSAPublicKey pk = keyPair.getRSAPublicKey();
        RSASecretKey sk = keyPair.getSecretKey();
        System.out.println();

        ArrayList<BigInteger> messages = new ArrayList<>(Arrays.asList(
                new BigInteger("250250"),
                new BigInteger("250250250250"),
                new BigInteger("1000"),
                new BigInteger("10000"),
                new BigInteger("100000"),
                new BigInteger("1000000"),
                new BigInteger("10000000"),
                new BigInteger("100000000"),
                new BigInteger("1000000000"),
                new BigInteger("10000000000"),
                new BigInteger("100000000000"),
                new BigInteger("123456789123456789"),
                new BigInteger("314159265358979323646"),
                new BigInteger("2222444466668888")
        ));

        System.out.println("==============");
        System.out.println("Positive tests");
        System.out.println("==============");
        
        for (BigInteger m : messages) {
            BigInteger signature = RSACryptoSystem.sign(sk, m);
            boolean result = RSACryptoSystem.verify(pk, m, signature);
            System.out.println("Message: " + m);
            System.out.println("Signature of Message: " + signature);
            System.out.println("Result of verifying (Message, Signature of Message): " + result);
            System.out.println();
        }

        System.out.println("==============");
        System.out.println("Negative tests");
        System.out.println("==============");
        
        BigInteger message = new BigInteger("123456789123456789");
        BigInteger modifiedMessage = new BigInteger("42");
        BigInteger signature = RSACryptoSystem.sign(sk, modifiedMessage);
        boolean result = RSACryptoSystem.verify(pk, message, signature);
        
        System.out.println("Message: " + message);
        System.out.println("OtherMessage: " + modifiedMessage);
        System.out.println("Signature of OtherMessage: " + signature);
        System.out.println("Result of verifying (Message, Signature of OtherMessage): " + result);
        System.out.println();

        signature = RSACryptoSystem.sign(sk, message);
        BigInteger modifiedSignature = new BigInteger("117");
        result = RSACryptoSystem.verify(pk, message, modifiedSignature);
        
        System.out.println("Message: " + message);
        System.out.println("Signature of Message: " + signature);
        System.out.println("Modified signature: " + modifiedMessage);
        System.out.println("Result of verifying (Message, Modified signature): " + result);
        System.out.println();

        System.out.println("==============");
        System.out.println("Hashing speed measurement:");
        System.out.println("==============");
        BigInteger hash;
        try {
            Path path = Paths.get("rsa/randombytes");
            byte[] data = Files.readAllBytes(path);

            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            md.update(data);

            long start = System.nanoTime();
            hash = new BigInteger(1, md.digest());
            long finish = System.nanoTime();

            double hashingSeconds = (finish - start) * Math.pow(10, -9);
            double hashingbps = 8 * 10000.0 / hashingSeconds;

            System.out.println("Hash value of 10 random kilobytes: " + hash);
            System.out.println("Hashing took: " + hashingSeconds + " seconds.");
            System.out.println("The hashing speed was: " + hashingbps + " bps.");
            System.out.println();

            System.out.println("==============");
            System.out.println("Signing speed measurement:");
            System.out.println("==============");

            start = System.nanoTime();
            RSACryptoSystem.decrypt(sk, hash);
            finish = System.nanoTime();

            double signingSeconds = (finish - start) * Math.pow(10, -9);
            double signingbps = 2000.0 / signingSeconds;

            System.out.println("Time spent producing a signature on a hash value " +
                    "when using a 2000 bit key: " + signingSeconds + " seconds.");
            System.out.println("The signing speed was: " + signingbps + " bps.");
            System.out.println();

            System.out.println("==============");
            System.out.println("Comparison of signing and hashing speed:");
            System.out.println("==============");
            System.out.println("Hashing took " + hashingbps + " bps while signing took " + signingbps + " bps. " +
                    "Hence, hashing was " + hashingbps / signingbps + " times faster than signing.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
