package fr.redline.pms.socket.listener.server;

import fr.redline.pms.socket.connection.ClientConnectionData;
import fr.redline.pms.socket.connection.ConnectionData;
import fr.redline.pms.socket.connection.LinkState;
import fr.redline.pms.socket.inter.DataTransfer;
import fr.redline.pms.socket.inter.SocketState;
import fr.redline.pms.socket.listener.Listener;
import fr.redline.pms.socket.listener.ListenerType;
import fr.redline.pms.socket.manager.ServerManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Timer;
import java.util.logging.Level;

public class Server extends Listener {

    ServerState serverState = ServerState.MISSING_BOUND;
    Timer timer = new Timer();

    public Server(ServerManager serverManager) {
        super(serverManager, ListenerType.SERVER);
    }


    @Override
    public void stop() {
        getClientManager().sendLogMessage(Level.SEVERE, "Stopping serveur");
        stopConnection();

        if (serverState == ServerState.RUNNING)
            this.serverState = ServerState.STOP;
    }

    public boolean startServer() {

        if (this.getIpInfo() == null) {
            getClientManager().sendLogMessage(Level.WARNING, "No Ip set, please use setIpInfo()");
            return false;
        }

        getClientManager().sendLogMessage(Level.INFO, "Starting server");
        this.serverState = ServerState.STARTING;

        try {

            startConnection();
            ServerSocketChannel server = (ServerSocketChannel) getSocketChannel();

            server.bind(new InetSocketAddress(this.getIpInfo().getIp(false), this.getIpInfo().getPort()));

            server.configureBlocking(false);
            server.register(getSelector(), server.validOps(), null);

            this.serverState = ServerState.RUNNING;

            startListener();

            return true;

        } catch (IOException ioException) {

            this.serverState = ServerState.RUNNING_ERROR;
            stop();

            return false;

        }

    }

    public void onAcceptable(){

        try {
            SocketChannel socketChannel = ((ServerSocketChannel) getSocketChannel()).accept();
            if (socketChannel == null) {
                getClientManager().sendLogMessage(Level.INFO, "Phase 1) No connection accepted");
                return;
            }

            getClientManager().sendLogMessage(Level.FINE, "Phase 1) accepted connection from " + socketChannel.getRemoteAddress());
            socketChannel.configureBlocking(false);

            SelectionKey newKey = socketChannel.register(getSelector(), SelectionKey.OP_READ);
            ClientConnectionData socketData1 = new ClientConnectionData(getClientManager(), this, newKey);
            getClientManager().sendLogMessage(Level.INFO, "Phase 1) Connection: " + socketChannel.getRemoteAddress() + " registered with id: " + socketData1.getId());

            newKey.attach(socketData1);
            getClientManager().sendLogMessage(Level.INFO, "Phase 2) Asking: " + socketData1.getId() + " for connection account");

            socketData1.write("needMDP");
            newKey.interestOps(SelectionKey.OP_READ);

        } catch (IOException ignored) {
            this.serverState = ServerState.RUNNING_ERROR;
            stop();
        }

    }

    @Override
    public void notifyConnectionStop(ConnectionData connectionData) {
        // Useless here
    }

    public void onReadable(ConnectionData socketData, SelectionKey key) {

        getClientManager().sendLogMessage(Level.INFO, "Reading) Reading data on Socket: " + socketData.getId());
        String text = socketData.read();
        if (text == null)
            return;

        if (socketData.getLinkState() == LinkState.NOT_LOGGED) {
            getClientManager().sendLogMessage(Level.INFO, "Phase 2) Received auth from: " + socketData.getId());
            logIn(socketData, text);
            return;
        }

        DataTransfer dataTransfer = socketData.getFirstDataSender();
        if (dataTransfer != null) {
            getClientManager().sendLogMessage(Level.INFO, "Phase 4) Socket: " + socketData.getId() + "Already has dataTransfer, broadcasting text to it");
            dataTransfer.messageReceived(text);
            if (dataTransfer.canClose()) {
                getClientManager().sendLogMessage(Level.WARNING, "Phase 4) Socket: " + socketData.getId() + ": Closing connection due to dataTransfer finish: CURRENTLY_EXECUTING_READ_SERVER");
                socketData.removeFirstDataSender();
                dataTransfer.setSocketState(SocketState.FINISH_OKAY);
                key.interestOps(SelectionKey.OP_READ);
            }
            return;
        }

        getClientManager().sendLogMessage(Level.INFO, "Phase 3) Checking if text is dataTransferApproval on socket: " + socketData.getId());
        String[] dataList = text.split(getClientManager().getSocketSplit());
        if (!(dataList.length == 2 && dataList[0].equals("dataTransferApproval"))) {
            getClientManager().sendLogMessage(Level.SEVERE, "Phase 3) Text on socket: " + socketData.getId() + " is not an dataTransferApproval");
            return;
        }

        getClientManager().sendLogMessage(Level.FINE, "Phase 3) DataTransferApproval found on: " + socketData.getId());
        String title = dataList[1];
        dataTransfer = ((ServerManager) getClientManager()).getSocketReceiver(socketData, title);

        if (dataTransfer != null) {
            getClientManager().sendLogMessage(Level.FINE, "Phase 3) DataTransferApproval approval okay on: " + socketData.getId());
            socketData.getDataTransferList().add(dataTransfer);
            dataTransfer.setSocketState(SocketState.CURRENTLY_EXECUTING);
            socketData.write("dataTransferOkay");
            return;
        }

        getClientManager().sendLogMessage(Level.SEVERE, "Phase 3) DataTransferApproval not found on: " + socketData.getId());
        socketData.write("dataTransferWrong");

    }

    public void onWritable(ConnectionData socketData, SelectionKey key) {
        getClientManager().sendLogMessage(Level.INFO, "Write) Checking for dataTransfer linked on: " + socketData.getId());
        DataTransfer dataTransfer = socketData.getFirstDataSender();
        if (dataTransfer == null)
            return;
        getClientManager().sendLogMessage(Level.INFO, "Phase 4) Broadcasting write possibility on: " + socketData.getId());
        dataTransfer.socketWritable();
        if (!dataTransfer.canClose()) {
            return;
        }
        getClientManager().sendLogMessage(Level.WARNING, "Phase 4) Socket: " + socketData.getId() + ": Closing connection due to dataTransfer finish: CURRENTLY_EXECUTING_WRITE_SERVER");
        socketData.removeFirstDataSender();
        dataTransfer.setSocketState(SocketState.FINISH_OKAY);
        key.interestOps(SelectionKey.OP_READ);
    }

}
