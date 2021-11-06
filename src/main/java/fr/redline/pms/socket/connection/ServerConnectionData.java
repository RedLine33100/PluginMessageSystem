package fr.redline.pms.socket.connection;

import fr.redline.pms.socket.inter.DataTransfer;
import fr.redline.pms.socket.listener.Listener;
import fr.redline.pms.socket.manager.ClientManager;

import java.nio.channels.SelectionKey;

public class ServerConnectionData extends ConnectionData {

    /*
    Use on server Side
     */

    private String pass = null;

    public ServerConnectionData(ClientManager clientManager, Listener listener, SelectionKey selectionKey) {
        super(clientManager, listener, selectionKey);
        super.setCryptState(CryptState.WAIT_RECEIVE);
    }

    public boolean isSocketConnected() {
        return this.getSocketChannel().isOpen();
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
