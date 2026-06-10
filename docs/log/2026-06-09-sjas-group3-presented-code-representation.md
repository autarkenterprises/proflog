# SJAS Group-3 Presented Code Representation

## Context

Track 1 fixed-point reconstruction requires Group-3 to refer to the public
system code `s` selected by the proof predicate, not merely to an equivalent
byte payload. ADR-0071 made this distinction observable: a `:u-grounding`
system excludes compact `code-N` constructors from its language and presents
formula, system, and proof codes as binary U-Grounding numerals.

The existing proof-free `AxiomConj(s)` path already used the walked public
`system-code` for Tableau-0, but the axiom-member Group-3 relations and the
Level-1 Group-3 reconstruction still accepted either internal code-term shape
after decoding the same byte payload. That allowed a U-Grounding system to cite
a compact-embedded variant of its self-consistency sentence.

## Change

- Added focused red tests showing that U-Grounding Tableau-0 and Level-1
  Group-3 membership must reject formulas that embed compact code terms with
  the same bytes as the presented U-Grounding `s`.
- Extended `sjas-public-code-byteso` and `sjas-public-code-bytes-coreo` to
  expose the public code representation kind relationally.
- Added `code-kind-internal-termo`, then threaded that kind through
  Tableau-0 and Level-1 Group-3 reconstruction in both proof-producing
  `axiom-member` and proof-free core paths.
- Updated Level-1 `AxiomConj(s)` reconstruction to use the same public code
  representation for the fixed-point skeleton check.
- Updated proof-free system-code validation to call the presented-code
  `AxiomConj` arity after walking equality state, so validation and
  reconstruction agree.

The change preserves relational purity: code representation is inferred by the
existing object-level code readers and propagated as a logic value. No source
registry, host code-format lookup, or host byte projector was added.

## Evidence

Red before implementation:

```text
lein test :only proflog.willard-sjas-test/sjas-tableau0-group-three-rejects-wrong-public-code-representation
FAIL, elapsed 1:38.95, maxrss 288752KB
```

The initial Level-1 `AxiomConj(s)` regression was stopped after about nine
minutes because it reconstructed the full U-Grounding axiom conjunction and was
too broad for a focused red selector. It was replaced with a direct Group-3
membership regression over the same malformed fixed-point representation.

Green focused selectors after implementation:

```text
lein test :only proflog.willard-sjas-test/sjas-tableau0-group-three-rejects-wrong-public-code-representation
0 failures, elapsed 2:50.09, maxrss 281656KB

lein test :only proflog.willard-sjas-test/sjas-level1-group-three-rejects-wrong-public-code-representation
0 failures, elapsed 3:15.99, maxrss 348520KB

lein test :only proflog.willard-sjas-test/sjas-subst-prf-substitution-axiom-branch-validates-system-code
0 failures, elapsed 2:47.38, maxrss 250024KB

lein test :only proflog.willard-sjas-test/sjas-proof-predicate-system-code-reconstruction-walks-equality-state
0 failures, elapsed 3:16.21, maxrss 303764KB

lein test :only proflog.willard-sjas-test/sjas-tableau0-axiomconj-reconstructs-zero-one-selfcons-target
0 failures, elapsed 2:36.26, maxrss 282440KB

lein test :only proflog.willard-sjas-test/sjas-tableau-proof-cites-tableau0-group-three-from-system-code
0 failures, elapsed 2:44.07, maxrss 350936KB

lein test :only proflog.willard-sjas-test/sjas-tableau-proof-cites-level1-group-three-from-system-code
0 failures, elapsed 2:58.16, maxrss 388740KB

lein test :only proflog.willard-sjas-test/sjas-level1-group-three-uses-selfcons-skeleton-code
0 failures, elapsed 2:15.69, maxrss 262688KB

lein test :only proflog.willard-sjas-test/sjas-u-grounding-tableau-proof-checks-numeral-system-theorem-and-proof-codes
0 failures, elapsed 2:04.21, maxrss 264928KB

lein test :only proflog.willard-sjas-test/sjas-u-grounding-subst-code-computes-level1-fixed-point
0 failures, elapsed 2:00.32, maxrss 273780KB

lein test :only proflog.willard-sjas-test/sjas-u-grounding-code-format-emits-numeral-codes-without-code-constructors
0 failures, elapsed 1:53.53, maxrss 236668KB
```

Broad gates were started in parallel after the focused selectors and rerun
after the final validator consistency patch:

```text
lein test-proflog-fast
0 failures, elapsed 10:12.39, maxrss 458616KB

lein test-proflog-extended
0 failures, elapsed 22:34.93, maxrss 752924KB
```
