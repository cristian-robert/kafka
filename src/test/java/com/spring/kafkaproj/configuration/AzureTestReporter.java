public class AzureTestReporter {
    private final String organization = "your-org";
    private final String project = "your-project";
    private final String pat = "your-pat";
     private Integer runId;
  
    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, List<Boolean>> testResults = new HashMap<>();
    private final Map<String, String> scenarioNames = new HashMap<>();
    private final Set<String> processedTestCases = new HashSet<>();

    @Before
    public void beforeScenario(Scenario scenario) {
        String testCaseId = extractTestCaseId(scenario);
        if (testCaseId != null) {
            processedTestCases.add(getScenarioKey(scenario));
            testResults.putIfAbsent(testCaseId, new ArrayList<>());
            scenarioNames.putIfAbsent(testCaseId, scenario.getName().split("Examples:")[0].trim());
        }
    }

    @After
    public void afterScenario(Scenario scenario) {
        String testCaseId = extractTestCaseId(scenario);
        if (testCaseId != null) {
            testResults.get(testCaseId).add(!scenario.isFailed());
            String currentKey = getScenarioKey(scenario);
            
            // If we've moved to a new scenario or it's the last scenario
            if (shouldUpdateResult(scenario, currentKey)) {
                boolean allPassed = testResults.get(testCaseId).stream().allMatch(result -> result);
                updateTestResult(testCaseId, allPassed ? "Passed" : "Failed", 
                    scenarioNames.get(testCaseId) + "\nExamples results: " + 
                    testResults.get(testCaseId).toString());
                
                testResults.remove(testCaseId);
                scenarioNames.remove(testCaseId);
            }
        }
    }
    
    private boolean shouldUpdateResult(Scenario scenario, String currentKey) {
        return !processedTestCases.contains(getNextScenarioKey(scenario));
    }
    
    private String getScenarioKey(Scenario scenario) {
        return scenario.getId();
    }
    
    private String getNextScenarioKey(Scenario scenario) {
        // Get the scenario ID and increment the last number
        String currentId = scenario.getId();
        int lastDotIndex = currentId.lastIndexOf('.');
        int lastNumber = Integer.parseInt(currentId.substring(lastDotIndex + 1));
        return currentId.substring(0, lastDotIndex + 1) + (lastNumber + 1);
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
            ORG, PROJECT);
            
        Map<String, Object> testResult = new HashMap<>();
        testResult.put("outcome", outcome);
        testResult.put("testCase", Map.of("id", testCaseId));
        testResult.put("testPlan", Map.of("id", TEST_PLAN_ID));
        testResult.put("comment", comment);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((":" + PAT).getBytes()));
        
        HttpEntity<List<Map<String, Object>>> request = 
            new HttpEntity<>(Collections.singletonList(testResult), headers);
        
        restTemplate.exchange(url, HttpMethod.POST, request, Object.class);
    }
}
