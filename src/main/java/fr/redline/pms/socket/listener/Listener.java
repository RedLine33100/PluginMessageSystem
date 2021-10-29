package fr.redline.pms.socket.listener;

import fr.redline.pms.socket.connection.ConnectionData;
import fr.redline.pms.socket.connection.LinkState;
import fr.redline.pms.socket.manager.ClientManager;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

public abstract class Listener {

    Selector selector = null;
    ListenerType listenerType;
    ClientManager clientManager;
    Timer timer = new Timer();

    public Listener(ClientManager clientManager, ListenerType listenerType){
        this.clientManager = clientManager;
        this.listenerType = listenerType;
    }

    public ClientManager getClientManager(){
        return clientManager;
    }

    public ListenerType getListenerType(){
        return listenerType;
    }

    public abstract ServerSocketChannel getServerSocketChannel();

    protected boolean logIn(ConnectionData socketData, String text) {
        if (socketData.getLinkState() == LinkState.LOGGED) {
            getClientManager().sendLogMessage(Level.SEVERE, "Phase 2) Error on connecting " + socketData.getId() + " Reason: Socket already connected");
            return true;
        }
        if (text == null) {
            getClientManager().sendLogMessage(Level.SEVERE, "Phase 2) Error on connecting " + socketData.getId() + " Reason: Login text null");
            return false;
        }
        String[] logInInfo = text.split(getClientManager().getSocketSplit());
        boolean authorized = false;
        if (!logInInfo[0].equals("logCred")) {
            getClientManager().sendLogMessage(Level.SEVERE, "Phase 2) Error on connecting " + socketData.getId() + " Reason: Text not recognize as login text");
            return false;
        }
        getClientManager().sendLogMessage(Level.INFO, "Phase 2) Authing socket " + socketData.getId() + ", text recognize as login text");
        if (logInInfo.length == 2) {
            getClientManager().sendLogMessage(Level.INFO, "Phase 2) Authing socket " + socketData.getId() + ", text recognize as login text with 3 args");
            String account = null;
            if (!logInInfo[1].equals("null"))
                account = logInInfo[1];
            authorized = getClientManager().getCredentialClass().isAccount(account, null);
        } else if (logInInfo.length == 3) {
            getClientManager().sendLogMessage(Level.INFO, "Phase 2) Authing socket " + socketData.getId() + ", text recognize as login text with 4 args");
            authorized = getClientManager().getCredentialClass().isAccount(logInInfo[1], logInInfo[2]);
        }
        socketData.getSelectionKey().interestOps(SelectionKey.OP_WRITE);

        socketData.setLinkState(authorized ? LinkState.LOGGED : LinkState.NOT_LOGGED);
        if (authorized) {
            getClientManager().sendLogMessage(Level.FINE, "Phase 2) Authing socket " + socketData.getId() + ", Socket logged");
            socketData.updateLastUse();
            socketData.write("logOkay");
        } else {
            getClientManager().sendLogMessage(Level.SEVERE, "Phase 2) Authing socket " + socketData.getId() + ", Socket logging failed");
            socketData.write("logWrong");
            socketData.closeConnection();
        }
        socketData.getSelectionKey().interestOps(1);
        socketData.updateLastUse();
        return authorized;
    }

    protected void sendCredential(ConnectionData socketData, String password) {
        String toSend = "logCred" + clientManager.getSocketSplit();
        if (socketData.getAccount() != null) {
            toSend += socketData.getAccount();
            if (password != null)
                toSend += password;
        } else
            toSend = toSend + "null";

        clientManager.sendLogMessage(Level.INFO, socketData.getId() + ": Sending credential");
        socketData.getSelectionKey().interestOps(SelectionKey.OP_WRITE);
        socketData.write(toSend);
        socketData.getSelectionKey().interestOps(SelectionKey.OP_READ);
        socketData.updateLastUse();
    }

    protected void startSelector() throws IOException {
        if(selector == null)
        this.selector = Selector.open();
    }

    protected void closeSelector() throws IOException {
        if(this.selector != null){
            this.selector.close();
            this.selector = null;
        }
    }

    protected Selector getSelector(){
        return this.selector;
    }

    public abstract void onReadable(ConnectionData clientConnectionData, SelectionKey selectionKey);

    public abstract void onWritable(ConnectionData clientConnectionData, SelectionKey selectionKey);

    public abstract void onAcceptable();

    protected void startListener(){

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {

                try {
                    getSelector().select();

                    for(SelectionKey selectionKey : getSelector().selectedKeys()){
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

        timer.scheduleAtFixedRate(timerTask, 500, 500);

    }

    protected void stopListener(){

        timer.cancel();
        timer.purge();

    }

}
