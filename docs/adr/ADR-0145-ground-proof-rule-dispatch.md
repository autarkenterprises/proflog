# ADR-0145: Ground-Proof Rule Dispatch — Proof-Node Match Deduplication

- Status: accepted
- Date: 2026-07-02
- Branch: `adr-0145-ground-proof-rule-dispatch` (off `adr-0144-checker-ground-determinism` @ `3cb5ab7`)
- AAR: [AAR-0145](../aar/AAR-0145-ground-proof-rule-dispatch.md)

## Context

ADR-0144 diagnosed the ground-tree checker wall as a **~78 000-node search for a
4-node ground proof** — the checker *searches* for a rule interpretation of each
ground node instead of *following* the ground tree — and handed this ADR the
follow-up: deterministic ground-proof rule dispatch, at the SJAS profile layer,
with the completeness argument as the central obligation.

Arm-level instrumentation (warm nREPL, benchmark
`wrong-premise-leaves-the-universal-open` at fuel 10) located the dominant
multiplier not in the 43-arm rule `conde` itself but in its **preamble**:
`sjas-proof-node-formula-matcho`, the reconciliation of the visible branch
formula against the decoded proof-node formula, run at every decoded-checker
entry. Counts for the 4-node tree: matcho constructed 5 528×, its alpha arm
pulled **8 705×** (with recursion), and the checker's quantifier arms pulled
**16 926×** (`sjas-next-branch-nomo` constructions) — each duplicate match
success re-explores the entire downstream checker subtree under backtracking.

**The duplication mechanism.** matcho was a flat 4-arm ladder
(binder-renaming / compound-renaming / exact / alpha). On ground-identical
pairs — the dominant case in construct-and-check, where the certificate carries
the exact branch formula — the arms massively overlap: alpha-equivalence is
reflexive (the alpha arm re-delivers every exact success), the binder arm's
rename-to-self is the identity on quantifier heads, and the compound arm
recurses matcho on subformulas with its own multiplicities, compounding
per node.

## Decision

Dispatch matcho on the visible formula's **head tag** (every checker call site
supplies a head-tagged branch formula), and for the **literal-family heads**
(`pos`, `neg`, `eq`, `neq`, boolean `true`/`false`) restrict the match to the
exact structural arm — which is **complete by itself** there:

- the binder/compound renaming arms require a quantifier/connective head, so
  they are structurally inapplicable to literal heads;
- below a literal head there is only first-order term structure (possibly
  `(var nom)`/`(par nom)` leaves but never a binder), and alpha-equivalence
  over an empty binding environment requires unmapped noms to be identical —
  so alpha coincides with structural equality, and the alpha arm can only
  re-deliver the exact arm's solution. This holds equally for a free or
  partially-instantiated proof-node formula (binder-free alpha has no freedom),
  so the restriction is **answer-set-identical in every mode**, including
  proof synthesis; only duplicate deliveries of identical states are removed.

**Structured heads** (`and`, `or`, `not`, `implies`, the five quantifier tags)
keep the original four-arm ladder in its original order, unchanged. The
dispatch is fully relational — one `lcons` head destructure plus
`static-table-entryo` over the two head tables; no `project`/`conda`/host cut,
matching the profile's §D purity constraint and the 1C precedent (pure
disjunction regrouping at the site of the domain knowledge).

## Measurement (within-JVM pairs; the honest protocol after two false attributions)

| measurement (same warm JVM, paired) | original matcho | dispatched | ratio |
|---|---:|---:|---|
| benchmark fuel 10 (ground-tree negative) | 15 926 ms | ~2 700–3 900 ms | **~4–5.9×** |
| benchmark fuel 80 | (fuel-flat) | ~2 500–3 200 ms | — |
| quantifier-arm pulls (`next-branch-nomo`) | 16 926 | 3 024 | 5.6× |
| alpha-arm pulls | 8 705 | 456 | 19× |
| beta-axiom synthesis (free proof) | 56.2 s | 54.1–58.6 s | **parity** |

Cumulative across ADR-0144+0145, the wall test that started this line
(`wrong-premise-leaves-the-universal-open`, ~106 s wall in-gate at ADR-0144's
start; ~14–20 s warm after 1C+1D) now completes in **~3 s warm**, still
correctly rejecting.

**Two false attributions, caught by protocol.** (i) An initial ~55 s
beta-axiom reading against a remembered ~31 s "baseline" suggested a synthesis
regression; two alternative mechanisms (relational-dispatch overhead; loss of
duplicate-delivery scheduler weight under `run 1` interleaving) were prototyped
and both *disproven* — the same-JVM original-matcho baseline also measured
56.2 s. The ~31 s figure came from a different JVM on a different day.
Cross-JVM comparisons of search runtimes are not evidence; only same-JVM
stash/alter-var-root pairs were accepted for this ADR's numbers.

## Consequences

- The ground construct-and-check mode no longer pays multiplicative
  reconciliation duplication per node; combined with ADR-0144's 1D this makes
  constructed-tree checking (ADR-0142's mode) interactive-speed at small scale.
- **Residual nondeterminism, out of scope here** (recorded for a future ADR if
  it becomes the binding cost): the checker's deliberate three-arm
  `forall`/`once-forall` (and two-arm `exists`) strategy ladder (~3 000
  residual quantifier-arm pulls on the benchmark — these arms produce
  *non-identical intermediate states* that converge, so pruning them needs a
  genuine partition proof, not a dedup argument); and proof-node-blind agenda
  selection (`sjas-proof-guided-selecto`, guarded against matcho coupling by an
  existing source audit).
- The dispatch requires matcho's visible formula to be head-tagged — true at
  every current call site (branch formulas from targets/agendas); a free
  visible formula would enumerate the 15 head tags where the old exact arm
  would have unified in one step. No current caller passes a free visible.
- Purity preserved: fully relational change; the host-peek recognizer variant
  (measured equivalent on the benchmark) was rejected in favor of the
  relational form once the synthesis "regression" it was meant to avoid was
  shown to be a measurement artifact.

## Test obligations

- Answer-set preservation: SJAS not-slow gate; fast suite minus
  `fitting-fidelity-test` plus that namespace isolated (the ADR-0144 lesson-5b
  landmine protocol); the profile source audit (131 assertions) green.
- Performance: the within-JVM paired table above; synthesis parity
  (`synthesizes-beta-axiom-citation`) explicitly re-measured against a
  same-JVM baseline.
