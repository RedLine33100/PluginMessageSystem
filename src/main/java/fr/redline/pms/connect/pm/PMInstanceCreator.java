package fr.redline.pms.connect.pm;

import fr.redline.pms.connect.linker.SocketGestion;
import fr.redline.pms.connect.linker.inter.DataTransfer;
import fr.redline.pms.connect.linker.inter.InstanceCreator;
import fr.redline.pms.connect.linker.thread.connection.ClientConnection;

public class PMInstanceCreator implements InstanceCreator {
    public DataTransfer getInstance(SocketGestion socketGestion, Class<? extends DataTransfer> dataTransferClass, ClientConnection socketData) {
        return new PMConnectServer(socketGestion, socketData);
    }
}
