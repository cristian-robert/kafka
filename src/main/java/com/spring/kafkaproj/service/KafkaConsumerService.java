package com.spring.kafkaproj.service;

import com.jayway.jsonpath.JsonPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final ConsumerFactory<byte[], byte[]> consumerFactory;
    private final ObjectMapper objectMapper;

    public boolean consumeAndVerifyMessage(String topic, Map<String, String> expectedValues, long timeoutSeconds) {
        CountDownLatch latch = new CountDownLatch(1);

        ContainerProperties containerProps = new ContainerProperties(topic);
        containerProps.setMessageListener((MessageListener<byte[], byte[]>) record -> {
            try {
                String jsonMessage = new String(record.value());
                log.debug("Received message from topic {}: {}", topic, jsonMessage);

                if (matchesExpectedValues(jsonMessage, expectedValues)) {
                    log.info("Found matching message in topic {}", topic);
                    latch.countDown();
                }
            } catch (Exception e) {
                log.error("Error processing message: {}", e.getMessage());
            }
        });

        KafkaMessageListenerContainer<byte[], byte[]> container =
                new KafkaMessageListenerContainer<>(consumerFactory, containerProps);

        container.start();

        try {
            boolean found = latch.await(timeoutSeconds, TimeUnit.SECONDS);
            if (!found) {
                log.warn("Timeout while waiting for matching message in topic {}", topic);
            }
            return found;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            container.stop();
        }
    }

    private boolean matchesExpectedValues(String jsonMessage, Map<String, String> expectedValues) {
        try {
            for (Map.Entry<String, String> entry : expectedValues.entrySet()) {
                String path = entry.getKey();
                String expectedValue = entry.getValue();

                // Handle null values
                if ("null".equalsIgnoreCase(expectedValue) || "<null>".equals(expectedValue)) {
                    Object actualValue = JsonPath.read(jsonMessage, normalizePath(path));
                    if (actualValue != null) {
                        log.debug("Path {} expected null but was {}", path, actualValue);
                        return false;
                    }
                    continue;
                }

                // Handle non-null values
                Object actualValue = JsonPath.read(jsonMessage, normalizePath(path));
                if (actualValue == null || !actualValue.toString().equals(expectedValue)) {
                    log.debug("Value mismatch at path {}: expected {} but was {}",
                            path, expectedValue, actualValue);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Error matching values: {}", e.getMessage());
            return false;
        }
    }

    private String normalizePath(String path) {
        return path.startsWith("$") ? path : "$." + path;
    }
}
