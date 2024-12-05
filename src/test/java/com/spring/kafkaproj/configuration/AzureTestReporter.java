public class AzureTestReporter {
    private final String organization = "your-org";
    private final String project = "your-project";
    private final String pat = "your-pat";
     private Integer runId;
    private final RestTemplate restTemplate = new RestTemplate();

 
    @After
    public void afterScenario(Scenario scenario) {
        String testCaseId = extractTestCaseId(scenario);
        if (testCaseId != null) {
            updateTestResult(testCaseId, scenario.isFailed() ? "Failed" : "Passed", scenario.getName());
        }
    }
    
    private String extractTestCaseId(Scenario scenario) {
        return scenario.getSourceTagNames().stream()
            .filter(tag -> tag.startsWith("@TC"))
            .findFirst()
            .map(tag -> tag.substring(3))
            .orElse(null);
    }
    
    private void updateTestResult(String testCaseId, String outcome, String scenarioName) {
        String url = String.format("https://dev.azure.com/%s/%s/_apis/test/Results?api-version=7.1", 
            ORG, PROJECT);
            
        Map<String, Object> testResult = new HashMap<>();
        testResult.put("outcome", outcome);
        testResult.put("testCase", Map.of("id", testCaseId));
        testResult.put("testPlan", Map.of("id", TEST_PLAN_ID));
        testResult.put("comment", scenarioName);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((":" + PAT).getBytes()));
        
        HttpEntity<List<Map<String, Object>>> request = 
            new HttpEntity<>(Collections.singletonList(testResult), headers);
        
        restTemplate.exchange(url, HttpMethod.POST, request, Object.class);
    }
}
