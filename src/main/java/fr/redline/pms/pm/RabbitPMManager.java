package fr.redline.pms.pm;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RabbitPMManager {
    public static boolean sendRabbitMQJavaClient(Channel channel, String queue, String message) {
        try {
            channel.queueDeclare(queue, false, false, false, null);
            channel.basicPublish("", queue, null, message.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public static boolean addRabbitMQListener(Channel channel, String queue, PMReceiver pmReceiver) {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> pmReceiver.rabbitPluginMessageReceived(consumerTag, queue, new String(delivery.getBody(), StandardCharsets.UTF_8));
        try {
            channel.basicConsume(queue, true, deliverCallback, consumerTag -> {
            });
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
