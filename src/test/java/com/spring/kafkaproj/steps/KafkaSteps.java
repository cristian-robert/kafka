package com.spring.kafkaproj.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.spring.kafkaproj.service.KafkaConsumerService;
import com.spring.kafkaproj.service.KafkaProducerService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.FileCopyUtils;
import org.testng.Assert;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class KafkaSteps {

    private final KafkaProducerService producerService;
    private final KafkaConsumerService consumerService;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("${test.kafka.timeout-seconds:90}")
    private long timeoutSeconds;

    @Given("I send {string} to {string} kafka topic")
    public void sendJsonToKafkaTopic(String jsonFile, String topic, io.cucumber.datatable.DataTable dataTable) throws Exception {
        // Load JSON from payload directory
        String jsonString = loadJsonFromPayloadFile(jsonFile);
        log.info("Loaded JSON template: {}", jsonString);

        Map<String, String> modifications = dataTable.asMap(String.class, String.class);
        for (Map.Entry<String, String> entry : modifications.entrySet()) {
            jsonString = updateJsonValue(jsonString, entry.getKey(), entry.getValue());
        }

        log.info("Sending modified JSON: {}", jsonString);
        producerService.sendMessage(topic, jsonString);
    }

    private String loadJsonFromPayloadFile(String jsonFile) throws Exception {
        String filePath = jsonFile.startsWith("/") ? jsonFile : "/payload/" + jsonFile;
        Resource resource = resourceLoader.getResource("classpath:" + filePath);

        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            String content = FileCopyUtils.copyToString(reader);
            // Validate that it's a valid JSON
            objectMapper.readTree(content);
            return content;
        } catch (Exception e) {
            log.error("Error loading JSON file {}: {}", filePath, e.getMessage());
            throw new RuntimeException("Error loading JSON payload: " + filePath, e);
        }
    }

    @Then("I should find matching message in {string} topic")
    public void findMatchingMessageInTopic(String topic, io.cucumber.datatable.DataTable dataTable) {
        Map<String, String> expectedValues = dataTable.asMap(String.class, String.class);
        log.info("Searching in topic: {} with expected values: {}", topic, expectedValues);

        boolean found = consumerService.consumeMessage(
                topic,
                message -> matchesExpectedValues(message, expectedValues),
                timeoutSeconds
        );

        if (!found) {
            log.error("Failed to find message matching expected values: {}", expectedValues);
        }

        Assert.assertTrue(found, "Message with expected values not found within timeout");
    }

    private String updateJsonValue(String jsonString, String path, String value) {
        try {
            // Handle null or "null" string values
            if (value == null || value.equalsIgnoreCase("null")) {
                return JsonPath.parse(jsonString)
                        .set(normalizePath(path), null)
                        .jsonString();
            }

            // Handle empty string
            if (value.isEmpty()) {
                return JsonPath.parse(jsonString)
                        .set(normalizePath(path), "")
                        .jsonString();
            }

            // Handle numeric values
            if (value.matches("-?\\d+(\\.\\d+)?")) {
                if (value.contains(".")) {
                    return JsonPath.parse(jsonString)
                            .set(normalizePath(path), Double.parseDouble(value))
                            .jsonString();
                } else {
                    return JsonPath.parse(jsonString)
                            .set(normalizePath(path), Long.parseLong(value))
                            .jsonString();
                }
            }

            // Handle boolean values
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                return JsonPath.parse(jsonString)
                        .set(normalizePath(path), Boolean.parseBoolean(value))
                        .jsonString();
            }

            // Handle special string values that might indicate null
            if (value.equalsIgnoreCase("<null>") || value.equalsIgnoreCase("${null}")) {
                return JsonPath.parse(jsonString)
                        .set(normalizePath(path), null)
                        .jsonString();
            }

            // Default: treat as string
            return JsonPath.parse(jsonString)
                    .set(normalizePath(path), value)
                    .jsonString();

        } catch (PathNotFoundException e) {
            log.error("Path not found: {}", path);
            throw new RuntimeException("Invalid JSON path: " + path, e);
        } catch (Exception e) {
            log.error("Error updating JSON value for path: {}, value: {}", path, value);
            throw new RuntimeException("Error updating JSON value: " + e.getMessage(), e);
        }
    }

    private boolean matchesExpectedValues(String message, Map<String, String> expectedValues) {
        try {
            log.debug("Checking message: {} against expected values: {}", message, expectedValues);

            for (Map.Entry<String, String> expectedValue : expectedValues.entrySet()) {
                String path = expectedValue.getKey();
                String expectedStrValue = expectedValue.getValue();

                Object actualValue = JsonPath.read(message, normalizePath(path));
                if (actualValue == null) {
                    log.debug("Path {} not found in message", path);
                    return false;
                }

                // Handle different data types in comparison
                if (!compareValues(actualValue, expectedStrValue)) {
                    log.debug("Value mismatch at path {}: expected {} but was {}",
                            path, expectedStrValue, actualValue);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Error matching values: {}", e.getMessage());
            return false;
        }
    }

    private boolean compareValues(Object actualValue, String expectedStrValue) {
        // Handle numbers
        if (expectedStrValue.matches("-?\\d+(\\.\\d+)?")) {
            if (expectedStrValue.contains(".")) {
                return Double.parseDouble(actualValue.toString()) == Double.parseDouble(expectedStrValue);
            } else {
                return Long.parseLong(actualValue.toString()) == Long.parseLong(expectedStrValue);
            }
        }
        // Handle booleans
        else if (expectedStrValue.equalsIgnoreCase("true") || expectedStrValue.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(actualValue.toString()) == Boolean.parseBoolean(expectedStrValue);
        }
        // Handle strings
        else {
            return actualValue.toString().equals(expectedStrValue);
        }
    }

    private String normalizePath(String path) {
        return path.startsWith("$") ? path : "$." + path;
    }
}