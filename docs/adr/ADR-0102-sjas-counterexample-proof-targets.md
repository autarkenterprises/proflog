# ADR-0102: SJAS Counterexample and Corrected Proof Targets

- Status: completed
- Date: 2026-06-13
- Branch: `adr-0102-sjas-counterexample-proof-targets`
- AAR: [AAR-0102](../aar/AAR-0102-sjas-counterexample-proof-targets.md)

## Context

ADR-0101 found that ADR-0100 could not be demonstrated wholly as written. The
remaining ambiguity was whether ADR-0100 was merely underproved or explicitly
false. The user asked for an explicit counterexample, then for pursuit of both
corrected routes:

- **A:** narrow to a literal Willard semantic-tableau fragment;
- **B:** define and prove against an extended selected SJAS deductive apparatus.

## Decision

Add an executable counterexample showing that ADR-0100's stated proof-size claim
is false over its covered domain. The counterexample uses the accepted
`sjas-axiom` citation path, so it is not a hypothetical proof object outside the
implementation:

```text
P = fixed compact proof code for sjas-axiom
S = Tableau-0 system whose beta block contains a large formula F
F = (= (f^8 1) (f^8 1))
```

Written as the object formula used by the test:

```text
F =
(eq
  (app f
    (app f
      (app f
        (app f
          (app f
            (app f
              (app f
                (app f
                  (app 1)))))))))
  (app f
    (app f
      (app f
        (app f
          (app f
            (app f
              (app f
                (app f
                  (app 1))))))))))
```

The proof predicate accepts `(P,S,F)` by axiom membership, but `P` has only
three base-64 proof bytes, i.e. 18 bits, while the formula has `J = 18`
application/function-symbol occurrences and therefore requires at least
`5J = 90` bits under ADR-0100's stated lower-bound sentence.

Then split follow-up proof work into two non-overlapping tracks:

- [Path A: Narrow Literal-Willard Fragment](../log/2026-06-13-sjas-path-a-narrow-willard-fragment.md)
- [Path B: Extended D_SJAS Apparatus](../log/2026-06-13-sjas-path-b-extended-dsjas.md)

## Consequences

ADR-0100 as stated is refuted, not merely incomplete. A revised proof may still
succeed, but it must change the theorem target.

Path A is conservative and can likely prove a real theorem soon, but it does
not cover the self-referential SJAS proof machinery. Path B is the likely
literature-compliant long-term target, but it requires a formal selected
deductive apparatus and a proof that Willard's self-verification argument still
applies to that apparatus.

## Test Obligations

- Add a focused test that constructs the accepted `(P,S,F)` counterexample and
  verifies `size(P) < 5J(F)`.
- Keep the ADR-0100 focused selectors green.
- Keep the correspondence audit namespace green.

## Exit Criteria

- The executable counterexample test passes.
- A counterexample note records the exact witness and inequality.
- Path A and Path B notes define their theorem targets, required lemmas, and
  next implementation/proof tasks.
- ADR/AAR indexes and `LOG.md` record the result.
