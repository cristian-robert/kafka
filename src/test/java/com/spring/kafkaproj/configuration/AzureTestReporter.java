@Component
@ConfigurationProperties(prefix = "azure")
public class AzureTestReporter implements ConcurrentEventListener {
    private String organization;
    private String project;
    private String pat;
    private Integer currentTestRunId;
    private final RestTemplate restTemplate;

    public AzureTestReporter() {
        this.restTemplate = new RestTemplate();
    }

    // Response class for Test Run
    public static class TestRunResponse {
        @JsonProperty("id")
        private Integer id;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }
    }

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestRunStarted.class, this::handleTestRunStarted);
        publisher.registerHandlerFor(TestCaseFinished.class, this::handleTestCaseFinished);
    }

    private void handleTestRunStarted(TestRunStarted event) {
        this.currentTestRunId = createTestRun();
    }

    private void handleTestCaseFinished(TestCaseFinished event) {
        getAzureTestCaseId(event.getTestCase())
            .ifPresent(testCaseId -> updateTestResult(
                testCaseId,
                mapStatus(event.getResult().getStatus()),
                event.getResult().getError()
            ));
    }

    private Optional<Integer> getAzureTestCaseId(TestCase testCase) {
        return testCase.getTags().stream()
            .filter(tag -> tag.toString().startsWith("@TC"))
            .findFirst()
            .map(tag -> Integer.parseInt(tag.toString().substring(3)));
    }

    private Integer createTestRun() {
        String url = String.format("https://dev.azure.com/%s/%s/_apis/test/runs?api-version=6.0",
            organization, project);

        HttpHeaders headers = createHeaders();
        
        HashMap<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "Automated Test Run " + System.currentTimeMillis());
        requestBody.put("isAutomated", true);

        var request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<TestRunResponse> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            request,
            TestRunResponse.class
        );
        
        if (response.getBody() == null) {
            throw new RuntimeException("Failed to create test run - no response body");
        }
        
        return response.getBody().getId();
    }

    private void updateTestResult(Integer testCaseId, String outcome, Throwable error) {
        String url = String.format("https://dev.azure.com/%s/%s/_apis/test/runs/%d/results?api-version=6.0",
            organization, project, currentTestRunId);

        HttpHeaders headers = createHeaders();
        
        List<HashMap<String, Object>> testResults = new ArrayList<>();
        HashMap<String, Object> testResult = new HashMap<>();
        testResult.put("testCaseId", testCaseId);
        testResult.put("outcome", outcome);
        testResult.put("errorMessage", error != null ? error.getMessage() : null);
        testResults.add(testResult);

        var request = new HttpEntity<>(testResults, headers);
        restTemplate.exchange(url, HttpMethod.PATCH, request, Void.class);
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String auth = ":" + pat;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + new String(encodedAuth));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String mapStatus(Status status) {
        return switch (status) {
            case PASSED -> "Passed";
            case FAILED -> "Failed";
            case SKIPPED -> "NotExecuted";
            default -> "InProgress";
        };
    }

    // Getters and setters for properties
    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public void setPat(String pat) {
        this.pat = pat;
    }
}
