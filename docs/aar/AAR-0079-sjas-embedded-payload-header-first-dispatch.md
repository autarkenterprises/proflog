# AAR-0079: SJAS Embedded Payload Header-First Dispatch

- Date: 2026-06-08
- ADR: [ADR-0079](../adr/ADR-0079-sjas-embedded-payload-header-first-dispatch.md)
- Branch: `adr-0073-sjas-correspondence-program`

## Outcome

Changed embedded code and natural payload decoding so each bounded length
candidate checks its low/high header bytes before allocating the `payload` logic
variable. The bounded finite enumeration remains relational, but wrong length
candidates now fail before parsing payload bytes or allocating payload state.

The change was motivated by a latest-source SelfCons JVM sample that showed the
hot stack in `decode-embedded-code-bodyo` around payload allocation.

## Evidence

The source-audit regression failed red before the implementation:

```text
lein test :only proflog.willard-sjas-test/sjas-embedded-payload-decoders-check-header-before-payload-fresh
Ran 1 tests containing 2 assertions.
2 failures, 0 errors.
elapsed 1:46.92 maxrss 232612KB
```

After moving header checks before `fresh [payload]`, the same selector passed:

```text
Ran 1 tests containing 2 assertions.
0 failures, 0 errors.
elapsed 1:49.35 maxrss 277076KB
```

Focused semantic selectors stayed green:

```text
sjas-proof-code-decoder-checks-wide-formula-bearing-tableau-nodes
Ran 1 tests containing 7 assertions.
0 failures, 0 errors.
elapsed 2:43.06 maxrss 295860KB

sjas-tableau-proof-accepts-u-grounding-formula-bearing-proof-certificate
Ran 1 tests containing 6 assertions.
0 failures, 0 errors.
elapsed 3:57.83 maxrss 393932KB
```

Clean post-change gates passed:

```text
lein test-proflog-fast
Ran 167 tests containing 659 assertions.
0 failures, 0 errors.
elapsed 1:34.24 maxrss 403224KB

lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
elapsed 3:48.10 maxrss 551248KB
```

## Follow-up

The latest SelfCons probes later had no live PIDs and their durable logs
contained only the test namespace header. They remain useful 15-minute milestone
evidence, but they are not pass/fail evidence. A future SelfCons run should use
a more robust runner that records child JVM death and exit status even if the
process is killed externally.
