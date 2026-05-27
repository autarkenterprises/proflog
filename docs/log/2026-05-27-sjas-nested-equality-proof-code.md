# SJAS Nested Equality Proof-Code Coverage

Date: 2026-05-27

## Context

After direct `free-close` validation was internalized, the next reachable
certificate gap was nested free-constructor equality evidence. Ordinary Proflog
can prove same-head constructor disequalities with proof terms such as
`(decompose (free-close))` and, when earlier arguments must be scanned past,
`(decompose (args (decompose ()) (free-close)))`. The SJAS proof checker could
already consume those decoded proof trees through `equality/eq-contradictiono`,
but the proof-code alphabet rejected `decompose` and `args`, so encoded
certificates could not reach that checker.

An attempted public regression over arithmetic terms `add(0,0)` and `add(0,1)`
also clarified a useful boundary: theorem-code decoding interprets SJAS
arithmetic terms, reducing that negated theorem to `eq(0,1)`. The nested public
test therefore uses inert compact code constructors, `code-2(0,0)` and
`code-2(0,1)`, where the structural constructor shape is intentionally
preserved.

## Change

`decompose` and `args` were appended to the SJAS proof-symbol alphabet, keeping
existing proof-symbol indices stable. The correspondence audit now classifies
both as relevant tableau/equality closure evidence. Focused regressions cover
proof-code encoding for the nested proof term and public `tableau-proof/3`
validation of the corresponding encoded certificate without a host-kernel
validator.

## Verification

- Red: `timeout 40s lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-nested-equality-closure-evidence`
- Green: `timeout 45s lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-nested-equality-closure-evidence`
- Green: `timeout 90s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-decomposed-free-equality-certificates`
- Green: `timeout 45s lein test proflog.sjas-correspondence-test`
- Green: `git diff --check`
- Green: `lein test-proflog-fast`
