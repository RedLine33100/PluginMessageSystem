package fr.redline.pms.socket.connection;

import fr.redline.pms.socket.listener.client.ClientManager;
import fr.redline.pms.socket.listener.server.ServerManager;
import fr.redline.pms.socket.listener.sub.Listener;

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
