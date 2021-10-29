package fr.redline.pms.socket.listener.server;

public enum ServerState {
    MISSING_BOUND,
    STARTING,
    RUNNING,
    RUNNING_ERROR,
    STOP,
    STOP_ERROR
}
