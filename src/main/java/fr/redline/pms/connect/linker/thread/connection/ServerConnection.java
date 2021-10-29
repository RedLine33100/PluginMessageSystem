package fr.redline.pms.connect.linker.thread.connection;

import fr.redline.pms.connect.linker.SocketGestion;
import fr.redline.pms.connect.linker.inter.DataTransfer;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ServerConnection extends Connection {

    /*
    Use on server Side
     */

    private final SelectionKey selectionKey;

    private String pass = null;

    public ServerConnection(SocketGestion socketGestion, SelectionKey selectionKey) {
        super(socketGestion);
        this.selectionKey = selectionKey;
    }

    public boolean isSocketConnected() {
        return this.getSocketChannel().isOpen();
    }

    public SocketChannel getSocketChannel() {
        return (SocketChannel) this.selectionKey.channel();
    }

    public SelectionKey getSelectionKey() {
        return this.selectionKey;
    }

    public String getPassword() {
        return this.pass;
    }

    public void setPassword(String pass) {
        this.pass = pass;
    }

    public void addDataSender(DataTransfer dataTransfer) {
        getDataTransferList().add(dataTransfer);
    }

}
