package fr.redline.pms.pm;

import fr.redline.pms.socket.manager.ClientManager;
import fr.redline.pms.socket.inter.DataTransfer;
import fr.redline.pms.socket.inter.InstanceCreator;
import fr.redline.pms.socket.connection.ClientConnection;

public class PMInstanceCreator implements InstanceCreator {
    public DataTransfer getInstance(ClientManager clientManager, Class<? extends DataTransfer> dataTransferClass, ClientConnection socketData) {
        return new PMConnectServer(clientManager, socketData);
    }
}
