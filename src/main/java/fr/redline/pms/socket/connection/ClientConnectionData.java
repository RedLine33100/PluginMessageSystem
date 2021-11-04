package fr.redline.pms.socket.connection;

import fr.redline.pms.socket.listener.Listener;
import fr.redline.pms.socket.manager.ClientManager;
import fr.redline.pms.socket.manager.ServerManager;

import java.nio.channels.SelectionKey;
import java.security.KeyPair;

public class ClientConnectionData extends ConnectionData {

    /*
    Use in Client Side
     */

    KeyPair keyPair;

    public ClientConnectionData(ClientManager clientManager, Listener listener, SelectionKey selectionKey) {
        super(clientManager, listener, selectionKey);
        if (((ServerManager) clientManager).getEncrypt())
            generateKeyPair(true);
    }

}
