package fr.redline.pms.socket.manager;

import fr.redline.pms.socket.connection.Connection;
import fr.redline.pms.utils.CredentialClass;
import fr.redline.pms.utils.GSONSaver;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

public class ClientManager {

    public ClientManager(boolean log, String socketSplit, String pmsSplit, String logDiff) {
        setLog(log);
        setSocketSplit(socketSplit);
        addForbiddenWord(socketSplit);
        setPMSSplit(pmsSplit);
        addForbiddenWord(pmsSplit);
        setLogDiff(logDiff);
    }


    private static final HashMap<Integer, Connection> connection = new HashMap<>();

    public static void addConnection(Connection client) {
        if (connection.containsKey(client.getId()))
            return;
        connection.put(client.getId(), client);
    }

    public static void removeConnection(Connection client) {
        if (!connection.containsKey(client.getId()))
            return;
        client.closeConnection();
        connection.remove(client.getId());
    }

    public static void closeAllConnection() {
        if (!connection.isEmpty()) {
            HashMap<Integer, Connection> connectionNew = new HashMap<>(connection);
            connectionNew.forEach((id, client) -> client.closeConnection());
        }
    }

    /*
        Credential Class
     */

    private CredentialClass credentialClass = new CredentialClass();

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

    /*
        AutoStop System
     */

    AutoStopSyst autoStop = new AutoStopSyst(this);

    public AutoStopSyst getAutoStop() {
        return this.autoStop;
    }

    /*
        Log
     */

    boolean log;
    String logDiff;

    public void setLogDiff(String log){
        this.logDiff = log;
    }

    public String getLogDiff(){
        return logDiff;
    }

    public void setLog(boolean log){
        this.log = log;
    }

    public boolean getLogState(){
        return log;
    }

    public void sendLogMessage(Level level, String message) {
        if (this.getLogState())
            if (this.logDiff != null) {
                System.out.println(level.getName() + ") " + this.logDiff + message);
            } else {
                System.out.println(level.getName() + ") " + message);
            }
    }

    /*
        Splitter Gestion
     */

    String socketSplit, pmsSplit;

    public String getSocketSplit() {
        return this.socketSplit;
    }

    public void setSocketSplit(String s) {
        removeForbiddenWord(this.socketSplit);
        this.socketSplit = s;
        addForbiddenWord(this.socketSplit);
    }

    public String getPMSSplit() {
        return this.pmsSplit;
    }

    public void setPMSSplit(String s) {
        removeForbiddenWord(this.pmsSplit);
        this.pmsSplit = s;
        addForbiddenWord(this.pmsSplit);
    }

    /*
        Forbidden Words
     */

    private final ArrayList<String> forbiddenWord = new ArrayList<>();

    public boolean containsForbiddenWord(String s) {
        return this.forbiddenWord.contains(s);
    }

    public void addForbiddenWord(String s) {
        if (!containsForbiddenWord(s))
            this.forbiddenWord.add(s);
    }

    public void removeForbiddenWord(String s) {
        if (containsForbiddenWord(s))
            this.forbiddenWord.remove(s);
    }
}
