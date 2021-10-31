package fr.redline.pms.socket.connection;

import fr.redline.pms.socket.listener.Listener;
import fr.redline.pms.socket.manager.ClientManager;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.channels.SelectionKey;
import java.security.*;
import java.util.Base64;

public class ClientConnectionData extends ConnectionData {

    /*
    Use in Client Side
     */

    KeyPair keyPair;

    public ClientConnectionData(ClientManager clientManager, Listener listener, SelectionKey selectionKey) {
        super(clientManager, listener, selectionKey);
        KeyPairGenerator keyPairGenerator;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            SecureRandom secureRandom = new SecureRandom();
            keyPairGenerator.initialize(2048, secureRandom);
            keyPair = keyPairGenerator.generateKeyPair();
            this.setPublicKey(keyPair.getPublic());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public String decrypt(byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, getKeyPair().getPrivate());
        return new String(cipher.doFinal(data));
    }

    public String decrypt(String data) throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        return decrypt(Base64.getDecoder().decode(data.getBytes()));
    }


}
