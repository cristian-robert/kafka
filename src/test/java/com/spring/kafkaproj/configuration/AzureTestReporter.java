
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
        try {
            String url = String.format("https://dev.azure.com/%s/%s/_apis/test/runs?api-version=6.0",
                organization, project);
            
            System.out.println("Calling Azure DevOps URL: " + url);  // Debug log

            HttpHeaders headers = createHeaders();
            System.out.println("Using Authorization header: " + headers.getFirst(HttpHeaders.AUTHORIZATION));  // Debug log
            
            HashMap<String, Object> requestBody = new HashMap<>();
            requestBody.put("name", "Automated Test Run " + System.currentTimeMillis());
            requestBody.put("isAutomated", true);
            requestBody.put("state", "InProgress");
            requestBody.put("type", "NoConfigRun");

            var request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> rawResponse = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class  // First get raw response to debug
            );
            
            System.out.println("Raw Response: " + rawResponse.getBody());  // Debug log
            System.out.println("Response Status: " + rawResponse.getStatusCode());  // Debug log
            System.out.println("Response Headers: " + rawResponse.getHeaders());  // Debug log

            // If we get here successfully, try parsing as JSON
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
        } catch (Exception e) {
            System.err.println("Error creating test run: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
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
        
        // Format for Personal Access Token authentication
        String encodedPat = Base64.getEncoder().encodeToString((":" + pat).getBytes());
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encodedPat);
        
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));  // Explicitly request JSON response
        
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
