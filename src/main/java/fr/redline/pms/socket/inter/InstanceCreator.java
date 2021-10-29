package fr.redline.pms.socket.inter;

import fr.redline.pms.socket.manager.ClientManager;
import fr.redline.pms.socket.connection.ClientConnection;

public interface InstanceCreator {
    DataTransfer getInstance(ClientManager paramClientManager, Class<? extends DataTransfer> paramClass, ClientConnection paramSocketData);
}
