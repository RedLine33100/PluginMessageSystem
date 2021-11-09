package fr.redline.pms.socket.connection;

import fr.redline.pms.socket.inter.DataTransfer;
import fr.redline.pms.socket.listener.client.ClientManager;
import fr.redline.pms.socket.listener.sub.Listener;

import java.nio.channels.SelectionKey;

public class ServerConnectionData extends ConnectionData {

    /*
        Use by fr.redline.pms.socket.listener.client.Client
     */

    LinkState linkState = LinkState.NOT_LOGGED;

    public ServerConnectionData(ClientManager clientManager, Listener listener, SelectionKey selectionKey) {
        super(clientManager, listener, selectionKey);
        super.setCryptState(CryptState.WAIT_RECEIVE);
    }

    public boolean isSocketConnected() {
        return this.getSocketChannel().isOpen();
    }

    public void addDataSender(DataTransfer dataTransfer) {
        getDataTransferList().add(dataTransfer);
    }

    public LinkState getLinkState() {
        return this.linkState;
    }

    public void setLinkState(LinkState linkState) {
        this.linkState = linkState;
    }

    public enum LinkState {
        NOT_LOGGED,
        FAILED,
        LOGGED
    }

}
