package rsa;

import java.math.BigInteger;

/**
 * Created by Martin Z on 12/04/2017.
 * 
 * Represention of a public RSA key
 */
public class RSAPublicKey {
    private BigInteger modulo;
    private BigInteger e;

    public RSAPublicKey(BigInteger n, BigInteger e){
        this.modulo = n;
        this.e = e;
    }

    public BigInteger getModulo() {
        return modulo;
    }

    public BigInteger getE() {
        return e;
    }
}
