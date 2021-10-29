package fr.redline.pms.connect.linker.inter;

public enum SocketState {
    WAIT_APPROVAL_SEND("WAIT_APPROVAL_SEND"),
    WAIT_APPROVAL("WAIT_APPROVAL"),
    CURRENTLY_EXECUTING("CURRENTLY_EXECUTING"),
    FINISH_OKAY("FINISH_OKAY"),
    FINISH_ERROR("FINISH_ERROR");

    String name;

    SocketState(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
