public class AzureTestReporter implements ConcurrentEventListener {
    private final String organization = "your-org";
    private final String project = "your-project";
    private final String pat = "your-pat";
      private Integer runId;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestRunStarted.class, this::handleTestRunStarted);
        publisher.registerHandlerFor(TestCaseFinished.class, this::handleTestCaseFinished);
    }

    private void handleTestRunStarted(TestRunStarted event) {
        try {
            String url = String.format("https://dev.azure.com/%s/%s/_apis/test/runs?api-version=6.0",
                organization, project);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((":" + pat).getBytes()));
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> runInfo = new HashMap<>();
            runInfo.put("name", "Automated Test Run " + System.currentTimeMillis());
            runInfo.put("isAutomated", true);
            runInfo.put("state", "InProgress");
            runInfo.put("results", new ArrayList<>());  // Empty results array required

            var request = new HttpEntity<>(runInfo, headers);
            var response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            runId = ((Number) response.getBody().get("id")).intValue();
            System.out.println("Created test run with ID: " + runId);
        } catch (Exception e) {
            System.err.println("Error creating test run: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleTestCaseFinished(TestCaseFinished event) {
        var pickle = event.getTestCase().getPickle();
        String testCaseId = pickle.getTags().stream()
            .filter(tag -> tag.startsWith("@TC"))
            .findFirst()
            .map(tag -> tag.substring(3))
            .orElse(null);

        if (testCaseId != null && runId != null) {
            updateTestResult(testCaseId, event.getResult().getStatus().toString());
        }
    }

    private void updateTestResult(String testCaseId, String status) {
        try {
            String url = String.format("https://dev.azure.com/%s/%s/_apis/test/runs/%d/results?api-version=6.0",
                organization, project, runId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((":" + pat).getBytes()));
            headers.setContentType(MediaType.APPLICATION_JSON);

            var testResult = Map.of(
                "testCaseId", Integer.parseInt(testCaseId),
                "outcome", status.equals("PASSED") ? "Passed" : "Failed"
            );

            var request = new HttpEntity<>(List.of(testResult), headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            System.out.println("Response: " + response.getStatusCode() + " - " + response.getBody());
        } catch (Exception e) {
            System.err.println("Error updating test result: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
