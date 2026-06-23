# ADR-0125: SJAS Total Multiplication Reduced Witness

- Status: completed
- Date: 2026-06-18
- Branch: `adr-0125-sjas-total-mul-reduced-witness`
- AAR: [AAR-0125](../aar/AAR-0125-sjas-total-mul-reduced-witness.md)

## Context

[ADR-0119](ADR-0119-sjas-next-research-roadmap.md) requires every Workstream B
negative variant to proceed in two stages: a reduced reflected-beta witness and
then the full generated SelfCons contradiction target. [ADR-0124](ADR-0124-sjas-boundary-variant-surface.md)
added the total-multiplication variant surface but deliberately kept the
reduced/full witness obligations open.

The Willard mechanism notes identify the first concrete destructive ingredient:
total multiplication permits short squaring-chain fragments. In the 2002
semantic-tableaux proof, Lemma 4.7 builds

```text
u_0 = 2
u_{i+1} = u_i * u_i
```

so a short proof fragment can name a witness whose binary length grows
exponentially in the fragment length. This is the compression step unavailable
when multiplication is only relational.

## Decision

Implement the reduced reflected-beta witness for the total-multiplication
variant as a finite squaring-chain fragment:

- `total-multiplication-squaring-chain-constants(depth)` returns
  `tm-u0 ... tm-u<depth>`;
- `total-multiplication-squaring-chain-axioms(depth)` returns the finite beta
  equations `tm-u0 = 2` and `tm-u(i+1) = mul(tm-ui, tm-ui)`;
- `total-multiplication-squaring-chain-summary(depth)` records the represented
  exponent and bit-length growth for the finite fragment;
- `total-multiplication-reduced-witness-options(depth)` returns the mergeable
  constants/functions/beta fragment;
- `total-multiplication-reduced-witness-system` builds the Level-1-compatible
  reduced witness system.

The reduced witness is not a proof of inconsistency and not the full generated
SelfCons target. It closes only the first Workstream B witness stage for the
total-multiplication variant: the reflected beta basis can now contain the
short squaring-chain compression fragment that later diagonal witness ADRs must
use.

## Consequences

- Workstream B now has a concrete, executable reduced witness for the first
  negative variant rather than only a variant surface.
- The total-multiplication audit can remove
  `:reduced-reflected-beta-witness` from open obligations while keeping the
  full SelfCons target, constructed certificate, and proof-search synthesis
  obligations open.
- The witness remains finite, Pi*1-admissible, and citeable through the same
  decoded `axiom-member/2` and `tableau-proof/3` paths as ordinary beta
  records.

## Test Obligations

Red first:

- the boundary audit still reports the reduced witness as open;
- squaring-chain helper tests fail because no public reduced witness builders
  exist.

Green after implementation:

- depth-3 constants are `tm-u0 ... tm-u3`;
- depth-3 squaring-chain axioms contain four finite beta equations;
- the axioms are Pi*1-admissible;
- the squaring-chain summary records bit-length growth larger than the number
  of equations;
- the reduced witness system declares `mul/2`, includes the chain constants,
  and contains the squaring-chain Group-2 records;
- the final chain record is visible through `axiom-member/2` and citeable by
  `tableau-proof/3` with `sjas-axiom`;
- adding the chain fragment changes encoded system identity and regenerated
  Group-3/SelfCons code compared with the same signature and only ADR-0124 seed
  beta;
- `lein test-proflog-fast`;
- `lein test-proflog-extended`;
- `lein test-proflog-sjas`.

## Exit Criteria

- The reduced reflected-beta witness stage is implemented for the
  total-multiplication variant.
- The Workstream B audit records that the full generated SelfCons contradiction
  target and synthesis evidence remain open.
- No final Goedel-boundary contradiction is claimed.
