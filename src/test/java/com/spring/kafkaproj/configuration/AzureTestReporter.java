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
        String url = String.format("https://dev.azure.com/%s/%s/_apis/test/runs?api-version=7.1", 
            organization, project);
            
        // Create test run
        Map<String, Object> runRequest = new HashMap<>();
        runRequest.put("name", "Automated Test Run");
        runRequest.put("state", "InProgress");
        Map<String, Object> plan = new HashMap<>();
        plan.put("id", testPlanId);
        runRequest.put("plan", plan);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((":" + pat).getBytes()));
        
        HttpEntity<Map<String, Object>> runReq = new HttpEntity<>(runRequest, headers);
        ResponseEntity<Map> runResponse = restTemplate.exchange(url, HttpMethod.POST, runReq, Map.class);
        Integer runId = (Integer) runResponse.getBody().get("id");

        // Update test result
        String resultUrl = String.format("https://dev.azure.com/%s/%s/_apis/test/Runs/%d/results?api-version=7.1", 
            organization, project, runId);

        Map<String, Object> result = new HashMap<>();
        result.put("outcome", outcome);
        result.put("state", "Completed");
        result.put("testCase", Map.of("id", testCaseId));
        result.put("comment", comment);

        HttpEntity<List<Map<String, Object>>> resultReq = new HttpEntity<>(Collections.singletonList(result), headers);
        restTemplate.exchange(resultUrl, HttpMethod.POST, resultReq, Object.class);

        // Complete the run
        String completeUrl = String.format("https://dev.azure.com/%s/%s/_apis/test/runs/%d?api-version=7.1", 
            organization, project, runId);
        Map<String, Object> completeRequest = new HashMap<>();
        completeRequest.put("state", "Completed");
        HttpEntity<Map<String, Object>> completeReq = new HttpEntity<>(completeRequest, headers);
        restTemplate.exchange(completeUrl, HttpMethod.PATCH, completeReq, Object.class);
    }
