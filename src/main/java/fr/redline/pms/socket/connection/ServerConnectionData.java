package fr.redline.pms.socket.connection;

import fr.redline.pms.socket.inter.DataTransfer;
import fr.redline.pms.socket.listener.Listener;
import fr.redline.pms.socket.manager.ClientManager;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ServerConnectionData extends ConnectionData {

    /*
    Use on server Side
     */

    private final SelectionKey selectionKey;

    private String pass = null;

    public ServerConnectionData(ClientManager clientManager, Listener listener, SelectionKey selectionKey) {
        super(clientManager, listener);
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
