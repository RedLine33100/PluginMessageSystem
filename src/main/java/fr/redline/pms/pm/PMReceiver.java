package fr.redline.pms.pm;

public interface PMReceiver {
    void socketPluginMessageReceived(String paramString, String message);

    void redisPluginMessageReceived(String paramString, Object paramObject);

    void rabbitPluginMessageReceived(String consumerTag, String queue, String message);
}