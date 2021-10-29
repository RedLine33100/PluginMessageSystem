package fr.redline.pms.connect.linker;

import fr.redline.pms.connect.linker.inter.DataTransfer;
import fr.redline.pms.connect.linker.inter.InstanceCreator;
import fr.redline.pms.connect.linker.thread.connection.ClientConnection;
import fr.redline.pms.connect.linker.thread.server.Server;
import fr.redline.pms.connect.pm.PMConnectServer;
import fr.redline.pms.connect.pm.PMInstanceCreator;
import fr.redline.pms.utils.CredentialClass;
import fr.redline.pms.utils.GSONSaver;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.HashMap;

public class ReceiverManager extends SocketGestion {

    private final HashMap<String, Memorize> socketReceiverHashMap;
    private final Server serverSocketReceiver;
    private CredentialClass credentialClass;

    public ReceiverManager(boolean log) {
        super(log, "<ssplit>", "<pmsplit>", "Server: ");
        this.serverSocketReceiver = new Server(this);
        this.socketReceiverHashMap = new HashMap<>();
        this.credentialClass = new CredentialClass();
        setSocketReceiver("pm", new PMInstanceCreator(), PMConnectServer.class);
    }

    public Server getServer(){
        return serverSocketReceiver;
    }

    public boolean hasSocketReceiver(String title) {
        return this.socketReceiverHashMap.containsKey(title);
    }

    public DataTransfer getSocketReceiver(ClientConnection socketData, String title) {
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

    public CredentialClass getCredentialClass() {
        return this.credentialClass;
    }

    public void saveCredential(File file) {
        GSONSaver.writeGSON(file, getCredentialClass());
    }

    public boolean loadCredential(File file) {
        CredentialClass credentialClass = GSONSaver.loadGSON(file, CredentialClass.class);
        if (credentialClass != null) {
            this.credentialClass = credentialClass;
            return true;
        }
        return false;
    }

    private boolean checkHasParameter(Class<? extends DataTransfer> javaClass, Integer number) {
        Constructor<?>[] arrayOfConstructor = javaClass.getDeclaredConstructors();
        for (Constructor<?> ctor : arrayOfConstructor)
            if ((ctor.getParameterTypes()).length == number)
                return true;
        return false;
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
