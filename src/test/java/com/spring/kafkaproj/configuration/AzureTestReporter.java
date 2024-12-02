

public class AzureTestReporter implements ConcurrentEventListener {
    private final String organization = "your-org";
    private final String project = "your-project";
    private final String pat = "your-pat";
    private Integer runId;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestCaseFinished.class, this::handleTestCaseFinished);
    }

    private void handleTestCaseFinished(TestCaseFinished event) {
        String testCaseId = event.getTestCase().getTags().stream()
            .filter(tag -> tag.toString().startsWith("@TC"))
            .findFirst()
            .map(tag -> tag.toString().substring(3))
            .orElse(null);

        if (testCaseId != null) {
            updateTestResult(testCaseId, event.getResult().getStatus().toString());
        }
    }

    private void updateTestResult(String testCaseId, String status) {
        String url = String.format("https://dev.azure.com/%s/%s/_apis/test/runs/%d/results?api-version=6.0",
            organization, project, runId);

        var headers = new org.springframework.http.HttpHeaders();
        headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((":" + pat).getBytes()));
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        var testResult = new HashMap<String, Object>();
        testResult.put("testCaseId", Integer.parseInt(testCaseId));
        testResult.put("outcome", status.equals("PASSED") ? "Passed" : "Failed");

        var request = new org.springframework.http.HttpEntity<>(testResult, headers);
        restTemplate.exchange(url, org.springframework.http.HttpMethod.PATCH, request, Void.class);
    }
}
