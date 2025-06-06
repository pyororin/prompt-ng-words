import yaml
import datetime
import os

def run_simulated_test(prompt: str) -> tuple[str, str | None]:
    """
    Simulates a test for a given prompt.
    For now, it just checks if the prompt is not empty.
    """
    if prompt and isinstance(prompt, str) and prompt.strip():
        return "passed", None
    return "failed", "Prompt was empty or not a string."

def main():
    report_lines = []
    test_results = []
    timestamp = datetime.datetime.now()
    report_lines.append(f"Integration Test Run: {timestamp.strftime('%Y-%m-%d %H:%M:%S')}\n")

    yaml_file_path = "integration-tester/src/main/resources/integration-test.yaml"
    if not os.path.exists(yaml_file_path):
        report_lines.append(f"ERROR: YAML file not found at {yaml_file_path}")
        print(f"ERROR: YAML file not found at {yaml_file_path}")
        # Write a minimal report and exit if YAML is missing
        report_filename = f"integration_test_report_{timestamp.strftime('%Y%m%d_%H%M%S')}.txt"
        with open(report_filename, "w") as f:
            f.write("\n".join(report_lines))
        print(f"Report generated: {report_filename}")
        return

    try:
        with open(yaml_file_path, "r") as f:
            data = yaml.safe_load(f)
        if not data or "integration-test" not in data:
            report_lines.append("ERROR: YAML file is malformed or missing 'integration-test' root key.")
            print("ERROR: YAML file is malformed or missing 'integration-test' root key.")
            # Write a minimal report and exit
            report_filename = f"integration_test_report_{timestamp.strftime('%Y%m%d_%H%M%S')}.txt"
            with open(report_filename, "w") as f:
                f.write("\n".join(report_lines))
            print(f"Report generated: {report_filename}")
            return

        test_data = data.get("integration-test", {})
    except yaml.YAMLError as e:
        report_lines.append(f"ERROR: Could not parse YAML file: {e}")
        print(f"ERROR: Could not parse YAML file: {e}")
        # Write a minimal report and exit
        report_filename = f"integration_test_report_{timestamp.strftime('%Y%m%d_%H%M%S')}.txt"
        with open(report_filename, "w") as f:
            f.write("\n".join(report_lines))
        print(f"Report generated: {report_filename}")
        return

    categories = {
        "prompt.ok": test_data.get("prompt", {}).get("ok", []),
        "prompt.ng": test_data.get("prompt", {}).get("ng", []),
        "personal.ok": test_data.get("personal", {}).get("ok", []),
        "personal.ng": test_data.get("personal", {}).get("ng", []),
    }

    total_tests = 0
    passed_tests = 0
    failed_tests = 0

    for category_name, prompts in categories.items():
        if not isinstance(prompts, list):
            print(f"Warning: Expected a list of prompts for category '{category_name}', but got {type(prompts)}. Skipping.")
            report_lines.append(f"Warning: Prompts for category '{category_name}' is not a list. Skipping.")
            continue
        for prompt in prompts:
            total_tests += 1
            status, reason = run_simulated_test(prompt)
            test_results.append({
                "prompt": prompt,
                "category": category_name,
                "status": status,
                "reason": reason,
            })
            if status == "passed":
                passed_tests += 1
            else:
                failed_tests += 1

    report_lines.append(f"Total tests run: {total_tests}")
    report_lines.append(f"Passed: {passed_tests}")
    report_lines.append(f"Failed: {failed_tests}\n")

    if failed_tests > 0:
        report_lines.append("Failed Tests:")
        for result in test_results:
            if result["status"] == "failed":
                report_lines.append(
                    f"  - Category: {result['category']}, Prompt: \"{result['prompt']}\", Reason: {result['reason']}"
                )

    report_filename = f"integration_test_report_{timestamp.strftime('%Y%m%d_%H%M%S')}.txt"
    try:
        with open(report_filename, "w") as f:
            f.write("\n".join(report_lines))
        print(f"Report generated: {report_filename}")
    except IOError as e:
        print(f"ERROR: Could not write report file: {e}")

if __name__ == "__main__":
    main()
