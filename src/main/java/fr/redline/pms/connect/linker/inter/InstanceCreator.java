package fr.redline.pms.connect.linker.inter;

import fr.redline.pms.connect.linker.SocketGestion;
import fr.redline.pms.connect.linker.thread.connection.ClientConnection;

public interface InstanceCreator {
    DataTransfer getInstance(SocketGestion paramSocketGestion, Class<? extends DataTransfer> paramClass, ClientConnection paramSocketData);
}
