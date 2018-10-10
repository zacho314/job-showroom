package rsa;

import java.math.BigInteger;

/**
 * Created by Martin Z on 12/04/2017.
 * 
 * Represention of a private RSA key.
 */
public class RSASecretKey {
    private BigInteger modulo;
    private BigInteger d;

    public RSASecretKey(BigInteger n, BigInteger d) {

        modulo = n;
        this.d = d;
    }

    public BigInteger getModulo() {
        return modulo;
    }

    public BigInteger getD() {
        return d;
    }
}
