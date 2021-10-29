package fr.redline.pms.socket.inter;

import fr.redline.pms.socket.connection.ConnectionData;
import fr.redline.pms.socket.manager.ClientManager;

public interface InstanceCreator {
    DataTransfer getInstance(ClientManager paramClientManager, Class<? extends DataTransfer> paramClass, ConnectionData paramSocketData);
}
