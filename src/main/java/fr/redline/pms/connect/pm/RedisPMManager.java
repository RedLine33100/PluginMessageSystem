package fr.redline.pms.connect.pm;

import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

public class RedisPMManager {
    public static <T> void addRedissonPMListener(RedissonClient redissonClient, String title, Class<T> objectClass, PMReceiver pmReceiver) {
        RTopic topic = redissonClient.getTopic(title);
        topic.addListener(objectClass, (channel, msg) -> pmReceiver.redisPluginMessageReceived(title, msg));
    }

    public static long sendRedissonPluginMessage(RedissonClient redissonClient, String title, Object object) {
        System.out.println("srpm: " + title);
        long l = redissonClient.getTopic(title).publish(object);
        System.out.println("returned long: " + l);
        return l;
    }
}
