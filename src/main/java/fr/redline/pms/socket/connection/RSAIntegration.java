package fr.redline.pms.socket.connection;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSAIntegration {

    private PublicKey publicKey = null;
    private PrivateKey privateKey = null;

    public KeyPair generateKeyPair(boolean set) {
        KeyPair keyPair = null;
        KeyPairGenerator keyPairGenerator;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            SecureRandom secureRandom = new SecureRandom();
            keyPairGenerator.initialize(2048, secureRandom);
            keyPair = keyPairGenerator.generateKeyPair();
            if (!set)
                return keyPair;
            setPublicKey(keyPair.getPublic());
            setPrivateKey(keyPair.getPrivate());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return keyPair;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public String getPublicKeyString() {
        if (getPublicKey() == null)
            return null;
        return Base64.getEncoder().encodeToString(getPublicKey().getEncoded());
    }

    public boolean setPublicKey(String publicKey) {

        byte[] byte_pubkey = Base64.getDecoder().decode(publicKey);

        KeyFactory factory = null;
        try {
            factory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            assert factory != null;
            setPublicKey(factory.generatePublic(new X509EncodedKeySpec(byte_pubkey)));
            return true;
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

        return false;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public String getPrivateKeyString() {
        if (getPrivateKey() == null)
            return null;
        return Base64.getEncoder().encodeToString(getPrivateKey().getEncoded());
    }

    public boolean setPrivateKey(String privateKey) {

        byte[] byte_pubkey = Base64.getDecoder().decode(privateKey);

        KeyFactory factory = null;
        try {
            factory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            assert factory != null;
            setPrivateKey(factory.generatePrivate(new X509EncodedKeySpec(byte_pubkey)));
            return true;
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

        return false;
    }


    public String encrypt(String data) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        if (getPublicKey() == null && getPrivateKey() == null) return data;
        Cipher cipher = Cipher.getInstance("RSA");
        Key key;
        if (getPrivateKey() != null)
            key = getPrivateKey();
        else key = getPublicKey();
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
    }

    public String decrypt(byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        if (getPrivateKey() == null) return Base64.getEncoder().encodeToString(data);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, getPrivateKey());
        return new String(cipher.doFinal(data));
    }

    public String decrypt(String data) throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        if (getPrivateKey() == null) return data;
        return decrypt(Base64.getDecoder().decode(data.getBytes()));
    }

}
