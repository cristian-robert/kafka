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
        publisher.registerHandlerFor(TestRunFinished.class, this::handleTestRunFinished);
    }



     private void handleTestRunFinished(TestRunFinished event) {
        try {
            if (runId != null) {
                // First try to GET the run to verify auth
                String getUrl = String.format("https://dev.azure.com/%s/%s/_apis/test/runs/%d?api-version=6.0",
                    organization, project, runId);
                System.out.println("Testing GET request: " + getUrl);
                
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((":" + pat).getBytes()));
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                var getRequest = new HttpEntity<>(headers);
                var getResponse = restTemplate.exchange(getUrl, HttpMethod.GET, getRequest, String.class);
                System.out.println("GET Response: " + getResponse.getBody());

                // Then try the PUT with more details
                Map<String, Object> runUpdate = new HashMap<>();
                runUpdate.put("name", "Cucumber Test Run " + System.currentTimeMillis());
                runUpdate.put("state", "Completed");
                runUpdate.put("id", runId);
                runUpdate.put("automated", true);
                runUpdate.put("iteration", "1");
                runUpdate.put("comment", "Test run completed by Cucumber");

                var putRequest = new HttpEntity<>(runUpdate, headers);
                var putResponse = restTemplate.exchange(getUrl, HttpMethod.PUT, putRequest, String.class);
                System.out.println("PUT Response: " + putResponse.getBody());
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
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


























     private void handleTestCaseFinished(TestCaseFinished event) {
        try {
            String testCaseId = event.getTestCase().getTags().stream()
                .filter(tag -> tag.toString().startsWith("@TC"))
                .findFirst()
                .map(tag -> tag.toString().substring(3))
                .orElse(null);

            System.out.println("Processing test case: " + testCaseId);
            
            if (testCaseId != null && !completedTests.contains(testCaseId)) {
                String url = String.format("https://dev.azure.com/%s/%s/_apis/test/runs/%d/results?api-version=6.0",
                    organization, project, runId);

                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((":" + pat).getBytes()));
                headers.setContentType(MediaType.APPLICATION_JSON);

                var testResult = new HashMap<String, Object>();
                testResult.put("testCaseId", Integer.parseInt(testCaseId));
                testResult.put("testCaseTitle", event.getTestCase().getName());
                testResult.put("outcome", event.getResult().getStatus().equals(Status.PASSED) ? "Passed" : "Failed");
                testResult.put("state", "Completed");
                testResult.put("comment", "Executed by Cucumber");

                var request = new HttpEntity<>(List.of(testResult), headers);
                restTemplate.exchange(url, HttpMethod.POST, request, String.class);
                
                completedTests.add(testCaseId);
                System.out.println("Updated result for test case: " + testCaseId);
            }
        } catch (Exception e) {
            System.err.println("Error updating test result: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void handleTestRunFinished(TestRunFinished event) {
        try {
            if (runId != null) {
                // First get the test run to get test result IDs
                String getUrl = String.format("https://dev.azure.com/%s/%s/_apis/test/runs/%d?api-version=6.0",
                    organization, project, runId);
                
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((":" + pat).getBytes()));
                headers.setContentType(MediaType.APPLICATION_JSON);

                var getRequest = new HttpEntity<>(headers);
                var getResponse = restTemplate.exchange(getUrl, HttpMethod.GET, getRequest, Map.class);
                System.out.println("Run status before completion: " + getResponse.getBody());

                // Update test results
                String resultsUrl = String.format("https://dev.azure.com/%s/%s/_apis/test/runs/%d/results?api-version=6.0",
                    organization, project, runId);

                List<Map<String, Object>> results = new ArrayList<>();
                
                // Get the test results from the response
                List<Map<String, Object>> testResults = (List<Map<String, Object>>) ((Map)getResponse.getBody()).get("results");
                if (testResults != null) {
                    for (Map<String, Object> testResult : testResults) {
                        Map<String, Object> update = new HashMap<>();
                        update.put("id", testResult.get("id"));
                        update.put("state", "Completed");
                        update.put("outcome", testResult.get("outcome"));
                        results.add(update);
                    }
                }

                System.out.println("Updating test results with: " + results);
                var patchRequest = new HttpEntity<>(results, headers);
                var patchResponse = restTemplate.exchange(resultsUrl, HttpMethod.PATCH, patchRequest, String.class);
                System.out.println("PATCH Response: " + patchResponse.getBody());

                // Now update the run state
                Map<String, Object> runUpdate = new HashMap<>();
                runUpdate.put("state", "Completed");

                var runRequest = new HttpEntity<>(List.of(runUpdate), headers);
                var runResponse = restTemplate.exchange(getUrl, HttpMethod.PATCH, runRequest, String.class);
                System.out.println("Run completion response: " + runResponse.getBody());
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
