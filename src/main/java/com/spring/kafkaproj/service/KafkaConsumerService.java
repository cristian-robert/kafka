package com.spring.kafkaproj.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final ConsumerFactory<String, String> consumerFactory;

    public boolean consumeMessage(String topic, Predicate<String> messageFilter, long timeoutSeconds) {
        CountDownLatch latch = new CountDownLatch(1);

        ContainerProperties containerProps = new ContainerProperties(topic);
        containerProps.setMessageListener((MessageListener<String, String>) record -> {
            if (messageFilter.test(record.value())) {
                latch.countDown();
            }
        });

        KafkaMessageListenerContainer<String, String> container =
                new KafkaMessageListenerContainer<>(consumerFactory, containerProps);

        container.start();

        try {
            return latch.await(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            container.stop();
        }
    }
}