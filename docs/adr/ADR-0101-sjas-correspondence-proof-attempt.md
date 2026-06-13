# ADR-0101: SJAS Correspondence Proof Attempt Audit

- Status: completed
- Date: 2026-06-13
- Branch: `adr-0101-sjas-correspondence-proof-audit`
- AAR: [AAR-0101](../aar/AAR-0101-sjas-correspondence-proof-attempt.md)

## Context

ADR-0100 claimed a complete direct-examination proof of the Track 2b
correspondence theorem over the first fragment:

```text
ProflogAccepts(P,S,F) iff SemPrf_D(decode(P),S,F)
```

with Willard's Conventional Tableaux Encoding lower bound, stated as at least
`5J` bits for `J` function-symbol occurrences. A follow-up review in
`docs/interdev/2026-06-13-adr-0100-review-corroboration.md` found that the
claim was narrower than the text said: the focused tests passed, but the
proof-table was not exhaustive and the anti-compression argument did not yet
prove the general lower bound.

The user then requested an independent attempt to prove the claim wholly.

## Decision

Attempt the proof against the literal ADR-0100 target before doing any kernel
changes. The attempt must:

- audit every `sjas-structural-proof-check-state-decodedo` branch that can
  consume a formula-bearing structural proof node;
- distinguish direct Willard `D` rules from branch bookkeeping, selected
  equality/arithmetic theory steps, reflected-call expansion, recursive
  `tableau-proof/3`, and `subst-prf/4`;
- test whether the proof-size lower bound can be repaired over the actual
  first-fragment proof-code grammar;
- record whether the whole theorem can be demonstrated as written.

This ADR makes no implementation change to the kernel or proof checker. The
artifact is the proof attempt and its result.

## Consequences

The proof attempt shows that ADR-0100 cannot be demonstrated wholly as written.
The directly formula-bearing propositional/quantifier part is credible, and the
structural formula-byte lower bound can be repaired for genuine structural
tableau proof trees. The full theorem still fails for two reasons:

1. Several accepted checker branches are not literal Willard `D` rules. They
   require an explicitly selected extended apparatus, bounded macro expansions,
   or exclusion from the theorem.
2. The bare `sjas-axiom` citation in the covered domain is fixed-size proof
   code while the cited axiom formula can be large through `S` and `F`. The
   `>=5J` lower bound therefore cannot be a property of `P` alone for the
   domain ADR-0100 quantified over.

The correct next step is not optimization. It is a revised proof target:

- either narrow Track 2b to non-axiom formula-bearing structural proof trees
  and exclude/replace the bare citation;
- or define a selected SJAS deductive apparatus that admits equality,
  arithmetic, reflected calls, `tableau-proof/3`, and `subst-prf/4` as
  primitives or proved macros, with proof-size accounting over the proof object
  plus any theorem/system payload counted by the literature-compliant encoding.

## Test Obligations

Because this ADR changes documentation and proof status rather than runtime
behavior, its operational obligations are regression checks:

- rerun the two ADR-0100 focused selectors;
- rerun `proflog.sjas-correspondence-test`;
- run `git diff --check`.

## Exit Criteria

- A proof-attempt note records the exhaustive clause audit, the partial proof,
  and the blockers.
- AAR-0100 is updated with the later refutation/correction.
- ADR/AAR indexes and `LOG.md` point to the new result.
- Focused regression checks pass.
