package fr.redline.pms.socket.manager;

import fr.redline.pms.socket.connection.ConnectionData;

import java.util.ArrayList;
import java.util.logging.Level;

public class AutoStopSyst extends Thread {

    private final ClientManager clientManager;
    private final ArrayList<ConnectionData> autoStopData = new ArrayList<>();
    private boolean running = false;
    private boolean disable = true;
    private int unique = 0;
    private long milliDiff = 5000L;

    public AutoStopSyst(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    public int getUnique() {
        return ++this.unique;
    }

    public void setAutoStopDelay(long l) {
        this.milliDiff = l;
    }

    public void setDisable(boolean d) {
        this.disable = d;
    }

    public int registerSocketData(ConnectionData socketData) {
        this.autoStopData.add(socketData);
        if (!this.running && !disable)
            this.start();
        return getUnique();
    }

    @Override
    public void run() {
        this.running = true;
        while (!this.disable && !this.autoStopData.isEmpty()) {
            long currentTimeMillis = System.currentTimeMillis();
            for (int i = 0; i < this.autoStopData.size(); i++) {
                ConnectionData socketData = this.autoStopData.get(i);
                if (socketData.isSocketConnected()) {
                    long diff = currentTimeMillis - socketData.getLastDataMillis();
                    if (diff >= this.milliDiff || diff <= this.milliDiff * -1L) {
                        i--;
                        this.autoStopData.remove(socketData);
                        socketData.closeConnection();
                        this.clientManager.sendLogMessage(Level.SEVERE, "Socket id: " + socketData.getId() + " stopped due to no responding");
                    }
                } else this.autoStopData.remove(socketData);
            }
        }
        this.running = false;
    }

}