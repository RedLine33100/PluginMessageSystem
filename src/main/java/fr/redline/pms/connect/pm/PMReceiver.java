package fr.redline.pms.connect.pm;

public interface PMReceiver {
    void socketPluginMessageReceived(String paramString, String message);

    void redisPluginMessageReceived(String paramString, Object paramObject);

    void rabbitPluginMessageReceived(String consumerTag, String queue, String message);
}
