package fr.redline.pms.socket.connection;

import fr.redline.pms.socket.listener.Listener;
import fr.redline.pms.socket.manager.ClientManager;
import fr.redline.pms.socket.manager.ServerManager;

import java.nio.channels.SelectionKey;

public class ClientConnectionData extends ConnectionData {

    /*
        Use by fr.redline.pms.socket.listener.server.Server
     */

    public ClientConnectionData(ClientManager clientManager, Listener listener, SelectionKey selectionKey) {
        super(clientManager, listener, selectionKey);
        if (((ServerManager) clientManager).getEncrypt()) {
            generateKeyPair(true);
            super.setCryptState(CryptState.WAIT_SEND);
        }
    }

}
