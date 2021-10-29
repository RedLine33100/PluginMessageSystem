package fr.redline.pms.connect.linker.autostop;

import fr.redline.pms.connect.linker.SocketGestion;
import fr.redline.pms.connect.linker.thread.connection.Connection;

import java.util.ArrayList;
import java.util.logging.Level;

public class AutoStopSyst extends Thread {

    private final SocketGestion socketGestion;
    private final ArrayList<Connection> autoStopData = new ArrayList<>();
    private boolean running = false;
    private boolean disable = true;
    private int unique = 0;
    private long milliDiff = 5000L;

    public AutoStopSyst(SocketGestion socketGestion) {
        this.socketGestion = socketGestion;
    }

    public int getUnique() {
        return ++this.unique;
    }

    public void setMaxTimeDiff(long l) {
        this.milliDiff = l;
    }

    public void setDisable(boolean d) {
        this.disable = d;
    }

    public int registerSocketData(Connection socketData) {
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
                Connection socketData = this.autoStopData.get(i);
                if (!socketData.getSocketChannel().socket().isClosed()) {
                    long diff = currentTimeMillis - socketData.getLastDataMillis();
                    if (diff >= this.milliDiff || diff <= this.milliDiff * -1L) {
                        i--;
                        this.autoStopData.remove(socketData);
                        socketData.closeConnection();
                        this.socketGestion.sendLogMessage(Level.SEVERE, "Socket id: " + socketData.getId() + " stopped due to no responding");
                    }
                } else this.autoStopData.remove(socketData);
            }
        }
        this.running = false;
    }

}