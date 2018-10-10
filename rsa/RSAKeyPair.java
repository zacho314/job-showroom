package rsa;

import java.math.BigInteger;

/**
 * Created by Martin Z on 12/04/2017.
 * 
 * Representation of an RSA key pair.
 */
public class RSAKeyPair {
    private RSASecretKey secretKey;
    private RSAPublicKey publicKey;

    public RSAKeyPair(BigInteger n, BigInteger e, BigInteger d) {
        this.secretKey = new RSASecretKey(n,d);
        this.publicKey = new RSAPublicKey(n,e);
    }

    public RSASecretKey getSecretKey() {
        return secretKey;
    }

    public RSAPublicKey getRSAPublicKey() {
        return publicKey;
    }
}
