package fr.redline.pms.socket.listener.server;

import fr.redline.pms.socket.manager.ServerManager;
import fr.redline.pms.socket.inter.DataTransfer;
import fr.redline.pms.socket.inter.SocketState;
import fr.redline.pms.socket.connection.LinkState;
import fr.redline.pms.socket.connection.ClientConnection;
import fr.redline.pms.utils.IpInfo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

public class Server extends Thread {
    ServerManager serverManager;

    IpInfo ipInfo = null;

    /// Socket Needed
    ServerSocketChannel server = null;
    Selector serverSelector = null;
    SelectionKey serverKey = null;

    ServerState serverState = ServerState.MISSING_BOUND;
    Timer timer = new Timer();

    public Server(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    public IpInfo getRunningIP() {
        return ipInfo;
    }

    public boolean setRunningIP(IpInfo ipInfo) {
        if (this.serverState == ServerState.RUNNING)
            return false;
        this.ipInfo = ipInfo;
        if (ipInfo != null)
            this.serverState = ServerState.STOP;
        else this.serverState = ServerState.MISSING_BOUND;
        return true;
    }

    public void stopServer() {
        this.serverManager.sendLogMessage(Level.SEVERE, "Stopping serveur");

        if (this.server != null) {
            try {
                this.server.close();
                this.server = null;
            } catch (IOException ignore) {
                if (serverState == ServerState.RUNNING)
                    this.serverState = ServerState.STOP_ERROR;
            }
        }

        if (this.serverSelector != null) {
            try {
                this.serverSelector.close();
                this.serverSelector = null;
            } catch (IOException ignore) {
                if (serverState == ServerState.RUNNING)
                    this.serverState = ServerState.STOP_ERROR;
            }
        }

        timer.cancel();
        timer.purge();

        if (serverState == ServerState.RUNNING)
            this.serverState = ServerState.STOP;
    }

    public boolean startServer() {

        this.serverManager.sendLogMessage(Level.INFO, "Starting server");
        this.serverState = ServerState.STARTING;

        try {

            this.serverSelector = Selector.open();
            this.server = ServerSocketChannel.open();

            this.server.bind(new InetSocketAddress(this.ipInfo.getIp(), this.ipInfo.getPort()));

            this.server.configureBlocking(false);
            this.serverKey = this.server.register(this.serverSelector, this.server.validOps(), null);

            this.serverState = ServerState.RUNNING;

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    runServer();
                }
            }, 100, 100);

            return true;

        } catch (IOException ioException) {

            this.serverState = ServerState.RUNNING_ERROR;
            stopServer();

            return false;

        }

    }

    private void runServer() {

        if (this.server == null || this.serverSelector == null || this.serverKey == null || !this.server.isOpen() || !this.serverSelector.isOpen() || this.serverState != ServerState.RUNNING) {
            this.serverState = ServerState.RUNNING_ERROR;
            stopServer();
            return;
        }

        try {
            this.serverSelector.select();

            for (SelectionKey key : this.serverSelector.selectedKeys()) {

                if (key.isAcceptable()) {
                    acceptableKey();
                    continue;
                }

                ClientConnection socketData = (ClientConnection) key.attachment();
                if (socketData == null) {
                    key.channel().close();
                    this.serverManager.sendLogMessage(Level.SEVERE, "Wrong) SocketData not found on key attachement");
                    continue;
                }

                this.serverManager.sendLogMessage(Level.INFO, "Check) Socket: " + socketData.getId() + ", Server key state: " + key.interestOps());
                if (key.isReadable()) {
                    readableKey(socketData, key);
                }else if (key.isWritable() && socketData.getLinkState() == LinkState.LOGGED) {
                    writableKey(socketData, key);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected boolean logIn(ClientConnection socketData, String text) {
        if (socketData.getLinkState() == LinkState.LOGGED) {
            this.serverManager.sendLogMessage(Level.SEVERE, "Phase 2) Error on connecting " + socketData.getId() + " Reason: Socket already connected");
            return true;
        }
        if (text == null) {
            this.serverManager.sendLogMessage(Level.SEVERE, "Phase 2) Error on connecting " + socketData.getId() + " Reason: Login text null");
            return false;
        }
        String[] logInInfo = text.split(this.serverManager.getSocketSplit());
        boolean authorized = false;
        if (!logInInfo[0].equals("logCred")) {
            this.serverManager.sendLogMessage(Level.SEVERE, "Phase 2) Error on connecting " + socketData.getId() + " Reason: Text not recognize as login text");
            return false;
        }
        this.serverManager.sendLogMessage(Level.INFO, "Phase 2) Authing socket " + socketData.getId() + ", text recognize as login text");
        if (logInInfo.length == 2) {
            this.serverManager.sendLogMessage(Level.INFO, "Phase 2) Authing socket " + socketData.getId() + ", text recognize as login text with 3 args");
            String account = null;
            if (!logInInfo[1].equals("null"))
                account = logInInfo[1];
            authorized = this.serverManager.getCredentialClass().isAccount(account, null);
        } else if (logInInfo.length == 3) {
            this.serverManager.sendLogMessage(Level.INFO, "Phase 2) Authing socket " + socketData.getId() + ", text recognize as login text with 4 args");
            authorized = this.serverManager.getCredentialClass().isAccount(logInInfo[1], logInInfo[2]);
        }
        socketData.getSelectionKey().interestOps(SelectionKey.OP_WRITE);

        socketData.setLinkState(authorized ? LinkState.LOGGED : LinkState.NOT_LOGGED);
        if (authorized) {
            this.serverManager.sendLogMessage(Level.FINE, "Phase 2) Authing socket " + socketData.getId() + ", Socket logged");
            socketData.updateLastUse();
            socketData.write("logOkay");
        } else {
            this.serverManager.sendLogMessage(Level.SEVERE, "Phase 2) Authing socket " + socketData.getId() + ", Socket logging failed");
            socketData.write("logWrong");
            socketData.closeConnection();
        }
        socketData.getSelectionKey().interestOps(1);
        socketData.updateLastUse();
        return authorized;
    }

    public void acceptableKey(){

        try {

            SocketChannel socketChannel = this.server.accept();
            if (socketChannel == null) {
                this.serverManager.sendLogMessage(Level.INFO, "Phase 1) No connection accepted");
                return;
            }

            this.serverManager.sendLogMessage(Level.FINE, "Phase 1) accepted connection from " + socketChannel.getRemoteAddress());
            socketChannel.configureBlocking(false);

            SelectionKey newKey = socketChannel.register(this.serverSelector, SelectionKey.OP_READ);
            ClientConnection socketData1 = new ClientConnection(this.serverManager, newKey);
            this.serverManager.sendLogMessage(Level.INFO, "Phase 1) Connection: " + socketChannel.getRemoteAddress() + " registered with id: " + socketData1.getId());

            newKey.attach(socketData1);
            this.serverManager.sendLogMessage(Level.INFO, "Phase 2) Asking: " + socketData1.getId() + " for connection account");

            socketData1.write("needMDP");
            newKey.interestOps(SelectionKey.OP_READ);
        }catch (IOException ignored){

        }

    }

    public void readableKey(ClientConnection socketData, SelectionKey key){

        this.serverManager.sendLogMessage(Level.INFO, "Reading) Reading data on Socket: " + socketData.getId());
        String text = socketData.read();
        if (text == null)
            return;

        if (socketData.getLinkState() == LinkState.NOT_LOGGED) {
            this.serverManager.sendLogMessage(Level.INFO, "Phase 2) Received auth from: " + socketData.getId());
            logIn(socketData, text);
            return;
        }

        DataTransfer dataTransfer = socketData.getFirstDataSender();
        if (dataTransfer != null) {
            this.serverManager.sendLogMessage(Level.INFO, "Phase 4) Socket: " + socketData.getId() + "Already has dataTransfer, broadcasting text to it");
            dataTransfer.messageReceived(text);
            if (dataTransfer.canClose()) {
                this.serverManager.sendLogMessage(Level.WARNING, "Phase 4) Socket: " + socketData.getId() + ": Closing connection due to dataTransfer finish: CURRENTLY_EXECUTING_READ_SERVER");
                socketData.removeFirstDataSender();
                dataTransfer.setSocketState(SocketState.FINISH_OKAY);
                key.interestOps(SelectionKey.OP_READ);
            }
            return;
        }

        this.serverManager.sendLogMessage(Level.INFO, "Phase 3) Checking if text is dataTransferApproval on socket: " + socketData.getId());
        String[] dataList = text.split(this.serverManager.getSocketSplit());
        if (!(dataList.length == 2 && dataList[0].equals("dataTransferApproval"))) {
            this.serverManager.sendLogMessage(Level.SEVERE, "Phase 3) Text on socket: " + socketData.getId() + " is not an dataTransferApproval");
            return;
        }

        this.serverManager.sendLogMessage(Level.FINE, "Phase 3) DataTransferApproval found on: " + socketData.getId());
        String title = dataList[1];
        dataTransfer = this.serverManager.getSocketReceiver(socketData, title);

        if (dataTransfer != null) {
            this.serverManager.sendLogMessage(Level.FINE, "Phase 3) DataTransferApproval approval okay on: " + socketData.getId());
            socketData.getDataTransferList().add(dataTransfer);
            dataTransfer.setSocketState(SocketState.CURRENTLY_EXECUTING);
            socketData.write("dataTransferOkay");
            return;
        }

        this.serverManager.sendLogMessage(Level.SEVERE, "Phase 3) DataTransferApproval not found on: " + socketData.getId());
        socketData.write("dataTransferWrong");

    }

    public void writableKey(ClientConnection socketData, SelectionKey key){
        this.serverManager.sendLogMessage(Level.INFO, "Write) Checking for dataTransfer linked on: " + socketData.getId());
        DataTransfer dataTransfer = socketData.getFirstDataSender();
        if (dataTransfer == null)
            return;
        this.serverManager.sendLogMessage(Level.INFO, "Phase 4) Broadcasting write possibility on: " + socketData.getId());
        dataTransfer.socketWritable();
        if (!dataTransfer.canClose()) {
            return;
        }
        this.serverManager.sendLogMessage(Level.WARNING, "Phase 4) Socket: " + socketData.getId() + ": Closing connection due to dataTransfer finish: CURRENTLY_EXECUTING_WRITE_SERVER");
        socketData.removeFirstDataSender();
        dataTransfer.setSocketState(SocketState.FINISH_OKAY);
        key.interestOps(SelectionKey.OP_READ);
    }

}
