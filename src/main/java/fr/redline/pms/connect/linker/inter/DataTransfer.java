package fr.redline.pms.connect.linker.inter;

import fr.redline.pms.connect.linker.thread.connection.Connection;

public abstract class DataTransfer {
    private final Connection socketData;
    private SocketState socketState = SocketState.WAIT_APPROVAL_SEND;

    public DataTransfer(Connection socketData) {
        this.socketData = socketData;
    }

    public SocketState getSocketState() {
        return this.socketState;
    }

    public void setSocketState(SocketState socketState) {
        this.socketState = socketState;
        switch (socketState) {
            case CURRENTLY_EXECUTING:
                afterApproval();
                break;
            case FINISH_ERROR:
                afterFinishError();
                break;
            case FINISH_OKAY:
                afterFinishOkay();
                break;
            case WAIT_APPROVAL:
                afterWaitApproval();
                break;
        }
    }

    public boolean isSocketState(SocketState socketState) {
        return getSocketState().getName().equals(socketState.getName());
    }

    public Connection getConnection() {
        return this.socketData;
    }

    public abstract void afterWaitApproval();

    public abstract void afterApproval();

    public abstract void afterFinishError();

    public abstract void afterFinishOkay();

    public abstract String getTitle();

    public abstract boolean canClose();

    public abstract void messageReceived(String paramString);

    public abstract void socketWritable();
}
