package fr.redline.pms.connect.pm;

import fr.redline.pms.connect.linker.SocketGestion;
import fr.redline.pms.connect.linker.inter.DataTransfer;
import fr.redline.pms.connect.linker.thread.connection.ClientConnection;

import java.util.logging.Level;

public class PMConnectServer extends DataTransfer {

    boolean end = false;
    ClientConnection socketData;
    SocketGestion socketGestion;

    public PMConnectServer(SocketGestion socketGestion, ClientConnection socketData) {
        super(socketData);
        this.socketData = socketData;
        this.socketGestion = socketGestion;
    }

    public void afterWaitApproval() {
    }

    public void afterApproval() {
        this.socketData.getSelectionKey().interestOps(1);
    }

    public void afterFinishError() {
    }

    public void afterFinishOkay() {
    }

    public String getTitle() {
        return "pm";
    }

    public boolean canClose() {
        return this.end;
    }

    public void messageReceived(String message) {
        this.socketGestion.sendLogMessage(Level.FINE, "PM: Receiving and Sending PM");
        String[] param = message.split(this.socketGestion.getPMSSplit());
        this.socketData.getSelectionKey().interestOps(4);
        if (param.length == 2) {
            PMReceiver pmReceiver = PMManager.getPMReceiver(param[0]);
            if (pmReceiver != null) {
                pmReceiver.socketPluginMessageReceived(param[0], param[1]);
                this.socketData.write("Okay");
            } else {
                this.socketData.write("Wrong");
            }
        } else {
            this.socketData.write("Wrong");
        }
        this.end = true;
    }

    public void socketWritable() {
    }
}
