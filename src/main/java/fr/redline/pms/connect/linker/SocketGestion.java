package fr.redline.pms.connect.linker;

import fr.redline.pms.connect.linker.autostop.AutoStopSyst;
import fr.redline.pms.connect.linker.thread.connection.Connection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

public class SocketGestion {
    private static final HashMap<Integer, Connection> connection = new HashMap<>();
    private final ArrayList<String> forbiddenWord;
    AutoStopSyst autoStop = new AutoStopSyst(this);
    boolean log;
    String logDiff;
    String socketSplit;
    String pmsSplit;

    public SocketGestion(boolean log, String socketSplit, String pmsSplit, String logDiff) {
        this.forbiddenWord = new ArrayList<>();
        this.log = log;
        this.socketSplit = socketSplit;
        addForbiddenWord(this.socketSplit);
        this.pmsSplit = pmsSplit;
        addForbiddenWord(this.pmsSplit);
        this.logDiff = logDiff;
    }

    public static void addConnection(Connection client) {
        if (connection.containsKey(client.getId()))
            return;
        connection.put(client.getId(), client);
    }

    public static void removeConnection(Connection client) {
        if (!connection.containsKey(client.getId()))
            return;
        connection.remove(client.getId());
    }

    public static void closeAllConnection() {
        if (!connection.isEmpty()) {
            HashMap<Integer, Connection> connectionNew = new HashMap<>(connection);
            connectionNew.forEach((id, client) -> client.closeConnection());
        }
    }

    public void startAutoStop() {
        this.autoStop.setDisable(false);
    }

    public void stopAutoStop() {
        this.autoStop.setDisable(true);
    }

    public AutoStopSyst getAutoStop() {
        return this.autoStop;
    }

    public void setAutoStopDelay(Long l) {
        this.autoStop.setMaxTimeDiff(l);
    }

    public void sendLogMessage(Level level, String message) {
        if (this.log)
            if (this.logDiff != null) {
                System.out.println(level.getName() + ") " + this.logDiff + message);
            } else {
                System.out.println(level.getName() + ") " + message);
            }
    }

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
