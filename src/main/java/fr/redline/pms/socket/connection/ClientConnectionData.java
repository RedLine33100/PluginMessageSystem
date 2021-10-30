package fr.redline.pms.socket.connection;

import fr.redline.pms.socket.listener.Listener;
import fr.redline.pms.socket.manager.ClientManager;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ClientConnectionData extends ConnectionData {

    /*
    Use in Client Side
     */

    private final SelectionKey selectionKey;

    public ClientConnectionData(ClientManager clientManager, Listener listener, SelectionKey selectionKey) {
        super(clientManager, listener);
        this.selectionKey = selectionKey;
    }

    public SocketChannel getSocketChannel() {
        return (SocketChannel) this.selectionKey.channel();
    }

    public SelectionKey getSelectionKey() {
        return this.selectionKey;
    }

}
