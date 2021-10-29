package fr.redline.pms.connect.linker.thread.server;

import fr.redline.pms.connect.linker.ReceiverManager;
import fr.redline.pms.connect.linker.inter.DataTransfer;
import fr.redline.pms.connect.linker.inter.SocketState;
import fr.redline.pms.connect.linker.thread.LinkState;
import fr.redline.pms.connect.linker.thread.connection.ClientConnection;
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
    ReceiverManager receiverManager;

    IpInfo ipInfo = null;

    /// Socket Needed
    ServerSocketChannel server = null;
    Selector serverSelector = null;
    SelectionKey serverKey = null;

    ServerState serverState = ServerState.MISSING_BOUND;
    Timer timer = new Timer();

    public Server(ReceiverManager receiverManager) {
        this.receiverManager = receiverManager;
    }

    public boolean startServer() {

        this.receiverManager.sendLogMessage(Level.INFO, "Starting server");
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
                    SocketChannel socketChannel = this.server.accept();
                    if (socketChannel == null) {
                        this.receiverManager.sendLogMessage(Level.INFO, "Phase 1) No connection accepted");
                        continue;
                    }

                    this.receiverManager.sendLogMessage(Level.FINE, "Phase 1) accepted connection from " + socketChannel.getRemoteAddress());
                    socketChannel.configureBlocking(false);

                    SelectionKey newKey = socketChannel.register(this.serverSelector, SelectionKey.OP_READ);
                    ClientConnection socketData1 = new ClientConnection(this.receiverManager, newKey);
                    this.receiverManager.sendLogMessage(Level.INFO, "Phase 1) Connection: " + socketChannel.getRemoteAddress() + " registered with id: " + socketData1.getId());

                    newKey.attach(socketData1);
                    this.receiverManager.sendLogMessage(Level.INFO, "Phase 2) Asking: " + socketData1.getId() + " for connection account");

                    socketData1.write("needMDP");
                    newKey.interestOps(SelectionKey.OP_READ);
                    continue;
                }

                ClientConnection socketData = (ClientConnection) key.attachment();
                if (socketData == null) {
                    key.channel().close();
                    this.receiverManager.sendLogMessage(Level.SEVERE, "Wrong) SocketData not found on key attachement");
                    continue;
                }

                this.receiverManager.sendLogMessage(Level.INFO, "Check) Socket: " + socketData.getId() + ", Server key state: " + key.interestOps());
                if (key.isReadable()) {

                    this.receiverManager.sendLogMessage(Level.INFO, "Reading) Reading data on Socket: " + socketData.getId());
                    String text = socketData.read();
                    if (text == null)
                        continue;

                    if (socketData.getLinkState() == LinkState.NOT_LOGGED) {
                        this.receiverManager.sendLogMessage(Level.INFO, "Phase 2) Received auth from: " + socketData.getId());
                        logIn(socketData, text);
                        continue;
                    }

                    DataTransfer dataTransfer = socketData.getFirstDataSender();
                    if (dataTransfer != null) {
                        this.receiverManager.sendLogMessage(Level.INFO, "Phase 4) Socket: " + socketData.getId() + "Already has dataTransfer, broadcasting text to it");
                        dataTransfer.messageReceived(text);
                        if (dataTransfer.canClose()) {
                            this.receiverManager.sendLogMessage(Level.WARNING, "Phase 4) Socket: " + socketData.getId() + ": Closing connection due to dataTransfer finish: CURRENTLY_EXECUTING_READ_SERVER");
                            socketData.removeFirstDataSender();
                            dataTransfer.setSocketState(SocketState.FINISH_OKAY);
                            key.interestOps(SelectionKey.OP_READ);
                        }
                        continue;
                    }

                    this.receiverManager.sendLogMessage(Level.INFO, "Phase 3) Checking if text is dataTransferApproval on socket: " + socketData.getId());
                    String[] dataList = text.split(this.receiverManager.getSocketSplit());
                    if (!(dataList.length == 2 && dataList[0].equals("dataTransferApproval"))) {
                        this.receiverManager.sendLogMessage(Level.SEVERE, "Phase 3) Text on socket: " + socketData.getId() + " is not an dataTransferApproval");
                        continue;
                    }

                    this.receiverManager.sendLogMessage(Level.FINE, "Phase 3) DataTransferApproval found on: " + socketData.getId());
                    String title = dataList[1];
                    dataTransfer = this.receiverManager.getSocketReceiver(socketData, title);

                    if (dataTransfer != null) {
                        this.receiverManager.sendLogMessage(Level.FINE, "Phase 3) DataTransferApproval approval okay on: " + socketData.getId());
                        socketData.getDataTransferList().add(dataTransfer);
                        dataTransfer.setSocketState(SocketState.CURRENTLY_EXECUTING);
                        socketData.write("dataTransferOkay");
                        continue;
                    }

                    this.receiverManager.sendLogMessage(Level.SEVERE, "Phase 3) DataTransferApproval not found on: " + socketData.getId());
                    socketData.write("dataTransferWrong");
                    continue;

                }

                if (key.isWritable() && socketData.getLinkState() == LinkState.LOGGED) {
                    this.receiverManager.sendLogMessage(Level.INFO, "Write) Checking for dataTransfer linked on: " + socketData.getId());
                    DataTransfer dataTransfer = socketData.getFirstDataSender();
                    if (dataTransfer != null) {
                        this.receiverManager.sendLogMessage(Level.INFO, "Phase 4) Broadcasting write possibility on: " + socketData.getId());
                        dataTransfer.socketWritable();
                        if (dataTransfer.canClose()) {
                            this.receiverManager.sendLogMessage(Level.WARNING, "Phase 4) Socket: " + socketData.getId() + ": Closing connection due to dataTransfer finish: CURRENTLY_EXECUTING_WRITE_SERVER");
                            socketData.removeFirstDataSender();
                            dataTransfer.setSocketState(SocketState.FINISH_OKAY);
                            key.interestOps(SelectionKey.OP_READ);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected boolean logIn(ClientConnection socketData, String text) {
        if (socketData.getLinkState() == LinkState.LOGGED) {
            this.receiverManager.sendLogMessage(Level.SEVERE, "Phase 2) Error on connecting " + socketData.getId() + " Reason: Socket already connected");
            return true;
        }
        if (text == null) {
            this.receiverManager.sendLogMessage(Level.SEVERE, "Phase 2) Error on connecting " + socketData.getId() + " Reason: Login text null");
            return false;
        }
        String[] logInInfo = text.split(this.receiverManager.getSocketSplit());
        boolean authorized = false;
        if (!logInInfo[0].equals("logCred")) {
            this.receiverManager.sendLogMessage(Level.SEVERE, "Phase 2) Error on connecting " + socketData.getId() + " Reason: Text not recognize as login text");
            return false;
        }
        this.receiverManager.sendLogMessage(Level.INFO, "Phase 2) Authing socket " + socketData.getId() + ", text recognize as login text");
        if (logInInfo.length == 2) {
            this.receiverManager.sendLogMessage(Level.INFO, "Phase 2) Authing socket " + socketData.getId() + ", text recognize as login text with 3 args");
            String account = null;
            if (!logInInfo[1].equals("null"))
                account = logInInfo[1];
            authorized = this.receiverManager.getCredentialClass().isAccount(account, null);
        } else if (logInInfo.length == 3) {
            this.receiverManager.sendLogMessage(Level.INFO, "Phase 2) Authing socket " + socketData.getId() + ", text recognize as login text with 4 args");
            authorized = this.receiverManager.getCredentialClass().isAccount(logInInfo[1], logInInfo[2]);
        }
        socketData.getSelectionKey().interestOps(SelectionKey.OP_WRITE);

        socketData.setLinkState(authorized ? LinkState.LOGGED : LinkState.NOT_LOGGED);
        if (authorized) {
            this.receiverManager.sendLogMessage(Level.FINE, "Phase 2) Authing socket " + socketData.getId() + ", Socket logged");
            socketData.updateLastUse();
            socketData.write("logOkay");
        } else {
            this.receiverManager.sendLogMessage(Level.SEVERE, "Phase 2) Authing socket " + socketData.getId() + ", Socket logging failed");
            socketData.write("logWrong");
            socketData.closeConnection();
        }
        socketData.getSelectionKey().interestOps(1);
        socketData.updateLastUse();
        return authorized;
    }

    public void stopServer() {
        this.receiverManager.sendLogMessage(Level.SEVERE, "Stopping serveur");

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

}
