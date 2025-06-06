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

## 統合テスト

このプロジェクトには、YAMLファイル内の定義に基づいて統合テストを実行するためのSpring Bootアプリケーションが含まれています。

### 前提条件
- Java JDK 11 以上
- Apache Maven

### テスト設定
統合テストは `integration-tester/src/main/resources/integration-test.yaml` で定義されています。既存の構造に従って、このファイルにテストケースを追加または変更できます。

```yaml
integration-test:
  prompt:
    ok:
      - "通常のプロンプト例"
    ng:
      - "問題のあるプロンプト例"
  personal:
    ok:
      - "個人情報ですが、特定はできません"
    ng:
      - "特定可能な個人情報"
```

### テストアプリケーションのビルド
`integration-tester` ディレクトリに移動し、Mavenを使用してアプリケーションをビルドします。
```bash
cd integration-tester
mvn clean package
```
これにより、`integration-tester/target/` ディレクトリにJARファイル（例: `integration-tester-0.0.1-SNAPSHOT.jar`）が生成されます。

### テストの実行
ビルド後、`integration-tester` ディレクトリから次のコマンドを使用して統合テストを実行できます（まだそのディレクトリにいない場合）。
1. `integration-tester` ディレクトリに移動します（まだ移動していない場合）：
   ```bash
   cd integration-tester
   ```
2. 次のコマンドを使用して統合テストを実行します。
   ```bash
   java -jar target/integration-tester-0.0.1-SNAPSHOT.jar
   ```
このコマンドは `src/main/resources/integration-test.yaml` で定義されたテストを実行します。

### 結果の確認方法

実行後、コンソールに出力が表示され、詳細なレポートファイルが生成されます。

**コンソール出力:**
コンソールには、テスト実行の概要が表示されます。これには以下が含まれます。
- 実行されたテストの総数
- 合格したテストの数
- 失敗したテストの数
- 失敗したテストがある場合、各失敗テストの詳細（カテゴリ、プロンプト、失敗理由）が一覧表示されます。

コンソール出力例:
```
Starting Integration Tests...

--- Integration Test Summary ---
Total Tests: 8
Passed: 8
Failed: 0
--- End of Summary ---

Report generated: reports/integration_test_report_YYYYMMDD_HHMMSS.txt
```

**レポートファイル:**
- 詳細なレポートは、`integration-tester/reports/` ディレクトリにテキストファイルとして保存されます。
- ファイル名にはタイムスタンプが含まれます（例: `integration_test_report_20231027_123000.txt`）。
- このファイルには以下が含まれます:
    - テスト実行のタイムスタンプ。
    - コンソールに表示されるものと同じ概要（合計、合格、失敗）。
    - 失敗したテストがある場合は、その詳細情報。

**テストステータスの解釈（改善されたロジック）:**
以前の単純な空文字チェックから、テストロジックはキーワードおよびパターンマッチングに基づく判定を行うように改善されました。
「合格」および「失敗」の意味は以下の通りです。

- **合格 (passed):**
  テスト対象のプロンプトが、そのカテゴリ（例: `prompt.ng`、`personal.ok`）の基準に従って、システムによって正しく識別・分類されたことを意味します。
    - `prompt.ok` の場合: プロンプトに不適切（NG）とされるキーワードや個人情報（PII）パターンが含まれていない場合に合格となります。
    - `prompt.ng` の場合: プロンプトに不適切（NG）とされるキーワードが含まれている場合に合格となります（システムがNGであることを正しく検知した）。
    - `personal.ok` の場合: プロンプトに特定の個人情報（PII）パターンや不適切（NG）とされるキーワードが含まれていない場合に合格となります。
    - `personal.ng` の場合: プロンプトに特定の個人情報（PII）パターンが含まれている場合に合格となります（システムがPIIを正しく検知した）。

- **失敗 (failed):**
  システムがプロンプトをそのカテゴリの基準に従って正しく識別・分類できなかったことを意味します。
    - 例えば、`prompt.ng` にリストされているプロンプトからNGキーワードが検出されなかった場合、そのテストは「失敗」となります。
    - 同様に、`personal.ng` のプロンプトからPIIパターンが検出されなかった場合も「失敗」となります。

レポートの「理由」フィールドには、各テストが合格または失敗した具体的な根拠（例: 「OK: NGキーワードが期待通り検出されました。」、「FAIL: personal.ngと判断されるべきプロンプトでPIIパターンが見つかりませんでした。」など）が示されます。

*(注意: このキーワードおよびパターンベースのロジックは、より洗練されたAI判定への第一歩です。定義されたキーワードやパターンは `TestService.java` 内で管理されており、必要に応じて拡張できます。)*
