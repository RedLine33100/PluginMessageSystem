package fr.redline.pms.socket.listener.sub;

import fr.redline.pms.socket.connection.ConnectionData;
import fr.redline.pms.socket.credential.Credential;
import fr.redline.pms.socket.listener.client.ClientManager;
import fr.redline.pms.utils.IpInfo;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.*;
import java.util.logging.Level;

public abstract class Listener {

    static HashMap<ListenerType, List<Listener>> listenerHashMap = new HashMap<>();

    public Listener(ClientManager clientManager, ListenerType listenerType) {
        this.clientManager = clientManager;
        this.listenerType = listenerType;
        this.add();
    }

    Selector selector = null;
    AbstractSelectableChannel socketChannel = null;
    ListenerType listenerType;
    ClientManager clientManager;
    IpInfo ipInfo = null;
    Timer timer = new Timer();

    public Listener getListener(ListenerType listenerType, IpInfo ipInfo) {

        if (!listenerHashMap.containsKey(listenerType))
            return null;

        for (Listener listener : listenerHashMap.get(listenerType)) {
            if (listener.getIpInfo() == ipInfo)
                return listener;
        }

        return null;

    }

    private void add() {
        Listener listener = this;
        if (listenerHashMap.containsKey(listenerType)) {
            listenerHashMap.get(listenerType).add(listener);
        } else listenerHashMap.put(listenerType, new ArrayList<Listener>() {{
            add(listener);
        }});
    }

    public ClientManager getClientManager() {
        return clientManager;
    }

    public ListenerType getListenerType() {
        return listenerType;
    }

    public IpInfo getIpInfo() {
        return ipInfo;
    }

    public void setIpInfo(IpInfo ipInfo) {
        this.ipInfo = ipInfo;
    }

    protected Credential logIn(ConnectionData socketData, String text) {
        if (socketData.getCredential() != null || text == null) {
            getClientManager().sendLogMessage(Level.SEVERE, "Phase 2) Error on connecting " + socketData.getId() + " Reason: Socket already connected");
            return socketData.getCredential();
        }

        String[] logInInfo = text.split(getClientManager().getSocketSplit());
        if (!logInInfo[0].equals("logCred") || logInInfo.length != 3) {
            getClientManager().sendLogMessage(Level.SEVERE, "Phase 2) Error on connecting " + socketData.getId() + " Reason: Text not recognize as login text");
            return null;
        }

        getClientManager().sendLogMessage(Level.INFO, "Phase 2) Authing socket " + socketData.getId() + ", text recognize as login text");
        Credential credential = clientManager.getCredentialClass().getCredential(logInInfo[1], logInInfo[2]);
        socketData.write(credential != null ? "logOkay" : "logWrong");

        if (credential == null) {
            getClientManager().sendLogMessage(Level.SEVERE, "Phase 2) Authing socket " + socketData.getId() + ", Socket logging failed");
            socketData.closeConnection();
            return null;
        }

        getClientManager().sendLogMessage(Level.FINE, "Phase 2) Authing socket " + socketData.getId() + ", Socket logged");
        socketData.setCredential(credential);

        socketData.getSelectionKey().interestOps(SelectionKey.OP_READ);
        return credential;
    }

    protected void sendCredential(ConnectionData socketData) {
        Credential credential = socketData.getCredential();
        clientManager.sendLogMessage(Level.INFO, socketData.getId() + ": Sending credential");
        socketData.write("logCred" + clientManager.getSocketSplit() + credential.getAccount() + clientManager.getSocketSplit() + credential.getPassword());
    }

    protected void startConnection() throws IOException {
        if (selector == null)
            this.selector = Selector.open();
        if (this.socketChannel == null)
            if (getListenerType() == ListenerType.CLIENT)
                this.socketChannel = SocketChannel.open();
            else this.socketChannel = ServerSocketChannel.open();
    }

    protected void stopConnection() {
        stopListener();
        if (this.selector != null) {
            try {
                this.selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.selector = null;
        }
        if (this.socketChannel != null) {
            try {
                this.socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.socketChannel = null;
        }
    }

    protected Selector getSelector() {
        return this.selector;
    }

    protected AbstractSelectableChannel getSocketChannel() {
        return this.socketChannel;
    }

    public abstract void onReadable(ConnectionData clientConnectionData, SelectionKey selectionKey);

    public abstract void onWritable(ConnectionData clientConnectionData, SelectionKey selectionKey);

    public abstract void onAcceptable();

    public abstract void notifyConnectionStop(ConnectionData connectionData);

    public abstract void stop();

    protected void startListener() {

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {

                try {
                    getSelector().select();

                    for (SelectionKey selectionKey : getSelector().selectedKeys()) {
                        if (getListenerType() == ListenerType.SERVER && selectionKey.isAcceptable()) {
                            onAcceptable();
                            continue;
                        }

                        ConnectionData socketData = (ConnectionData) selectionKey.attachment();
                        if (selectionKey.isReadable()) {
                            onReadable(socketData, selectionKey);
                        }else if (selectionKey.isWritable()) {
                            onWritable(socketData, selectionKey);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };

        timer.scheduleAtFixedRate(timerTask, 0, 500);

    }

    protected void stopListener(){

        timer.cancel();
        timer.purge();

    }

}
