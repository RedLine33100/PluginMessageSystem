package fr.redline.pms.pm;

import fr.redline.pms.socket.connection.ServerConnectionData;
import fr.redline.pms.socket.inter.DataTransfer;
import fr.redline.pms.socket.manager.ClientManager;

import java.util.logging.Level;

public class PMConnectClient extends DataTransfer {
    boolean send = false;

    boolean end = false;

    boolean reussite = false;

    String message;

    ServerConnectionData socketData;

    ClientManager clientManager;

    public PMConnectClient(ClientManager clientManager, ServerConnectionData socketData, String message) {
        super(socketData);
        this.socketData = socketData;
        this.clientManager = clientManager;
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
            this.clientManager.sendLogMessage(Level.INFO, "PM: Receiving PM");
            this.end = true;
            this.reussite = message.equals("Okay");
        }
    }

    public void socketWritable() {
        if (!this.send) {
            this.clientManager.sendLogMessage(Level.INFO, "PM: Sending PM");
            this.socketData.write(this.message);
            this.send = true;
            this.socketData.getSelectionKey().interestOps(1);
        }
    }
}
