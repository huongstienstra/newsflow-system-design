# NewsFlow Test Report

Generated: 2026-06-01

## Commands Run

```bash
./gradlew :app:clean :app:testDebugUnitTest :app:koverHtmlReportDebug :app:koverXmlReportDebug :app:koverVerifyDebug :app:assembleDebug
```

## Result

- Unit tests: 60 passed, 0 failed, 0 skipped.
- Debug APK build: passed.
- Kover verification: passed with an 80% minimum line coverage gate.

## Coverage

- Line coverage: 87.0% (844 / 970)
- Method coverage: 94.8% (163 / 172)
- Class coverage: 97.5% (79 / 81)
- Branch coverage: 63.4% (232 / 366)
- Instruction coverage: 83.7% (4449 / 5313)

## Reports

- HTML coverage report: `app/build/reports/kover/htmlDebug/index.html`
- XML coverage report: `app/build/reports/kover/reportDebug.xml`
- Unit test results: `app/build/test-results/testDebugUnitTest`
