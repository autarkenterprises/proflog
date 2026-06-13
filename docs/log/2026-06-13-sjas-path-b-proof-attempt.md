# Path B Verdict: Extended `D_SJAS` Apparatus

Date: 2026-06-13

ADR: [ADR-0103](../adr/ADR-0103-sjas-proof-attempts-a-b.md)

Predecessor: [Path B target](2026-06-13-sjas-path-b-extended-dsjas.md)

## Result

Path B is complete as a negative Track 2b result for the current accepted
domain.

The current implementation cannot be honestly claimed to correspond to literal
Willard `D`. The branch inventory contains rule families that are not literal
Willard semantic-tableau rules, and ADR-0102 supplies a concrete accepted
fixed-size `sjas-axiom` citation that violates the stated proof-size bound when
the proof object is only `P`.

Therefore:

```text
CurrentProflogAccepts(P,S,F) iff SemPrf_D(decode(P),S,F)
```

is impossible over the current accepted domain.

## Extended Rule Families

The executable inventory still records a plausible future `D_SJAS` apparatus:

- `:base-tableau`;
- `:branch-bookkeeping`;
- `:truth-normalization`;
- `:quantifier`;
- `:equality-theory`;
- `:arithmetic-profile`;
- `:axiom-membership`;
- `:reflected-call`;
- `:recursive-proof`;
- `:substitution-proof`.

The last six families go beyond literal Willard `D` and must be selected as
part of a different apparatus before a positive proof can be stated.

## Blocking Reasons

The conclusive blockers for literal Track 2b are:

- `:non-willard-extended-rule-families`: equality theory, arithmetic/profile
  closure, axiom membership, reflected calls, recursive proof, and substitution
  proof are not literal Willard `D` rules.
- `:sjas-axiom-citation-size-counterexample`: ADR-0102's accepted citation has
  fixed proof-code size while the cited formula grows through `S` and `F`.

The required proof-object accounting repair is:

```text
:formula-bearing-axiom-leaf-or-combined-object-required
```

That is, a future positive theorem must either replace bare citations with
formula-bearing axiom leaves or explicitly count a combined proof object
containing the required theorem/system payload.

## Executable Evidence

The verdict API is:

```text
correspondence/audit-path-b-correspondence-verdict
```

The focused test is:

```text
path-b-extended-apparatus-proof-has-conclusive-track-2b-verdict
```

It passed as part of:

```text
lein test proflog.sjas-correspondence-test
Ran 30 tests containing 407 assertions.
0 failures, 0 errors.
```

## Track 2c Handoff

A positive `D_SJAS` result is still possible, but it is a different theorem:

```text
CurrentProflogAccepts(P,S,F) iff SemPrf_D_SJAS(translate(P,S,F),S,F)
```

That theorem requires a new ADR that defines `D_SJAS` mathematically, chooses
the citation size-accounting repair, and proves literature admissibility for
the selected apparatus.
