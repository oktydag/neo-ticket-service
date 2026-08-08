# neo-ticket-service
Secure Ticketing &amp; Reservation API

## Test coverage

```
mvn clean verify
```

runs the full suite (unit + integration) and enforces a JaCoCo gate (line ≥ 80%, branch ≥ 70%,
see `pom.xml`). The build fails if coverage drops below these thresholds.

| Suite            | Tests | Line   | Branch | Instruction | Method |
|-------------------|:-----:|-------:|-------:|------------:|-------:|
| Unit tests only    | 256   | 86.82% | 66.33% | 86.28%      | 83.47% |
| Full suite (+ IT)  | 270   | 89.19% | 71.72% | 88.82%      | 85.06% |

After running `mvn clean verify`, open the HTML report for a line-by-line, package-by-package
breakdown:

```
open target/site/jacoco/index.html
```
