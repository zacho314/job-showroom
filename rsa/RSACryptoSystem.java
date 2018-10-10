package rsa;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Created by Martin Z on 12/04/2017.
 * 
 * A class supporting
 * - RSA encryption
 * - RSA decryption and
 * - RSA key pair generation
 */
public class RSACryptoSystem {
    
    private static Random random = new Random();

    /**
     * Returns an RSAKeyPair consisting of a public and private key.
     * The keys are made up of the following values:
     * - n : modulos, shared by private and public key. The key pair
             cannot encrypt/ decrypt messages larger that this value.
     * - e : public key value used to encrypt messages - this is fixed
             to the constant 3 for encryption efficiency.
     * - d : private key value used to decrypt messages.
     *
     * The values are generated using the litterature from dSikkerhed 2017.
     * The key generation algorithm inefficient due to the use of while-loops
     * to find suitable primes.
     */
    public static RSAKeyPair keyGen(int k) {
        
        // Step1.: Generate random prime p, where gcd(p - 1, 3) = 1

        BigInteger p;        
        BigInteger e = new BigInteger("3");
        
        while(true) {
            int lenght = random.nextInt(k-3)+3;
            p = BigInteger.probablePrime(lenght, random);

            // If gcd(p - 1, 3) = 1
            if (p.subtract(BigInteger.ONE).gcd(e).equals(BigInteger.ONE)) {
                break;
            }
        }

        // Step2.: Generate random prime q, where gcd(q - 1, 3) = 1
        //         and the bitlength of p * q is length

        BigInteger q;
        
        while (true) {            
            BigInteger offset = new BigInteger(k-1,random);
            BigInteger r = new BigInteger("2").pow(k-1).add(offset);

            q = r.divide(p).nextProbablePrime();

            // if gcd(q - 1, 3) = 1 and key has proper length
            if (q.subtract(BigInteger.ONE).gcd(e).equals(BigInteger.ONE) &&
                p.multiply(q).bitLength() == k) {
                break;
            }
        }

        //Computing d:
        BigInteger d = e.modInverse(p.subtract(BigInteger.ONE)
                                    .multiply(q.subtract(BigInteger.ONE)));

        //Computing n:
        BigInteger n = p.multiply(q);

        return new RSAKeyPair(n, e, d);
    }


    public static BigInteger encrypt(RSAPublicKey pk, BigInteger message) {
        return message.modPow(pk.getE(), pk.getModulo());
    }

    public static BigInteger decrypt(RSASecretKey sk, BigInteger cipher) {
        return cipher.modPow(sk.getD(), sk.getModulo());
    }

    public static BigInteger sign(RSASecretKey sk, BigInteger m){
        BigInteger hash = hashWithSHA256(m);
        return decrypt(sk,hash);
    };
    public static boolean verify(RSAPublicKey pk, BigInteger message, BigInteger signature) {
        BigInteger hash = hashWithSHA256(message);
        return encrypt(pk, signature).equals(hash);
    }

    public static BigInteger hashWithSHA256(BigInteger message) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update(message.toByteArray());
        BigInteger hash = new BigInteger(1,md.digest());
        return hash;
    }

}

