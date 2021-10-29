package fr.redline.pms.connect.linker.thread.connection;

import fr.redline.pms.connect.linker.SocketGestion;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ClientConnection extends Connection {

    /*
    Use in Client Side
     */

    private final SelectionKey selectionKey;

    public ClientConnection(SocketGestion socketGestion, SelectionKey selectionKey) {
        super(socketGestion);
        this.selectionKey = selectionKey;
    }

    public SocketChannel getSocketChannel() {
        return (SocketChannel) this.selectionKey.channel();
    }

    public SelectionKey getSelectionKey() {
        return this.selectionKey;
    }

}
