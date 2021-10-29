package fr.redline.pms.socket.connection;

import fr.redline.pms.socket.manager.ClientManager;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ClientConnection extends Connection {

    /*
    Use in Client Side
     */

    private final SelectionKey selectionKey;

    public ClientConnection(ClientManager clientManager, SelectionKey selectionKey) {
        super(clientManager);
        this.selectionKey = selectionKey;
    }

    public SocketChannel getSocketChannel() {
        return (SocketChannel) this.selectionKey.channel();
    }

    public SelectionKey getSelectionKey() {
        return this.selectionKey;
    }

}
