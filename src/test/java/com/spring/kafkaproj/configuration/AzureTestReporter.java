public class AzureTestReporter implements ConcurrentEventListener {
    private final String organization = "your-org";
    private final String project = "your-project";
    private final String pat = "your-pat";
     private Integer runId;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        System.out.println("AzureTestReporter: Publisher set, initializing test run...");
        runId = createTestRun();
        System.out.println("AzureTestReporter: Test run created with ID: " + runId);
        publisher.registerHandlerFor(TestCaseFinished.class, this::handleTestCaseFinished);
    }

    private Integer createTestRun() {
        try {
            String url = String.format("https://dev.azure.com/%s/%s/_apis/test/runs?api-version=6.0",
                organization, project);
            System.out.println("AzureTestReporter: Creating test run at URL: " + url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((":" + pat).getBytes()));
            headers.setContentType(MediaType.APPLICATION_JSON);

            String runName = "Cucumber Test Run " + System.currentTimeMillis();
            System.out.println("AzureTestReporter: Creating run with name: " + runName);

            Map<String, Object> testRun = new HashMap<>();
            testRun.put("name", runName);
            testRun.put("isAutomated", true);
            testRun.put("state", "InProgress");
            testRun.put("type", "NoConfigRun");
            
            Map<String, Object> runInfo = new HashMap<>();
            runInfo.put("testRun", testRun);

            System.out.println("AzureTestReporter: Request body for test run: " + runInfo);
            
            var request = new HttpEntity<>(runInfo, headers);
            var response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            System.out.println("AzureTestReporter: Test run creation response: " + response.getBody());
            
            Integer newRunId = ((Number) response.getBody().get("id")).intValue();
            System.out.println("AzureTestReporter: Successfully created test run with ID: " + newRunId);
            return newRunId;
        } catch (Exception e) {
            System.err.println("AzureTestReporter: Error creating test run: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void handleTestCaseFinished(TestCaseFinished event) {
        try {
            System.out.println("\nAzureTestReporter: Processing finished test case: " + event.getTestCase().getName());
            System.out.println("AzureTestReporter: Test case tags: " + event.getTestCase().getTags());
            
            String testCaseId = event.getTestCase().getTags().stream()
                .filter(tag -> tag.toString().startsWith("@TC"))
                .findFirst()
                .map(tag -> tag.toString().substring(3))
                .orElse(null);

            System.out.println("AzureTestReporter: Extracted Test Case ID: " + testCaseId);
            
            if (testCaseId != null && runId != null) {
                System.out.println("AzureTestReporter: Updating test result for TC" + testCaseId);
                updateTestResult(testCaseId, event.getResult().getStatus().toString(), event.getTestCase().getName());
            } else {
                System.out.println("AzureTestReporter: Skipping result update - testCaseId: " + testCaseId + ", runId: " + runId);
            }
        } catch (Exception e) {
            System.err.println("AzureTestReporter: Error in handleTestCaseFinished: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateTestResult(String testCaseId, String status, String testName) {
        try {
            String url = String.format("https://dev.azure.com/%s/%s/_apis/test/runs/%d/results?api-version=6.0",
                organization, project, runId);
            System.out.println("AzureTestReporter: Updating test result at URL: " + url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((":" + pat).getBytes()));
            headers.setContentType(MediaType.APPLICATION_JSON);

            var testResult = new HashMap<String, Object>();
            testResult.put("testCaseId", Integer.parseInt(testCaseId));
            testResult.put("testCaseTitle", testName);
            testResult.put("automatedTestName", testName);
            testResult.put("outcome", status.equals("PASSED") ? "Passed" : "Failed");
            testResult.put("state", "Completed");

            System.out.println("AzureTestReporter: Test result request body: " + testResult);

            var request = new HttpEntity<>(List.of(testResult), headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            System.out.println("AzureTestReporter: Response status: " + response.getStatusCode());
            System.out.println("AzureTestReporter: Response body: " + response.getBody());
        } catch (Exception e) {
            System.err.println("AzureTestReporter: Error in updateTestResult: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
