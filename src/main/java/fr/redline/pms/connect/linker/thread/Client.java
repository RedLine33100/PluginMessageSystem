package fr.redline.pms.connect.linker.thread;

import com.google.common.hash.Hashing;
import fr.redline.pms.connect.linker.SocketGestion;
import fr.redline.pms.connect.linker.inter.DataTransfer;
import fr.redline.pms.connect.linker.inter.SocketState;
import fr.redline.pms.connect.linker.thread.connection.ServerConnection;
import fr.redline.pms.utils.IpInfo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.logging.Level;

public class Client extends SocketGestion {

    private final HashMap<String, ServerConnection> hashMap = new HashMap<>();
    private boolean running = false;
    private Selector clientSelector = null;

    public Client(boolean log) {
        super(log, "<ssplit>", "<pmsplit>", "Client: ");
    }

    public void startSelector() throws IOException {
        if (this.clientSelector != null)
            return;
        sendLogMessage(Level.FINE, "Starting selector");
        this.clientSelector = Selector.open();
    }

    public void stopSelector() {
        if (this.clientSelector == null)
            return;
        sendLogMessage(Level.INFO, "Stopping selector");
        stopListening();
        if (this.clientSelector.isOpen()) {
            for (ServerConnection socketData : this.hashMap.values())
                closeConnection(socketData);
            try {
                this.clientSelector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.clientSelector = null;
    }

    public void stopListening() {
        sendLogMessage(Level.INFO, "Stopping listener");
        this.running = false;
    }

    public void startListening() {
        if (this.running || this.clientSelector == null)
            return;
        Runnable runnable = () -> {
            try {
                sendLogMessage(Level.INFO, "Starting listener");
                this.running = true;
                while (this.running && this.clientSelector != null && this.clientSelector.isOpen()) {
                    sendLogMessage(Level.INFO, "Test");
                    if (this.clientSelector.select() == 0) {
                        sendLogMessage(Level.FINE, "No key, breaking while");
                        break;
                    }
                    for (SelectionKey key : this.clientSelector.selectedKeys()) {

                        ServerConnection socketData = (ServerConnection) key.attachment();
                        if (key.isReadable()) {

                            String received = socketData.read();
                            if (received == null) {
                                sendLogMessage(Level.INFO, socketData.getId() + ": Received: null");
                                continue;
                            }

                            sendLogMessage(Level.FINE, socketData.getId() + ": Received: " + received);
                            socketData.updateLastUse();

                            if (socketData.getLinkState() == LinkState.NOT_LOGGED) {
                                sendLogMessage(Level.INFO, socketData.getId() + ": Treat 1");
                                switch (received) {
                                    case "needMDP": {
                                        sendLogMessage(Level.INFO, socketData.getId() + ": Sending credential");
                                        sendCredential(socketData);
                                        break;
                                    }
                                    case "logOkay": {
                                        sendLogMessage(Level.FINE, socketData.getId() + ": Registration Okay");
                                        socketData.setLinkState(LinkState.LOGGED);
                                        key.interestOps(SelectionKey.OP_WRITE);
                                        break;
                                    }
                                    case "logWrong": {
                                        sendLogMessage(Level.SEVERE, socketData.getId() + ": Credential Wrong");
                                        socketData.setLinkState(LinkState.FAILED);
                                        socketData.closeConnection();
                                        break;
                                    }
                                }

                                continue;
                            }

                            sendLogMessage(Level.INFO, socketData.getId() + ": Treat 2");
                            socketData.updateLastUse();
                            DataTransfer dataTransfer = socketData.getFirstDataSender();
                            if (dataTransfer == null)
                                continue;

                            if (dataTransfer.isSocketState(SocketState.WAIT_APPROVAL_SEND)) {
                                key.interestOps(SelectionKey.OP_WRITE);
                            } else if (dataTransfer.isSocketState(SocketState.WAIT_APPROVAL)) {
                                switch (received) {
                                    case "dataTransferOkay": {
                                        sendLogMessage(Level.INFO, socketData.getId() + ": Data transfer accepted");
                                        dataTransfer.setSocketState(SocketState.CURRENTLY_EXECUTING);
                                        if (!dataTransfer.canClose()) {
                                            key.interestOps(SelectionKey.OP_WRITE);
                                            continue;
                                        }

                                        sendLogMessage(Level.WARNING, socketData.getId() + ": Closing connection due to dataTransfer finish: WAIT_APPROVAL");
                                        dataTransfer.setSocketState(SocketState.FINISH_OKAY);
                                        socketData.removeFirstDataSender();
                                        if (socketData.getFirstDataSender() == null)
                                            closeConnection(socketData);

                                    }
                                    case "dataTransferWrong": {
                                        sendLogMessage(Level.SEVERE, socketData.getId() + ": Data transfer refused, DATATRANSFER_REFUSED_SERVER");
                                        dataTransfer.setSocketState(SocketState.FINISH_ERROR);
                                        socketData.removeFirstDataSender();
                                        if (socketData.getFirstDataSender() == null)
                                            closeConnection(socketData);
                                    }
                                }
                            } else if (dataTransfer.isSocketState(SocketState.CURRENTLY_EXECUTING)) {

                                sendLogMessage(Level.INFO, socketData.getId() + ": Broadcasting message received");
                                dataTransfer.messageReceived(received);
                                if (!dataTransfer.canClose()) {
                                    continue;
                                }

                                sendLogMessage(Level.WARNING, socketData.getId() + ": Closing connection due to dataTransfer finish: CURRENTLY_EXECUTING");
                                dataTransfer.setSocketState(SocketState.FINISH_OKAY);
                                socketData.removeFirstDataSender();
                                if (socketData.getFirstDataSender() == null)
                                    closeConnection(socketData);
                            } else if (dataTransfer.isSocketState(SocketState.FINISH_OKAY) || dataTransfer.isSocketState(SocketState.FINISH_ERROR)) {
                                socketData.removeFirstDataSender();
                                key.interestOps(SelectionKey.OP_WRITE);
                            }

                            continue;
                        }

                        if (key.isWritable()) {
                            DataTransfer dataTransfer = socketData.getFirstDataSender();
                            if (dataTransfer != null && socketData.getLinkState() == LinkState.LOGGED) {
                                if (dataTransfer.isSocketState(SocketState.WAIT_APPROVAL_SEND)) {
                                    sendLogMessage(Level.INFO, socketData.getId() + ": Sending data transfer approval");
                                    dataTransfer.setSocketState(SocketState.WAIT_APPROVAL);
                                    socketData.write("dataTransferApproval" + getSocketSplit() + dataTransfer.getTitle());
                                    key.interestOps(SelectionKey.OP_READ);
                                } else if (dataTransfer.isSocketState(SocketState.CURRENTLY_EXECUTING)) {
                                    sendLogMessage(Level.INFO, socketData.getId() + ": Socket writable to dataTransfer");
                                    dataTransfer.socketWritable();
                                    if (dataTransfer.canClose()) {
                                        sendLogMessage(Level.WARNING, socketData.getId() + ": Closing connection due to dataTransfer finish: CURRENTLY_EXECUTING_WRITE");
                                        dataTransfer.setSocketState(SocketState.FINISH_OKAY);
                                        socketData.removeFirstDataSender();
                                        if (socketData.getFirstDataSender() == null)
                                            closeConnection(socketData);
                                    }
                                }
                            } else {
                                sendLogMessage(Level.INFO, socketData.getId() + ": No Data to transfer");
                            }
                        } else {
                            sendLogMessage(Level.INFO, socketData.getId() + ": Nothing to do");
                        }
                        sendLogMessage(Level.INFO, "Key interest op: " + key.interestOps());
                        sendLogMessage(Level.INFO, "End boucle");

                        continue;
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
                stopSelector();
            }
            stopListening();
        };
        (new Thread(runnable)).start();
    }

    public ServerConnection createConnection(IpInfo ipInfo, String account, String pass) {
        try {

            if (this.hashMap.containsKey(ipInfo.toString()))
                return this.hashMap.get(ipInfo.toString());

            startSelector();

            SocketChannel channelSend = SocketChannel.open();
            channelSend.connect(new InetSocketAddress(ipInfo.getIp(), ipInfo.getPort()));

            ServerConnection socketData = new ServerConnection(this, channelSend.register(this.clientSelector, SelectionKey.OP_CONNECT));
            this.hashMap.put(ipInfo.toString(), socketData);

            sendLogMessage(Level.INFO, socketData.getId() + ": Connecting to: " + ipInfo);
            socketData.getSelectionKey().attach(socketData);
            socketData.setAccount(account);

            String encryptPass = null;
            if (pass != null)
                encryptPass = Hashing.sha256().hashString(pass, StandardCharsets.UTF_8).toString();
            socketData.setPassword(encryptPass);

            if (channelSend.finishConnect())

                startListening();
            return socketData;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void closeConnection(ServerConnection socketData) {
        sendLogMessage(Level.WARNING, socketData.getId() + ": Removing socket data");
        try {
            SocketAddress remoteAddress = socketData.getSocketChannel().getRemoteAddress();
            this.hashMap.remove(remoteAddress.toString());
            socketData.closeConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ServerConnection getConnection(IpInfo ipInfo) {
        return this.hashMap.get(ipInfo.toString());
    }

    public void addDataTransfer(ServerConnection socketData, DataTransfer dataTransfer) {
        socketData.addDataSender(dataTransfer);
    }

    protected void sendCredential(ServerConnection socketData) {
        String toSend = "logCred" + getSocketSplit();
        if (socketData.getAccount() != null) {
            toSend += socketData.getAccount();
            if (socketData.getPassword() != null)
                toSend += socketData.getPassword();
        } else
            toSend = toSend + "null";

        sendLogMessage(Level.INFO, socketData.getId() + ": Sending credential");
        socketData.getSelectionKey().interestOps(SelectionKey.OP_WRITE);
        socketData.write(toSend);
        socketData.getSelectionKey().interestOps(SelectionKey.OP_READ);
        socketData.updateLastUse();
    }
}
