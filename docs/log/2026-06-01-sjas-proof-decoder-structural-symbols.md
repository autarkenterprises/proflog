# SJAS Proof Decoder Structural Symbols

Date: 2026-06-01

## Context

ADR-0073 Track 1 removes host-side source registries and nominal lookup paths
from SJAS proof predicates. The proof-facing formula decoder still retained a
private bridge for direct proof-checker tests: it reconstructed a source
signature codebook from the compiled program language, then used host relation
symbols when matching reflected procedure calls.

That bridge was narrower than the old generated symbol registry, but it still
made reflected proof checking depend on source-language names rather than on
the structural symbol indexes present in formula and system codes.

## Change

Added an object symbol decoder with two cases:

- fixed SJAS vocabulary indexes decode to their semantic symbols, such as
  arithmetic and proof-profile relations;
- user symbol indexes decode to structural `(sym n)` identifiers.

Proof-facing formula decoding now uses that object decoder directly. Reflected
call matching compares the focused call head with the relation index decoded
from `system-code`; it no longer reconstructs `program-coding-context`, uses
`sjas-host-symbol-indexo`, or falls back to host relation names.

Private proof-checker tests that intentionally bypass theorem-code decoding now
build targets with structural `(sym n)` heads, matching the shape produced by
the arithmeticized formula-code decoder.

## Test Evidence

The source audit was made red by rejecting:

- `program-coding-context`
- `sjas-host-symbol-indexo`
- `sjas-reflected-relation-indexo`

After the implementation, the audit and reflected-call proof checks passed.

## Verification

- `lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route`
- reflected-call and guarded-call focused selectors for private proof checker
  and public `tableau-proof/3` certificates
- `lein test-proflog-sjas-focused`: `pass=462 fail=0 error=0`
- `lein test-proflog-fast`: 164 tests, 653 assertions
- `lein test-proflog-extended`: 68 tests, 203 assertions
- `git diff --check`
