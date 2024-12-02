public class AzureTestReporter implements ConcurrentEventListener {
    private final String organization = "your-org";
    private final String project = "your-project";
    private final String pat = "your-pat";
    private Integer runId = 1; // Temporary hardcoded value for testing
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        System.out.println("Publisher set");
        publisher.registerHandlerFor(TestCaseFinished.class, this::handleTestCaseFinished);
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
            if (testCaseId != null) {
                updateTestResult(testCaseId, event.getResult().getStatus().toString());
            }
        } catch (Exception e) {
            System.err.println("Error in handleTestCaseFinished: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateTestResult(String testCaseId, String status) {
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

            System.out.println("Request body: " + testResult);

            var request = new HttpEntity<>(testResult, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.PATCH, 
                request, 
                String.class
            );

            System.out.println("Response status: " + response.getStatusCode());
            System.out.println("Response body: " + response.getBody());
        } catch (Exception e) {
            System.err.println("Error in updateTestResult: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
