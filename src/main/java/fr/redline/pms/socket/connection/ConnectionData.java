package fr.redline.pms.socket.connection;

import fr.redline.pms.socket.inter.DataTransfer;
import fr.redline.pms.socket.inter.SocketState;
import fr.redline.pms.socket.listener.Listener;
import fr.redline.pms.socket.manager.ClientManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public abstract class ConnectionData {

    private final ClientManager clientManager;
    private final int id;
    private final ByteBuffer byteBuffer = ByteBuffer.allocate(256);
    private final List<DataTransfer> dataTransferList = new ArrayList<>();
    private Object attachment = null;
    private String account = null;
    private LinkState linkState = LinkState.NOT_LOGGED;
    private final Listener listener;
    private long lastDataMillis = System.currentTimeMillis();

    public ConnectionData(ClientManager clientManager, Listener listener) {
        this.clientManager = clientManager;
        this.id = clientManager.getAutoStop().registerSocketData(this);
        clientManager.addConnection(this);
        this.listener = listener;
    }

    public int getId() {
        return id;
    }

    public abstract SelectionKey getSelectionKey();

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public LinkState getLinkState() {
        return this.linkState;
    }

    public Listener getListener() {
        return listener;
    }

    public void setLinkState(LinkState linkState) {
        this.linkState = linkState;
    }

    public void updateLastUse() {
        this.lastDataMillis = System.currentTimeMillis();
    }

    public long getLastDataMillis() {
        return this.lastDataMillis;
    }

    public Object getAttachment() {
        return attachment;
    }

    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    public ClientManager getSocketGestion() {
        return clientManager;
    }

    public String getAccount() {
        return this.account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public abstract SocketChannel getSocketChannel();

    public DataTransfer getFirstDataSender() {
        if (this.dataTransferList.isEmpty())
            return null;
        return this.dataTransferList.get(0);
    }

    public DataTransfer removeFirstDataSender() {
        if (this.dataTransferList.isEmpty())
            return null;
        return this.dataTransferList.remove(0);
    }

    public List<DataTransfer> getDataTransferList() {
        return dataTransferList;
    }

    public void closeConnection() {
        if (!isSocketConnected())
            return;
        getSocketGestion().sendLogMessage(Level.WARNING, "Closing socketData");
        getSelectionKey().cancel();
        try {
            getSocketChannel().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        getByteBuffer().clear();
        for (DataTransfer dataTransfer : getDataTransferList()) {
            if (!dataTransfer.isSocketState(SocketState.FINISH_OKAY) && !dataTransfer.isSocketState(SocketState.FINISH_ERROR))
                dataTransfer.setSocketState(SocketState.FINISH_ERROR);
        }
        this.getDataTransferList().clear();
        clientManager.removeConnection(this);
        this.getListener().notifyConnectionStop(this);
    }

    public boolean isSocketConnected() {
        return this.getSocketChannel().isOpen();
    }

    public boolean write(String textToWrite) {

        if (!isSocketConnected()) {
            this.clientManager.sendLogMessage(Level.SEVERE, "Text cannot be write: " + textToWrite + " on: " + getId());
            return false;
        }

        getSelectionKey().interestOps(SelectionKey.OP_WRITE);
        this.clientManager.sendLogMessage(Level.INFO, "Writing on: " + getId() + " text: " + textToWrite);
        try {
            ByteBuffer buffer = getByteBuffer();
            buffer.clear();
            buffer.put(textToWrite.getBytes());
            buffer.flip();
            while (buffer.hasRemaining()) {
                int bytesWritten = getSocketChannel().write(buffer);
                assert bytesWritten > 0;
            }
            buffer.clear();
            this.clientManager.sendLogMessage(Level.INFO, "Text: " + textToWrite + " writed on socket: " + getId());
            return true;
        } catch (IOException e) {
            this.clientManager.sendLogMessage(Level.SEVERE, "Failed to write: " + textToWrite + " on: " + getId());
            e.printStackTrace();
            return false;
        }

    }

    public String read() {
        if (isSocketConnected()) {
            this.clientManager.sendLogMessage(Level.INFO, "Trying to read on: " + getId());
            try {
                getSelectionKey().interestOps(SelectionKey.OP_READ);
                ByteBuffer buffer = getByteBuffer();
                buffer.clear();
                int readBytes = getSocketChannel().read(buffer);
                if (readBytes <= 0)
                    return null;
                buffer.flip();
                String text = new String(buffer.array()).trim();
                this.clientManager.sendLogMessage(Level.FINE, "Message: " + text + " readed on: " + getId());
                return text;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else
            this.clientManager.sendLogMessage(Level.SEVERE, "Message cannot be read on: " + getId() + " Socket not connected");
        return null;
    }

}