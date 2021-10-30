package fr.redline.pms.socket.listener;

import fr.redline.pms.socket.connection.ConnectionData;
import fr.redline.pms.socket.connection.LinkState;
import fr.redline.pms.socket.connection.ServerConnectionData;
import fr.redline.pms.socket.inter.DataTransfer;
import fr.redline.pms.socket.inter.SocketState;
import fr.redline.pms.socket.manager.ClientManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

public class Client extends Listener {

    public Client(ClientManager clientManager) {
        super(clientManager, ListenerType.CLIENT);
    }

    public ServerConnectionData connect(String account) {

        String accountPassword = getClientManager().getCredentialClass().getEncryptedPassword(account);

        if (accountPassword == null)
            return null;

        if (getIpInfo() == null) {
            getClientManager().sendLogMessage(Level.WARNING, "No Ip set, please use setIpInfo()");
            return null;
        }

        try {
            startConnection();

            SocketChannel socketChannel = (SocketChannel) getSocketChannel();

            socketChannel.connect(new InetSocketAddress(this.getIpInfo().getIp(), this.getIpInfo().getPort()));

            ServerConnectionData socketData = new ServerConnectionData(getClientManager(), this, getSocketChannel().register(getSelector(), SelectionKey.OP_CONNECT));

            getClientManager().sendLogMessage(Level.INFO, socketData.getId() + ": Connecting to: " + ipInfo);
            socketData.getSelectionKey().attach(socketData);
            socketData.setAccount(account);

            socketData.setPassword(accountPassword);

            if (socketChannel.isConnected() || socketChannel.finishConnect()) {
                socketChannel.configureBlocking(false);
                startListener();
            } else {
                socketData.closeConnection();
                stopConnection();
                socketData = null;
            }

            return socketData;
        } catch (IOException e) {
            e.printStackTrace();
            stopConnection();
            return null;
        }
    }

    public void addDataTransfer(ServerConnectionData socketData, DataTransfer dataTransfer) {
        socketData.addDataSender(dataTransfer);
    }

    public void onReadable(ConnectionData socketData, SelectionKey key) {

        if (!(socketData instanceof ServerConnectionData)) {
            return;
        }

        String received = socketData.read();
        if (received == null) {
            getClientManager().sendLogMessage(Level.INFO, socketData.getId() + ": Received: null");
            return;
        }

        getClientManager().sendLogMessage(Level.FINE, socketData.getId() + ": Received: " + received);
        socketData.updateLastUse();

        if (socketData.getLinkState() == LinkState.NOT_LOGGED) {
            getClientManager().sendLogMessage(Level.INFO, socketData.getId() + ": Treat 1");
            switch (received) {
                case "needMDP": {
                    getClientManager().sendLogMessage(Level.INFO, socketData.getId() + ": Sending credential");
                    sendCredential(socketData, ((ServerConnectionData) socketData).getPassword());
                    break;
                }
                case "logOkay": {
                    getClientManager().sendLogMessage(Level.FINE, socketData.getId() + ": Registration Okay");
                    socketData.setLinkState(LinkState.LOGGED);
                    key.interestOps(SelectionKey.OP_WRITE);
                    break;
                }
                case "logWrong": {
                    getClientManager().sendLogMessage(Level.SEVERE, socketData.getId() + ": Credential Wrong");
                    socketData.setLinkState(LinkState.FAILED);
                    socketData.closeConnection();
                    break;
                }
            }

            return;
        }

        getClientManager().sendLogMessage(Level.INFO, socketData.getId() + ": Treat 2");

        DataTransfer dataTransfer = socketData.getFirstDataSender();
        if (dataTransfer == null)
            return;

        if (dataTransfer.isSocketState(SocketState.WAIT_APPROVAL_SEND)) {
            key.interestOps(SelectionKey.OP_WRITE);
        } else if (dataTransfer.isSocketState(SocketState.WAIT_APPROVAL)) {
            switch (received) {
                case "dataTransferOkay": {
                    getClientManager().sendLogMessage(Level.INFO, socketData.getId() + ": Data transfer accepted");
                    dataTransfer.setSocketState(SocketState.CURRENTLY_EXECUTING);
                    if (!dataTransfer.canClose()) {
                        key.interestOps(SelectionKey.OP_WRITE);
                        return;
                    }

                    getClientManager().sendLogMessage(Level.WARNING, socketData.getId() + ": Closing connection due to dataTransfer finish: WAIT_APPROVAL");
                    dataTransfer.setSocketState(SocketState.FINISH_OKAY);
                    socketData.removeFirstDataSender();
                    if (socketData.getFirstDataSender() == null)
                        socketData.closeConnection();

                }
                case "dataTransferWrong": {
                    getClientManager().sendLogMessage(Level.SEVERE, socketData.getId() + ": Data transfer refused, DATATRANSFER_REFUSED_SERVER");
                    dataTransfer.setSocketState(SocketState.FINISH_ERROR);
                    socketData.removeFirstDataSender();
                    if (socketData.getFirstDataSender() == null)
                        socketData.closeConnection();
                }
            }
        } else if (dataTransfer.isSocketState(SocketState.CURRENTLY_EXECUTING)) {

            getClientManager().sendLogMessage(Level.INFO, socketData.getId() + ": Broadcasting message received");
            dataTransfer.messageReceived(received);
            if (!dataTransfer.canClose()) {
                return;
            }

            getClientManager().sendLogMessage(Level.WARNING, socketData.getId() + ": Closing connection due to dataTransfer finish: CURRENTLY_EXECUTING");
            dataTransfer.setSocketState(SocketState.FINISH_OKAY);
            socketData.removeFirstDataSender();
            if (socketData.getFirstDataSender() == null)
                socketData.closeConnection();
        } else if (dataTransfer.isSocketState(SocketState.FINISH_OKAY) || dataTransfer.isSocketState(SocketState.FINISH_ERROR)) {
            socketData.removeFirstDataSender();
            key.interestOps(SelectionKey.OP_WRITE);
        }

    }

    public void onWritable(ConnectionData socketData, SelectionKey key) {
        DataTransfer dataTransfer = socketData.getFirstDataSender();
        if (dataTransfer != null && socketData.getLinkState() == LinkState.LOGGED) {
            if (dataTransfer.isSocketState(SocketState.WAIT_APPROVAL_SEND)) {
                getClientManager().sendLogMessage(Level.INFO, socketData.getId() + ": Sending data transfer approval");
                dataTransfer.setSocketState(SocketState.WAIT_APPROVAL);
                socketData.write("dataTransferApproval" + getClientManager().getSocketSplit() + dataTransfer.getTitle());
                key.interestOps(SelectionKey.OP_READ);
            } else if (dataTransfer.isSocketState(SocketState.CURRENTLY_EXECUTING)) {
                getClientManager().sendLogMessage(Level.INFO, socketData.getId() + ": Socket writable to dataTransfer");
                dataTransfer.socketWritable();
                if (dataTransfer.canClose()) {
                    getClientManager().sendLogMessage(Level.WARNING, socketData.getId() + ": Closing connection due to dataTransfer finish: CURRENTLY_EXECUTING_WRITE");
                    dataTransfer.setSocketState(SocketState.FINISH_OKAY);
                    socketData.removeFirstDataSender();
                }
            }
        } else {
            getClientManager().sendLogMessage(Level.INFO, socketData.getId() + ": No Data to transfer");
        }
    }

    @Override
    public void onAcceptable() {

    }

    @Override
    public void notifyConnectionStop(ConnectionData connectionData) {
        stop();
    }

    @Override
    public void stop() {
        stopListener();
        stopConnection();
    }

}
