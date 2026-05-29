# SJAS Symbol-Table Registry Removal

Date: 2026-05-29
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This Track 1 slice removes the generated finite symbol table from compiled
SJAS programs and from proof-profile formula-code decoding.

Earlier slices split the symbol problem into cases:

- syntax predicates decode application heads as structural numeric `(sym n)`
  ids;
- beta, reflected-axiom, reflected-call, and substitution checks compare user
  symbols structurally;
- fixed U-Grounding arithmetic/profile symbols have reserved numeric ids, so
  proof-facing branch closure can recover `lt`, `leq`, `mult`,
  `tableau-proof`, `subst-prf`, and related semantic primitives without a
  generated registry.

With those pieces in place, the proof profile no longer needs the source
codebook payload. `sjas-symbol-indexo` now recognizes only the reserved SJAS
semantic prefix. User symbols remain inside formula and system codes as
numeric ids, interpreted structurally where user-symbol identity matters.

The builder no longer stores the generated symbol-index table in
`:sjas/registry`.

## Red/Green Evidence

Two focused regressions made the remaining boundary red:

- `sjas-formal-codes-are-godel-byte-terms` asserted that compiled SJAS
  programs do not carry the generated source symbol table;
- `sjas-profile-source-audit-rejects-host-proof-checker-route` asserted that
  neither the proof profile nor the SJAS builder contains the source table
  keyword.

Before the change, the first failed because the registry contained the symbol
table, and the second failed because both source files still mentioned the
table key.

After the change, both selectors pass.

## Consequence

The earlier symbol-table isomorphism argument is now mostly a coding-context
argument at the source construction boundary: user-symbol renamings still
produce isomorphic recodings, but the proof predicate no longer consults a
generated table to resolve those names. The remaining source-side coding
context is used to write formula/system bytes, not to decide proof-predicate
semantics.

This does not encode a full signature block with symbol kind and arity records
inside `system-code`. Instead, it makes the current proof predicate independent
of that host table: semantic primitives are reserved, and user symbols are
compared structurally by numeric id.

## Verification

```text
timeout -k 5s 100s lein test :only proflog.willard-sjas-test/sjas-formal-codes-are-godel-byte-terms
  Ran 1 tests containing 13 assertions.
  0 failures, 0 errors.

timeout -k 5s 100s lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Ran 1 tests containing 21 assertions.
  0 failures, 0 errors.

timeout -k 5s 160s lein test :only proflog.willard-sjas-test/sjas-symbol-table-is-irrelevant-up-to-signature-isomorphism
  Ran 1 tests containing 10 assertions.
  0 failures, 0 errors.

timeout -k 5s 240s lein test :only proflog.willard-sjas-test/sjas-proof-predicates-check-reflected-calls-without-symbol-registry
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.

timeout -k 5s 300s lein test :only proflog.willard-sjas-test/sjas-subst-code-decodes-user-symbols-without-symbol-registry
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 260s lein test :only proflog.willard-sjas-test/sjas-proof-predicates-decode-built-in-relations-without-symbol-registry
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.

git diff --check
  clean.

timeout -k 5s 900s lein test-proflog-fast
  Ran 159 tests containing 594 assertions.
  0 failures, 0 errors.

timeout -k 5s 600s lein test-proflog-extended
  Ran 68 tests containing 203 assertions.
  0 failures, 0 errors.
```
