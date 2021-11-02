package fr.redline.pms.socket.manager;

import fr.redline.pms.socket.connection.ConnectionData;
import fr.redline.pms.socket.inter.DataTransfer;
import fr.redline.pms.socket.inter.InstanceCreator;
import fr.redline.pms.socket.listener.server.Server;

import java.util.HashMap;

public class ServerManager extends ClientManager {

    private final HashMap<String, Memorize> socketReceiverHashMap = new HashMap<>();
    private final Server serverSocketReceiver;

    public ServerManager(boolean log) {
        super(log, "<ssplit>", "<pmsplit>", "Server: ");
        this.serverSocketReceiver = new Server(this);
    }

    public Server getServer(){
        return serverSocketReceiver;
    }

    public boolean hasSocketReceiver(String title) {
        return this.socketReceiverHashMap.containsKey(title);
    }

    public DataTransfer getSocketReceiver(ConnectionData socketData, String title) {
        if (!hasSocketReceiver(title))
            return null;
        Memorize memorize = this.socketReceiverHashMap.get(title);
        return memorize.getInstanceCreator().getInstance(this, memorize.getDataTransferClass(), socketData);
    }

    public boolean setSocketReceiver(String title, InstanceCreator instanceCreator, Class<? extends DataTransfer> dr) {
        removeSocketReceiver(title);
        this.socketReceiverHashMap.put(title, new Memorize(instanceCreator, dr));
        return true;
    }

    public void removeSocketReceiver(String title) {
        this.socketReceiverHashMap.remove(title);
    }

    private static class Memorize {
        InstanceCreator instanceCreator;

        Class<? extends DataTransfer> dataTransferClass;

        Memorize(InstanceCreator instanceCreator, Class<? extends DataTransfer> dataTransferClass) {
            this.instanceCreator = instanceCreator;
            this.dataTransferClass = dataTransferClass;
        }

        InstanceCreator getInstanceCreator() {
            return this.instanceCreator;
        }

        Class<? extends DataTransfer> getDataTransferClass() {
            return this.dataTransferClass;
        }
    }
}
