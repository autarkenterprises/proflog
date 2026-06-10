# Stack-Safe Large Proof Terms

ADR-0073 Track 1's U-Grounding proof certificate exposed two distinct host-stack
failures:

1. core.logic's occurs check recursed through the first child of deeply nested
   persistent collections.
2. Proflog's surface term validator recursed through every unary constructor
   layer before proof search began.

## Core.logic Occurs Check

Added `proflog.core-logic-occurs-check-test` with a deep acyclic ground-term
binding and a direct self-reference rejection assertion. The new selector failed
red with `StackOverflowError` at `clojure.core.logic/occurs-check`, then passed
after `vendor/core.logic-1.0.1/src` became the default source overlay and the
occurs check used an explicit worklist. The existing 1.1.1 overlay received the
same patch.

## Language Validation

After the core.logic fix, the U-Grounding proof selector exposed
`proflog.language/validate-term` recursion. Added a focused language regression
for a deep declared unary successor term. It failed red with `StackOverflowError`
and passed after `validate-term` switched to an explicit worklist.

## Final Evidence

```text
lein test :only proflog.core-logic-occurs-check-test/occurs-check-is-stack-safe-for-deep-acyclic-ground-terms
Ran 1 tests containing 2 assertions.
0 failures, 0 errors.
elapsed 0:11.33 maxrss 203500KB

lein test :only proflog.language-test/validate-term-accepts-deep-declared-unary-terms
Ran 1 tests containing 1 assertions.
0 failures, 0 errors.
elapsed 0:09.17 maxrss 213336KB

lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-u-grounding-formula-bearing-proof-certificate
Ran 1 tests containing 6 assertions.
0 failures, 0 errors.
elapsed 2:18.62 maxrss 427616KB

lein test-proflog-fast
Ran 167 tests containing 659 assertions.
0 failures, 0 errors.
elapsed 2:22.51 maxrss 444196KB

lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
elapsed 5:49.71 maxrss 570300KB
```

`lein probe-core-logic-host` reports the default runtime source as
`vendor/core.logic-1.0.1/src/clojure/core/logic.clj` with marker
`vendor/core.logic-1.0.1/src stack-safe-occurs-check`.

## Long SelfCons Probe Notes

The exact Group-3 SelfCons public proof-query remains a correctness probe rather
than a fast or medium-duration regression. After the stack-safety fixes:

- `sjas-tableau-proof-accepts-formula-bearing-selfcons-certificate` exceeded a
  900-second foreground run. It is marked `^:slow` with expected duration
  `> 15 minutes`; repeat measurements should use durable `test-runs/` logging.
- An exploratory exact SelfCons U-Grounding public proof-query also exceeded a
  900-second foreground run after an earlier recursive test-helper overflow.
- A representation-only exact SelfCons U-Grounding certificate probe exceeded a
  180-second foreground run.

These timeouts did not produce a new stack trace. The passing Track 1
representation evidence for U-Grounding proof codes is the smaller public
formula-bearing proof-path selector above, and the passing concrete SelfCons
evidence remains the in-memory/core structural checks plus the compact public
selector as a long correctness probe with an open runtime envelope.

On 2026-06-08, after ADR-0077 removed exact duplicate structural-checker
branches, the focused core SelfCons selector was relaunched durably with
`setsid`:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-selfcons-core-tableau
pid 224039
started 2026-06-08T21:33:15Z
log test-runs/selfcons-core-no-timeout-20260608T213315Z.log
```

Per the user's instruction, the run was not killed. At
`2026-06-08T21:49:15Z`, the wrapper PID was still alive at `15:59` elapsed,
so this focused SelfCons core selector had passed the 15-minute milestone under
durable logging.

A non-invasive JVM thread sample of child PID `224210` during the run showed
the active thread in `clojure.core.logic/occurs-check-worklist`, reached via
`membero` and `==`. The current hypothesis is therefore operational rather than
semantic: stack safety is fixed, but the checker/decoder still performs too many
general occurs-check scans while enumerating static or broad proof-search
alternatives. ADR-0078 records the next finite-table scheduling cleanup.

## SJAS Proof-Check Scheduling Cleanup

ADR-0077 removed redundant structural proof-checker alternatives: exact
duplicate guided literal, complementary literal, and conjunction branches, plus
an unguided literal-continuation branch covered by the decoded child-formula
guided path. ADR-0078 replaced recursive `membero` scans in fixed SJAS metadata
tables with explicit finite alternatives using the local acyclic unifier.

Focused evidence after these changes:

```text
sjas-structural-proof-checker-does-not-duplicate-guided-branches
Ran 1 tests containing 4 assertions.
0 failures, 0 errors.
elapsed 2:21.38 maxrss 254664KB

sjas-static-code-table-lookups-avoid-membero-scheduling
Ran 1 tests containing 14 assertions.
0 failures, 0 errors.
elapsed 1:46.14 maxrss 280744KB

sjas-proof-code-decoder-checks-wide-formula-bearing-tableau-nodes
Ran 1 tests containing 7 assertions.
0 failures, 0 errors.
elapsed 3:01.38 maxrss 333264KB

sjas-tableau-proof-accepts-u-grounding-formula-bearing-proof-certificate
Ran 1 tests containing 6 assertions.
0 failures, 0 errors.
elapsed 2:50.42 maxrss 379368KB

sjas-proof-check-accepts-formula-bearing-complementary-literal-closures
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.
elapsed 2:49.47 maxrss 297832KB

sjas-proof-check-accepts-formula-bearing-right-first-conjunction-tableaux
Ran 1 tests containing 6 assertions.
0 failures, 0 errors.
elapsed 2:37.14 maxrss 276800KB

sjas-proof-check-accepts-formula-bearing-and-true-false-tableaux
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.
elapsed 3:01.97 maxrss 276200KB
```

The final broad gates passed under heavy concurrent load from the preserved
SelfCons probes:

```text
lein test-proflog-fast
Ran 167 tests containing 659 assertions.
0 failures, 0 errors.
elapsed 15:38.49 maxrss 370056KB

lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
elapsed 35:08.07 maxrss 935680KB
```

The ADR-0078 intermediate SelfCons run,
`test-runs/selfcons-core-adr78-20260608T220400Z.log`, also passed 15 minutes and
remained alive past 58 minutes, but it was launched before ADR-0077's final
guided-only literal continuation cleanup. A fresh latest-source SelfCons timing
was launched separately as PID `242205` with log
`test-runs/selfcons-core-latest-20260608T230532Z.log`; that run includes the
final ADR-0077 and ADR-0078 source state and should be monitored for completion
or its own 15-minute milestone. At `2026-06-08T23:21:25Z`, the latest-source
run was still alive at `15:52` elapsed, so the focused core SelfCons selector
still exceeds the 15-minute milestone under the current loaded conditions.

After that milestone, a JVM sample of the latest-source run showed the hot stack
in `decode-embedded-code-bodyo`, allocating `payload` before failed length
headers had been rejected. ADR-0079 moved low/high header checks before payload
allocation for embedded code and natural payload decoders.

ADR-0079 focused evidence:

```text
sjas-embedded-payload-decoders-check-header-before-payload-fresh
Ran 1 tests containing 2 assertions.
0 failures, 0 errors.
elapsed 1:49.35 maxrss 277076KB

sjas-proof-code-decoder-checks-wide-formula-bearing-tableau-nodes
Ran 1 tests containing 7 assertions.
0 failures, 0 errors.
elapsed 2:43.06 maxrss 295860KB

sjas-tableau-proof-accepts-u-grounding-formula-bearing-proof-certificate
Ran 1 tests containing 6 assertions.
0 failures, 0 errors.
elapsed 3:57.83 maxrss 393932KB
```

Clean post-ADR-0079 gates:

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

The SelfCons probe PIDs later disappeared and the three durable logs contained
only the namespace header, with no pass/fail or `/usr/bin/time` trailer. They
therefore remain milestone/runtime evidence, not result evidence.

## Post-ADR-0079 SelfCons Milestone

After ADR-0079, a fresh SelfCons core proof-check probe was launched as wrapper
PID `12980`, with durable log
`test-runs/selfcons-core-post-adr79-20260609T002713Z.log` and exit-code file
`test-runs/selfcons-core-post-adr79-20260609T002713Z.exit`. At
`2026-06-09T00:42:45Z`, the wrapper was still alive at `15:20` elapsed and the
log still contained only the namespace header:

```text
lein test proflog.willard-sjas-test
```

This satisfies the requested 15-minute record point for the preserved long
SelfCons run. It is not pass/fail evidence, and the process was left running.

## ADR-0080 App-Arity Dispatch

The post-ADR-0079 SelfCons sample moved the hot path to application arity
decoding, where each finite arity candidate tried to match the same byte-stream
tail. ADR-0080 changed ordinary app-term decoding, syntax-only app-term
decoding, and syntax-skip app-term decoding to destructure the encoded arity
byte once and then dispatch over finite argument counts.

The ADR-0080 source-audit selector failed red before implementation:

```text
sjas-app-arity-decoders-destructure-arity-byte-once
Ran 1 tests containing 6 assertions.
3 failures, 0 errors.
elapsed 0:18.02
```

It passed after the scheduling change, and the final current-worktree focused
evidence was:

```text
sjas-app-arity-decoders-destructure-arity-byte-once
Ran 1 tests containing 6 assertions.
0 failures, 0 errors.
elapsed 0:36.08 maxrss 248284KB

sjas-proof-code-decoder-checks-wide-formula-bearing-tableau-nodes
Ran 1 tests containing 7 assertions.
0 failures, 0 errors.
elapsed 0:39.63 maxrss 271776KB

sjas-tableau-proof-accepts-u-grounding-formula-bearing-proof-certificate
Ran 1 tests containing 6 assertions.
0 failures, 0 errors.
elapsed 0:47.49 maxrss 436728KB
```

Clean post-ADR-0080 gates:

```text
lein test-proflog-fast
Ran 167 tests containing 659 assertions.
0 failures, 0 errors.
elapsed 2:32.35 maxrss 422208KB

lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
elapsed 5:47.17 maxrss 659796KB
```

ADR-0080 is the last optimization opened during the core.logic stack-safety
thread. Further work returns to ADR-0073 Track 1 arithmeticization; optimizing
an incomplete proof predicate remains premature.
