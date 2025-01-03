package com.test.azure;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class AzureTestReporter {
    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, List<Boolean>> testResults = new HashMap<>();
    private final Map<String, String> scenarioNames = new HashMap<>();
    private String lastProcessedScenario = null;

    @Value("${azure.testplan.pat}")
    private String pat;
    
    @Value("${azure.testplan.organization}")
    private String organization;
    
    @Value("${azure.testplan.project}")
    private String project;
    
    @Value("${azure.testplan.planId}")
    private String testPlanId;

    @Before
    public void beforeScenario(Scenario scenario) {
        String testCaseId = extractTestCaseId(scenario);
        if (testCaseId != null) {
            String scenarioBase = getScenarioBase(scenario.getId());
            if (!scenarioBase.equals(lastProcessedScenario)) {
                lastProcessedScenario = scenarioBase;
                testResults.putIfAbsent(testCaseId, new ArrayList<>());
                scenarioNames.putIfAbsent(testCaseId, scenario.getName().split("Examples:")[0].trim());
            }
        }
    }

    @After
    public void afterScenario(Scenario scenario) {
        String testCaseId = extractTestCaseId(scenario);
        if (testCaseId != null) {
            testResults.get(testCaseId).add(!scenario.isFailed());
            
            String currentBase = getScenarioBase(scenario.getId());
            if (!currentBase.equals(getScenarioBase(getNextScenarioId(scenario)))) {
                boolean allPassed = testResults.get(testCaseId).stream().allMatch(result -> result);
                updateTestResult(testCaseId, allPassed ? "Passed" : "Failed", 
                    scenarioNames.get(testCaseId) + "\nExamples results: " + 
                    testResults.get(testCaseId).toString());
                
                testResults.remove(testCaseId);
                scenarioNames.remove(testCaseId);
            }
        }
    }
    
    private String getScenarioBase(String id) {
        return id.substring(0, id.lastIndexOf(';'));
    }
    
    private String getNextScenarioId(Scenario scenario) {
        return scenario.getUri() + ";" + (scenario.getLine() + 1);
    }
    
    private String extractTestCaseId(Scenario scenario) {
        return scenario.getSourceTagNames().stream()
            .filter(tag -> tag.startsWith("@TC"))
            .findFirst()
            .map(tag -> tag.substring(3))
            .orElse(null);
    }
    
    private void updateTestResult(String testCaseId, String outcome, String comment) {
        String url = String.format("https://dev.azure.com/%s/%s/_apis/test/Results?api-version=7.1", 
            organization, project);
            
        Map<String, Object> testResult = new HashMap<>();
        testResult.put("outcome", outcome);
        testResult.put("testCase", Map.of("id", testCaseId));
        testResult.put("testPlan", Map.of("id", testPlanId));
        testResult.put("comment", comment);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((":" + pat).getBytes()));
        
        HttpEntity<List<Map<String, Object>>> request = 
            new HttpEntity<>(Collections.singletonList(testResult), headers);
        
        restTemplate.exchange(url, HttpMethod.POST, request, Object.class);
    }
}















private void updateTestResult(String testCaseId, String outcome, String comment) {
        String pointsUrl = String.format("https://dev.azure.com/%s/%s/_apis/test/Plans/%s/Suites/%s/points?api-version=7.1",
            organization, project, testPlanId, suiteId);
            
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((":" + pat).getBytes()));

        HttpEntity<Void> pointsRequest = new HttpEntity<>(headers);
        ResponseEntity<Map> pointsResponse = restTemplate.exchange(pointsUrl, HttpMethod.GET, pointsRequest, Map.class);
        List<Map<String, Object>> points = (List<Map<String, Object>>) pointsResponse.getBody().get("value");
        Integer pointId = points.stream()
            .filter(p -> testCaseId.equals(((Map)p.get("testCase")).get("id")))
            .findFirst()
            .map(p -> (Integer)p.get("id"))
            .orElseThrow(() -> new RuntimeException("Test point not found"));

        // Create run
        String runUrl = String.format("https://dev.azure.com/%s/%s/_apis/test/runs?api-version=7.1", 
            organization, project);

        Map<String, Object> runData = new HashMap<>();
        runData.put("name", comment);
        runData.put("plan", Map.of("id", testPlanId));
        runData.put("state", "InProgress");
        
        ResponseEntity<Map> runResponse = restTemplate.exchange(runUrl, HttpMethod.POST, new HttpEntity<>(runData, headers), Map.class);
        Integer runId = (Integer) runResponse.getBody().get("id");

        // Add result
        String resultUrl = String.format("https://dev.azure.com/%s/%s/_apis/test/runs/%d/results?api-version=7.1", 
            organization, project, runId);
            
        Map<String, Object> result = new HashMap<>();
        result.put("testPoint", Map.of("id", pointId));
        result.put("testCase", Map.of("id", testCaseId));
        result.put("testCaseRevision", 1);
        result.put("testCaseTitle", comment);
        result.put("outcome", outcome);
        result.put("state", "Completed");
        
        HttpEntity<List<Map<String, Object>>> resultRequest = new HttpEntity<>(Collections.singletonList(result), headers);
        restTemplate.exchange(resultUrl, HttpMethod.POST, resultRequest, Map.class);

        // Complete run
        String completeUrl = String.format("https://dev.azure.com/%s/%s/_apis/test/runs/%d?api-version=7.1", 
            organization, project, runId);
        Map<String, Object> completeData = new HashMap<>();
        completeData.put("state", "Completed");
        headers.setContentType(MediaType.valueOf("application/json-patch+json"));
        HttpEntity<Map<String, Object>> completeRequest = new HttpEntity<>(completeData, headers);
        restTemplate.exchange(completeUrl, HttpMethod.PATCH, completeRequest, Map.class);
    }





















































private String extractTestCaseId(Scenario scenario) {
    Path featurePath = Paths.get("src/test/resources/", 
        scenario.getUri().toString().replace("classpath:", ""));
    
       // Get example index from scenario ID
    int exampleIndex = Integer.parseInt(scenario.getId().split(";")[1]);
    
    try {
        List<String> lines = Files.readAllLines(featurePath);
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("| testId |")) {
                String dataLine = lines.get(i + exampleIndex).trim();
                return Arrays.stream(dataLine.split("\\|"))
                    .map(String::trim)
                    .filter(cell -> !cell.isEmpty())
                    .reduce((first, second) -> second)
                    .orElse(null);
            }
        }
    } catch (IOException e) {
        throw new RuntimeException("Failed to read feature file", e);
    }
    return null;
}
