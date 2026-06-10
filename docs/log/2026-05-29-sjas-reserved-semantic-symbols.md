# SJAS Reserved Semantic Symbols

Date: 2026-05-29
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This Track 1 slice addresses the large semantic symbol boundary left by the
earlier syntax-only and reflected-call work.

The syntax-only decoder can treat an application head as structural data such
as `(sym n)`. That is sufficient for `wff`, beta membership, reflected-axiom
membership, and reflected procedure-call comparison when only identity of the
encoded symbol matters. It is not sufficient for semantic profile rules. A
proof checker that sees the code for `lt(1, 2)` must know that the application
head is the fixed SJAS arithmetic relation `lt`, not merely an opaque symbol
id, because branch closure depends on invoking the U-Grounding `<` relation.

The encoding now gives the fixed SJAS vocabulary a reserved prefix:

- `0`, `1`;
- U-Grounding arithmetic functions such as `add`, `dbl`, `pred`, `sub`,
  `div`, `max`, `log`, `root`, and `count`;
- profile relations such as `lt`, `leq`, `mult`, `wff`, `axiom-member`,
  `tableau-proof`, `subst-code`, and `subst-prf`.

User symbols still live in a conventional finite codebook, sorted after the
reserved prefix. This is intentionally narrower than full signature
internalization: it internalizes the semantic constants whose interpretation
affects proof-predicate closure, while ordinary user relation names remain
irrelevant up to signature isomorphism unless a future slice encodes the whole
signature structurally in `system-code`.

## Compact-Code Reader Probe

Before this semantic slice, I attempted to remove the compact public-code
ground deconstruction path (`ground-compact-code-args` /
`ground-code-argso`). A source-audit regression made that red, and deleting the
fast path made existing focused selectors exceed their timeouts:

```text
sjas-syntax-predicates-decode-application-codes-without-symbol-registry
  timeout 120s

sjas-proof-predicates-do-not-require-source-preprocessing-registry
  timeout 140s

sjas-tableau-proof-accepts-axiom-citation-certificates
  timeout 150s
```

That path was restored. Its status is a representation/tractability boundary:
it deconstructs a ground public `code-N` term but still emits one
`sjas-code-arg` proof node per byte via `code-byte-termo`. It is not the same
kind of semantic shortcut as decoding `lt` through a host registry, because it
does not decide which proof rule closes a branch. A later arithmeticization
slice may still replace it, but this commit prioritizes the larger semantic
boundary identified by the user.

## Red/Green Evidence

The focused regression
`sjas-proof-predicates-decode-built-in-relations-without-symbol-registry`
removes `:sjas/registry` from a Tableau-0 demo system and asks both
`tableau-proof/3` and `subst-prf/4` to validate the certificate for the
non-generated arithmetic theorem `lt(1, 2)`.

Before the change, both checks failed:

```text
FAIL in (sjas-proof-predicates-decode-built-in-relations-without-symbol-registry)
tableau-proof must recover fixed arithmetic relation semantics from formula-code structure, not the source symbol registry
  actual: (not (successful? ()))

FAIL in (sjas-proof-predicates-decode-built-in-relations-without-symbol-registry)
subst-prf must recover fixed arithmetic relation semantics from formula-code structure, not the source symbol registry
  actual: (not (successful? ()))
```

After the change, the same selector passes. Proof-facing formula decoding can
recover reserved symbols through the fixed prefix even when the generated
symbol registry is absent.

## Remaining Boundary

This does not make every user-level signature fact internal. User relations and
functions are still finite-codebook entries unless they appear in reflected
system-code clauses where structural symbol-id comparison is enough. Full
signature internalization would encode symbol kind and arity records inside
`system-code` and decode all applications against that structural signature.

For current ADR-0073 Track 1 purposes, the important improvement is that fixed
SJAS semantic primitives no longer need host registry lookup to choose their
proof-closing relation. The remaining user-symbol codebook is still justified
only up to kind/arity-preserving signature isomorphism.

## Verification

```text
timeout -k 5s 260s lein test :only proflog.willard-sjas-test/sjas-proof-predicates-decode-built-in-relations-without-symbol-registry
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.

timeout -k 5s 100s lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Ran 1 tests containing 19 assertions.
  0 failures, 0 errors.

timeout -k 5s 120s lein test :only proflog.willard-sjas-test/sjas-syntax-predicates-decode-application-codes-without-symbol-registry
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-proof-predicates-do-not-require-source-preprocessing-registry
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.

timeout -k 5s 160s lein test :only proflog.willard-sjas-test/sjas-symbol-table-is-irrelevant-up-to-signature-isomorphism
  Ran 1 tests containing 10 assertions.
  0 failures, 0 errors.

timeout -k 5s 160s lein test :only proflog.willard-sjas-test/sjas-beta-axiom-member-decodes-application-codes-without-symbol-registry
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-reflected-axiom-member-decodes-application-codes-without-symbol-registry
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 220s lein test :only proflog.willard-sjas-test/sjas-proof-predicates-check-reflected-calls-without-symbol-registry
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.

timeout -k 5s 240s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-checks-structural-non-generated-theorem-codes
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.

timeout -k 5s 260s lein test :only proflog.willard-sjas-test/sjas-subst-prf-checks-structural-non-generated-theorem-codes
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-axiom-citation-certificates
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.
```
