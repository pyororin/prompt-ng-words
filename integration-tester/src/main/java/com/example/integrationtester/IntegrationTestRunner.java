package com.example.integrationtester;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class IntegrationTestRunner implements CommandLineRunner {

    private final TestService testService;

    public IntegrationTestRunner(TestService testService) {
        this.testService = testService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting Integration Tests...");
        List<TestResult> results = testService.loadAndRunTests();

        long totalTests = results.size();
        long passedTests = results.stream().filter(r -> "passed".equalsIgnoreCase(r.getStatus())).count();
        long failedTests = totalTests - passedTests;

        System.out.println("\n--- Integration Test Summary ---");
        System.out.println("Total Tests: " + totalTests);
        System.out.println("Passed: " + passedTests);
        System.out.println("Failed: " + failedTests);

        if (failedTests > 0) {
            System.out.println("\nFailed Tests Details:");
            results.stream()
                   .filter(r -> !"passed".equalsIgnoreCase(r.getStatus())) // More robust check for failed status
                   .forEach(r -> System.out.println(" - Category: " + r.getCategory() +
                                                    ", Prompt: '" + r.getPrompt() +
                                                    (r.getReason() != null && !r.getReason().isEmpty() ? "', Reason: " + r.getReason() : "'")));
        }
        System.out.println("--- End of Summary ---\n");

        generateReportFile(results, totalTests, passedTests, failedTests);

        // Ensure the application exits after the runner is done.
        // This is important for command-line tools.
        // Exit with 0 if all tests passed (or no tests to fail), 1 otherwise.
        System.exit(failedTests > 0 ? 1 : 0);
    }

    private void generateReportFile(List<TestResult> results, long totalTests, long passedTests, long failedTests) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = dateFormat.format(new Date());
        // Store reports in a 'reports' subdirectory to keep the project root clean
        String reportsDir = "reports";
        new java.io.File(reportsDir).mkdirs(); // Create reports directory if it doesn't exist
        String fileName = reportsDir + "/integration_test_report_" + timestamp + ".txt";

        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write("--- Integration Test Report ---\n");
            writer.write("Timestamp: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
            writer.write("Total Tests: " + totalTests + "\n");
            writer.write("Passed: " + passedTests + "\n");
            writer.write("Failed: " + failedTests + "\n\n");

            if (failedTests > 0) {
                writer.write("Failed Tests Details:\n");
                for (TestResult result : results) {
                    if (!"passed".equalsIgnoreCase(result.getStatus())) {
                        writer.write(" - Category: " + result.getCategory() +
                                     ", Prompt: '" + result.getPrompt() +
                                     (result.getReason() != null && !result.getReason().isEmpty() ? "', Reason: " + result.getReason() : "'") +
                                     "\n");
                    }
                }
            }
            writer.write("\n--- End of Report ---");
            System.out.println("Report generated: " + fileName);
        } catch (IOException e) {
            System.err.println("Error writing report file: " + e.getMessage());
        }
    }
}
