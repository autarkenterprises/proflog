# ADR-0144: SJAS Multiplication-Boundary Comparison Implementation

- Status: accepted
- Date: 2026-06-23
- Branch: `adr-0144-sjas-multiplication-boundary-comparison`
- AAR: pending

## Context

ADR-0142 redirects the invalid ADR-0141 boundary-completion claim toward
Willard's semantic-tableau Second Incompleteness argument. Its subsequent
[interdeveloper review](../interdev/2026-06-22-adr-0142-review-and-corrections.md)
corrected the operative requirements: the relevant source is the JSL2 paper;
`Map`, `V4`, and `V5` are genuine parts of that paper; the existing
`semprfk-alpha` ignores its `K` argument; predicate names do not establish
Theorem 2.3 conditions (A)-(C); and a concrete multiplication result closes
only that Workstream B variant.

The ADR-0142 owner is implementing a revised design independently. This ADR
authorizes a comparison implementation from the completed ADR-0143 baseline.
It must not reuse unmerged implementation commits from the ADR-0142 worktree.

## Decision

Implement the total-multiplication boundary result as an ordinary,
formula-bearing semantic-tableau derivation checked by the existing SJAS proof
kernel. The implementation will use the JSL2 definitions and keep one exact
measured proof relation aligned across SelfCons, `SemPrf`, `SemPrfK`, the
diagonal sentence, and the final contradiction witness.

The work proceeds through these semantic obligations:

1. Implement `SemPrfK_alpha(x,y,z)` as `SemPrf_alpha(x,y)` together with
   `y < Log(z,K)`, where `Log(z,K)` is relational iterated binary logarithm and
   changing `K` can change acceptance.
2. Implement `Map(alpha,K,d)` as the checked relation identifying `d` with the
   code of the generated diagonal `DK(alpha,K)`. Compute the diagonal through
   structural `subst-code`; do not admit a host lookup or trusted conclusion.
3. Establish executable proofs or reflected, source-justified finite axioms for
   Theorem 2.3 conditions (A), (B), and (C), including universal substitution
   functionality rather than a single ground example.
4. Establish the arithmetic applicability bridge for the exact generated
   multiplication system. Operational arithmetic handlers alone do not count
   as proofs of the required universal Q or `W_D` consequences.
5. Construct the required proof composition as an expanded ordinary tableau,
   or as a separately tested transformation whose output is accepted by the
   ordinary structural checker and whose full formula-bearing proof object is
   measured.
6. Validate the resulting contradiction against the generated SelfCons code
   and reflected beta of the same system. Demonstrate the addition-only
   contrast at the arithmetic/bounded-proof obligation, not by profile-name
   inspection.
7. Run a separate synthesis path with witness and proof-code variables fresh at
   entry. It may use fair bounds and goal ordering, but no expected tuple or
   proof bytes.

No trusted boundary constructor, proof node, profile closure rule, host-side
certificate predicate, or caller-controlled completion metadata may establish
any of these obligations.

## Test And Acceptance Criteria

Tests must first fail against the ADR-0143 baseline and then establish:

- exact iterated-log boundary behavior for several `K` values, including
  equality and adjacent bounds;
- rejection of malformed, mismatched-system, and out-of-bound proofs;
- decoder-verified `Map` and diagonal round trips through `subst-code`;
- necessity of each condition (A)-(C) in the constructed ordinary tableau;
- proof-object measurement covering the entire composed tableau;
- multiplication closure and addition-only failure at the identified semantic
  obligation;
- an independently synthesized fresh tuple accepted by the same checker;
- canonical completion derived solely from those checked artifacts.

Focused selectors run first. Fast and extended suites run in parallel during
semantic work. Long synthesis probes write durable command, timing, tuple,
proof bytes, and decoded-tree evidence under `test-runs/`.

ADR-0144 is complete only when every criterion above is met. A correct
`SemPrfK` or `Map` implementation is necessary infrastructure, not completion.
Tab-2 and Xtab/LEM remain separate Workstream B obligations.

## Consequences

- The comparison branch can produce an independently reviewable proof design
  without merging or depending on the ADR-0142 agent's implementation.
- Existing inaccurate V-route specializations may be corrected or replaced
  only formula by formula against JSL2 Equations (12)-(16); genuine source
  machinery is not removed merely because an earlier ADR misdescribed it.
- Any failed theorem hypothesis, arithmetic bridge, proof-composition check, or
  fresh synthesis leaves the ADR open and must be recorded without relabeling
  partial infrastructure as boundary evidence.
