package fr.redline.pms.pm;

import fr.redline.pms.socket.listener.Client;
import fr.redline.pms.socket.connection.ServerConnection;

import java.util.HashMap;

public class PMManager {

    private static final HashMap<String, PMReceiver> pluginMessageReceiver = new HashMap<>();

    public static boolean hasPMReceiver(String title) {
        return pluginMessageReceiver.containsKey(title);
    }

    public static PMReceiver getPMReceiver(String title) {
        return pluginMessageReceiver.get(title);
    }

    public static void setPMReceiver(String title, PMReceiver run) {
        System.out.println("spr: " + title);
        removePMReceiver(title);
        pluginMessageReceiver.put(title, run);
    }

    public static void removePMReceiver(String title) {
        pluginMessageReceiver.remove(title);
    }

    public static void sendPluginMessage(Client client, ServerConnection socketData, String title, String message) {
        System.out.println("spm: " + title);
        if (client.containsForbiddenWord(title))
            return;
        if (client.containsForbiddenWord(message))
            return;
        PMConnectClient pmConnectClient = new PMConnectClient(client, socketData, title + client.getPMSSplit() + message);
        client.addDataTransfer(socketData, pmConnectClient);
    }
}
