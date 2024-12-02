public class AzureTestReporter implements ConcurrentEventListener {
    private final String organization = "your-org";
    private final String project = "your-project";
    private final String pat = "your-pat";
 private Integer runId;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        System.out.println("Publisher set");
        runId = createTestRun();
        publisher.registerHandlerFor(TestCaseFinished.class, this::handleTestCaseFinished);
    }

    private Integer createTestRun() {
        try {
            String url = String.format("https://dev.azure.com/%s/%s/_apis/test/runs?api-version=6.0",
                organization, project);
            System.out.println("Creating test run at URL: " + url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((":" + pat).getBytes()));
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> runInfo = new HashMap<>();
            runInfo.put("name", "Automated Test Run " + System.currentTimeMillis());
            runInfo.put("isAutomated", true);
            runInfo.put("state", "InProgress");
            runInfo.put("type", "NoConfigRun");
            runInfo.put("automatedTestName", "CucumberTest");

            var request = new HttpEntity<>(runInfo, headers);
            var response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            Integer newRunId = ((Number) response.getBody().get("id")).intValue();
            System.out.println("Created test run with ID: " + newRunId);
            return newRunId;
        } catch (Exception e) {
            System.err.println("Error creating test run: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void handleTestCaseFinished(TestCaseFinished event) {
        try {
            System.out.println("Test case finished: " + event.getTestCase().getName());
            System.out.println("Tags: " + event.getTestCase().getTags());
            
            String testCaseId = event.getTestCase().getTags().stream()
                .filter(tag -> tag.toString().startsWith("@TC"))
                .findFirst()
                .map(tag -> tag.toString().substring(3))
                .orElse(null);

            System.out.println("Found Test Case ID: " + testCaseId);
            if (testCaseId != null && runId != null) {
                updateTestResult(testCaseId, event.getResult().getStatus().toString(), event.getTestCase().getName());
            }
        } catch (Exception e) {
            System.err.println("Error in handleTestCaseFinished: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateTestResult(String testCaseId, String status, String testName) {
        try {
            String url = String.format("https://dev.azure.com/%s/%s/_apis/test/runs/%d/results?api-version=6.0",
                organization, project, runId);
            System.out.println("Calling Azure URL: " + url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((":" + pat).getBytes()));
            headers.setContentType(MediaType.APPLICATION_JSON);

            var testResult = new HashMap<String, Object>();
            testResult.put("testCaseId", Integer.parseInt(testCaseId));
            testResult.put("outcome", status.equals("PASSED") ? "Passed" : "Failed");
            testResult.put("automatedTestName", testName);

            System.out.println("Request body: " + testResult);

            var request = new HttpEntity<>(List.of(testResult), headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            System.out.println("Response status: " + response.getStatusCode());
            System.out.println("Response body: " + response.getBody());
        } catch (Exception e) {
            System.err.println("Error in updateTestResult: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
