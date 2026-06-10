# SJAS Occurs-Check Proof Code

Date: 2026-05-28

## Context

The SJAS-local checker already delegated positive equality contradictions to
`equality/eq-contradictiono`, which can produce `(occurs-close)` when a proof
variable would have to equal a term containing itself. The proof-code alphabet
could not encode that evidence, so public certificates could not carry this
otherwise object-level closure.

The regression uses:

```clojure
exists x. x != f(x)
```

The negated theorem opens a single-use universal branch with `x = f(x)`, which
closes by occurs-check contradiction.

## Change

Appended `occurs-close` to the stable proof-code symbol alphabet and classified
it as relevant closure evidence in the ADR-0073 correspondence audit. No new
checker rule was needed because the existing positive equality contradiction
clause already consumes the decoded proof term locally.

## Verification

- Red: `timeout 45s lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-occurs-check-closure-evidence`
- Green: `timeout 50s lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-occurs-check-closure-evidence`
- Green: `timeout 80s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-occurs-check-closures-without-kernel-validator`
- Green: `timeout 160s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-occurs-check-closure-certificates`
- Green: `timeout 45s lein test proflog.sjas-correspondence-test`
- Green: `git diff --check`
- Green: `lein test-proflog-fast`
- Green: `lein test-proflog-extended`
