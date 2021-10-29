package fr.redline.pms.connect.pm;

import fr.redline.pms.connect.linker.SocketGestion;
import fr.redline.pms.connect.linker.inter.DataTransfer;
import fr.redline.pms.connect.linker.thread.connection.ServerConnection;

import java.util.logging.Level;

public class PMConnectClient extends DataTransfer {
    boolean send = false;

    boolean end = false;

    boolean reussite = false;

    String message;

    ServerConnection socketData;

    SocketGestion socketGestion;

    public PMConnectClient(SocketGestion socketGestion, ServerConnection socketData, String message) {
        super(socketData);
        this.socketData = socketData;
        this.socketGestion = socketGestion;
        this.message = message;
    }

    public boolean isReussite() {
        return this.reussite;
    }

    public void afterWaitApproval() {
    }

    public void afterApproval() {
        this.socketData.getSelectionKey().interestOps(4);
    }

    public void afterFinishError() {
        this.end = true;
    }

    public void afterFinishOkay() {
        this.end = true;
    }

    public String getTitle() {
        return "pm";
    }

    public boolean canClose() {
        return this.end;
    }

    public void messageReceived(String message) {
        if (this.send) {
            this.socketGestion.sendLogMessage(Level.INFO, "PM: Receiving PM");
            this.end = true;
            this.reussite = message.equals("Okay");
        }
    }

    public void socketWritable() {
        if (!this.send) {
            this.socketGestion.sendLogMessage(Level.INFO, "PM: Sending PM");
            this.socketData.write(this.message);
            this.send = true;
            this.socketData.getSelectionKey().interestOps(1);
        }
    }
}
