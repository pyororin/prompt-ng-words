# プロンプトインジェクション検出API (prompt-ng-api)

## 概要

このプロジェクトは、ユーザー入力におけるプロンプトインジェクションの試みを検出するために設計されたSpring Boot製APIです。
LLM（大規模言語モデル）などへの入力テキストを検査し、悪意のある指示や機密情報の漏洩につながる可能性のあるパターンを特定します。

## 主な機能

*   **ルールベースの検出**: `src/main/resources/prompt_injection_rules.yaml` ファイルに定義されたルールセットに基づいてインジェクションの試みを検出します。
    *   **禁止単語リスト**: 特定の危険な単語（日本語、英語、韓国語、中国語に対応）が含まれているかをチェックします。
    *   **インジェクションパターン**: 指示の上書き、役割の変更、機密情報の開示要求など、典型的なインジェクション手法に合致するフレーズや正規表現パターンを検出します。
*   **プロンプト言語制限**: 入力プロンプトは日本語のみ受け付けます。
*   **自然言語処理技術の活用**: テキスト解析の精度向上のため、以下の自然言語処理ライブラリを利用しています。
    *   `kuromoji-ipadic`: 日本語形態素解析によるトークン化と正規化に利用。
    *   `tika-langdetect-optimaize` (`org.apache.tika`): 入力テキストの言語検出に利用。
    *   `commons-text` (`org.apache.commons`): Jaro-Winkler類似度計算など、テキスト操作の補助に利用。
*   **Spring Bootベース**: Java 17 と Spring Boot 3.3.0 を使用して構築されており、既存のSpring Bootアプリケーションへの統合が容易です。

## 仕組み

APIは、受け取った入力テキストを `prompt_injection_rules.yaml` に記述されたルールと照合します。
このファイルには、以下の2つの主要なセクションがあります。

1.  `forbidden_words_jp`: 日本語、英語、韓国語、中国語の禁止単語のリスト。これらの単語が入力に含まれている場合、警告の対象となります。
2.  `injection_patterns`: プロンプトインジェクションとして識別されるフレーズや正規表現パターンのリスト。各パターンには、それが日本語のフレーズ (`japanese_phrase`)、または日本語の正規表現 (`japanese_regex`) であるかを示す `type` が関連付けられています。

一致するルールが見つかった場合、APIはそれが潜在的なプロンプトインジェクションであると判断できます。

## 今後の展望 (例)

*   より高度な検出ロジックの追加（例：機械学習モデルの利用）
*   詳細な設定オプションの提供

## APIドキュメント

このAPIはSwagger (OpenAPI) を使用してAPIドキュメントを提供します。
ドキュメントは、アプリケーションが `development` プロファイルで実行されている場合、またはプロファイルが指定されていない場合にのみ閲覧可能です。
`production` プロファイルがアクティブな場合は、セキュリティのためドキュメントは無効になります。

APIドキュメントには、以下のURLからアクセスできます:

`/swagger-ui.html` (例: `http://localhost:8080/swagger-ui.html`)

ドキュメントには、各エンドポイントの詳細、リクエストとレスポンスのスキーマ、およびパラメータの説明が日本語で記載されています。

---

*このAPIはプロンプトの検査を支援することを目的としています。*
---

## Integration Testing

This project includes a Spring Boot application to run integration tests based on definitions in a YAML file.

### Prerequisites
- Java JDK 11 or higher
- Apache Maven

### Test Configuration
The integration tests are defined in `integration-tester/src/main/resources/integration-test.yaml`. You can add or modify test cases in this file following the existing structure:

```yaml
integration-test:
  prompt:
    ok:
      - "Normal prompt example"
    ng:
      - "Problematic prompt example"
  personal:
    ok:
      - "Personal info, but not identifiable"
    ng:
      - "Identifiable personal information"
```

### Building the Test Application
Navigate to the `integration-tester` directory and use Maven to build the application:
```bash
cd integration-tester
mvn clean package
```
This will generate a JAR file in the `integration-tester/target/` directory (e.g., `integration-tester-0.0.1-SNAPSHOT.jar`).

### Running the Tests
1. Navigate to the `integration-tester` directory (if you are not already there):
   ```bash
   cd integration-tester
   ```
2. Run the integration tests using the following command:
   ```bash
   java -jar target/integration-tester-0.0.1-SNAPSHOT.jar
   ```
This command executes the tests defined in `src/main/resources/integration-test.yaml`.

### Understanding the Results

After execution, you will see output in your console and a detailed report file will be generated.

**Console Output:**
The console will display a summary of the test run, including:
- Total number of tests executed.
- Number of tests that passed.
- Number of tests that failed.
- If there are any failed tests, details for each failed test (category, prompt, and reason for failure) will be listed.

Example Console Output:
```
Starting Integration Tests...

--- Integration Test Summary ---
Total Tests: 8
Passed: 8
Failed: 0
--- End of Summary ---

Report generated: reports/integration_test_report_YYYYMMDD_HHMMSS.txt
```

**Report File:**
- A detailed report is saved as a text file in the `integration-tester/reports/` directory.
- The filename includes a timestamp, for example: `integration_test_report_20231027_123000.txt`.
- This file contains:
    - The timestamp of the test run.
    - The same summary as shown in the console (total, passed, failed).
    - Detailed information for each failed test, if any.

**Interpreting Test Status (Current Simulation):**
- **Passed:** Currently, a test is marked as "passed" if the prompt string from the YAML file is not empty.
- **Failed:** A test is marked as "failed" if the prompt string is empty or null, or if there's an issue like the YAML file not being found.
*(Note: This simulation logic is a placeholder. In the future, these tests will be expanded to interact with the actual API to determine pass/fail status based on its responses.)*
