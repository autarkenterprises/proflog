# AAR-0099: SJAS Track 2a Completion

- Date: 2026-06-13
- ADR: [ADR-0099](../adr/ADR-0099-sjas-track2a-completion.md)
- Branch: `adr-0099-sjas-track2a-completion`

## Outcome

Track 2a of ADR-0073 is complete. The two remaining *unresolved and high risk*
relevance-matrix rows are resolved via the same unreachability route ADR-0098
used for equality:

- **Procedure-call / profile-interleaved theory rules** — the SJAS structural
  checker expands a reflected call into the cited/negated clause body as a
  formula-bearing child (`sjas-system-reflected-call-clauseo`), never consuming
  `pos-call`/`neg-call`/`alt`/guarded tags. Because the expansion is the
  explicit subtree, its size is accounted by the ADR-0097 tree audit — the
  proof-size concern the procedure-call note raised.
- **Quantifier instantiation / witness policy** — a quantifier node introduces a
  `par-term` parameter (or gamma witness) and continues with the instantiated
  body as a formula-bearing child, never consuming `univ`/`once-univ`/`witness`
  tags, with the instantiation carried explicitly and size-accounted.

All three high-risk constructor families are therefore unreachable in accepted
first-fragment certificates. The
[relevance matrix completion note](../log/2026-06-13-sjas-track2a-relevance-matrix-completion.md)
records the final disposition of every matrix row; no proof symbol remains
`:unresolved`. This is an audit — no kernel/checker/encoder/query change.

## Evidence

Source: the structural checker handles reflected calls (6806-6867) and
quantifiers (6311-6602) formula-bearing; the existing suite already accepts
formula-bearing reflected-call and quantifier certificates without call/witness
tags.

Executable (red/green TDD):

- Generalized the ADR-0098 audit: `fragment-reachability-constructor-sets`
  (equality / procedure-call / quantifier) and `audit-fragment-reachability` in
  `proflog.sjas-correspondence`.
- `fragment-reachability-audit-covers-the-high-risk-aspects` /
  `...-flags-tags-and-clears-formula-bearing-certificates` — per-aspect coverage
  and reporting.
- `sjas-procedure-call-expansion-is-formula-bearing-and-tag-free` and
  `sjas-quantifier-instantiation-is-formula-bearing-and-tag-free` — close a
  reflected call and a quantifier through `sjas-proof-check-programo` with a
  formula-bearing certificate, asserting the relevant aspect audits empty and
  the certificate is `:formula-bearing-tableau`.
- `track-2a-relevance-matrix-has-no-unresolved-symbols` — capstone: the Track 2a
  classifier has no `:unresolved` symbol.

Focused run: `Ran 4 tests containing 34 assertions. 0 failures, 0 errors.`

Broad gates:

- `lein test-proflog-fast` — Ran 196 tests containing 1035 assertions, 0
  failures (carries the three new `proflog.sjas-correspondence-test` cases).
- `lein test-proflog-sjas` (not-slow) — 140 vars, 1016 assertions, 0 failures
  (carries the two willard-sjas reachability probes and the source audits).

## Follow-up

- **Track 2b** is now unblocked: a correspondence theorem, over the
  formula-bearing tree, that the equality/call/quantifier closures correspond to
  the selected SJAS `D` (or specified free-constructor / reflected-axiom /
  parameter theories) and preserve the proof-size lower bound — plus the choice
  of formal proof medium (the matrix's standing meta-obligation). The parallel
  agent owns Track 2b (ADR-0096/0097).
- Independent of Track 2: the subst-prf negative-exhaustion tractability wall
  (algorithmic; subgoal tabling / search-relevance prefilter).
