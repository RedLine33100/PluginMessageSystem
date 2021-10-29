package fr.redline.pms.connect.linker.thread.server;

public enum ServerState {
    MISSING_BOUND,
    STARTING,
    RUNNING,
    RUNNING_ERROR,
    STOP,
    STOP_ERROR
}
