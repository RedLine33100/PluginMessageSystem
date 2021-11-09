package fr.redline.pms.pm;

public interface PMReceiver {
    default void socketPluginMessageReceived(String paramString, String message) {

    }

    default void redisPluginMessageReceived(String paramString, Object paramObject) {

    }

    default void rabbitPluginMessageReceived(String consumerTag, String queue, String message) {

    }
}