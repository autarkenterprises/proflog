# ADR-0109: D_SJAS Composite Proof-Object Internalization

- Status: completed
- Date: 2026-06-14
- Branch: `adr-0109-dsjas-composite-proof-object`
- AAR: [AAR-0109](../aar/AAR-0109-dsjas-composite-proof-object-internalization.md)

## Context

ADR-0108 proved quantitative EA-stability for `D_SJAS` using the selected
measure `Log_D_SJAS`. That measure is over formula-bearing proof-code `P` for
structural proofs, over the composite object `(S,F,P)` for tableau proof
citations, and over `(S,G,F,P)` for substitution-proof citations.

Before this ADR, the generated `SelfCons` sentences still quantified bare
proof-code variables. Tableau-0 used `tableau-proof(S,F,p)`. Level-1 used
`subst-prf(S,G,x,p)` and `subst-prf(S,G,y,q)`. This was not enough for the
EA-stability proof to apply to the implemented `IS#_{D_SJAS}(beta)` system,
because `p` and `q` did not carry the measured `S`/`G`/`F` payloads.

## Decision

Keep the public compatibility predicates:

```text
tableau-proof(S,F,P)
subst-prf(S,G,F,P)
```

Add measured internal predicates for generated SelfCons:

```text
dsjas-tableau-proof(S,F,C)
dsjas-subst-prf(S,G,F,C)
```

where `C` is a public proof-code term whose decoded proof payload is:

```text
(dsjas-tableau-proof-object S-bytes F-bytes P-bytes)
(dsjas-subst-prf-object S-bytes G-bytes F-bytes P-bytes)
```

The checker must read `C` through the same arithmeticized public-code readers
used for ordinary proof codes, decode the tagged proof payload relation over
bytes, verify its embedded bytes match the separate `S`, `G`, and `F` arguments,
and then delegate to the existing arithmeticized proof checker using the
embedded `P-bytes`.

Generated `SelfCons` sentences must quantify over composite proof-object codes:

```text
forall c. not dsjas-tableau-proof(S, contradiction, c)

forall x y c d.
  not pi-star-1-code(x) or
  not neg-pair(x,y) or
  not dsjas-subst-prf(S, skeleton, x, c) or
  not dsjas-subst-prf(S, skeleton, y, d)
```

## Consequences

- The proof variable quantified by SelfCons is now the object measured by
  `Log_D_SJAS`.
- Existing callers of `tableau-proof/3` and `subst-prf/4` remain compatible.
- Axiom-citation synthesis for the public predicate can remain as-is; measured
  synthesis may be added only if needed by focused tests.
- Formula-code bytes for Group-3 change, because the generated SelfCons
  relation symbols change.

## Implementation Notes

- Public `tableau-proof/3` and `subst-prf/4` remain available for compatibility.
- `dsjas-tableau-proof/3` and `dsjas-subst-prf/4` are declared in the SJAS
  language and generated into SelfCons.
- `dsjas-tableau-proof-object` and `dsjas-subst-prf-object` are proof-code
  payload symbols, classified as relevant proof-object accounting symbols by
  the correspondence audit.
- The measured predicate closures decode `C` through the arithmeticized public
  code readers, verify embedded bytes against the separate public predicate
  arguments, and then reuse the proof-free proof/substitution checker over the
  decoded proof bytes.

## Test Obligations

- Red tests must show generated Tableau-0 SelfCons uses
  `dsjas-tableau-proof`, not `tableau-proof`.
- Red tests must show generated Level-1 SelfCons uses `dsjas-subst-prf`, not
  `subst-prf`.
- Red tests must require host builders for composite proof-object codes.
- Red tests must require the measured predicates to accept an axiom citation
  whose composite object embeds the matching `S`, `F`, and `P` bytes.
- Red tests must require mismatched embedded theorem/system bytes to be rejected.
- Existing public `tableau-proof/3` and `subst-prf/4` tests must remain green.

## Exit Criteria

- SelfCons formulas quantify composite proof-object codes.
- The composite object relation is arithmeticized through public byte readers
  and proof-code decoding.
- Focused SJAS tests and correspondence tests pass.
- Fast and extended regression gates pass before commit.

## Evidence

Initial red tests:

```text
No such var: sjas/dsjas-tableau-proof-object

FAIL proof-symbol-audit-classifies-every-encoded-certificate-symbol
actual: #{dsjas-subst-prf-object dsjas-tableau-proof-object}
```

Focused green selectors included:

```text
dsjas-composite-tableau-proof-object-carries-measured-components
dsjas-tableau-proof-accepts-and-checks-composite-axiom-citations
dsjas-composite-subst-prf-object-carries-measured-components
dsjas-subst-prf-accepts-and-checks-composite-axiom-citations
sjas-tableau0-axiomconj-reconstructs-zero-one-selfcons-target
sjas-level1-group-three-uses-selfcons-skeleton-code
```

Final gates:

```text
lein test-proflog-fast
Ran 208 tests containing 1115 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 73 tests containing 219 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1078 fail=0 error=0
```

An accidental all-vars `lein test-proflog-sjas-focused` run was interrupted in
the known over-envelope slow selector
`sjas-subst-prf-checks-selfcons-fixed-point-certificate` after it crossed the
historical 45-minute envelope. The not-slow `lein test-proflog-sjas` gate is the
recorded SJAS gate for this ADR; slow fixed-point probes remain in the
documented slow-suite lane.
