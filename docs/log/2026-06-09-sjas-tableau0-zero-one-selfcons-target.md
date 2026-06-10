# SJAS Tableau-0 Zero-One SelfCons Target

Date: 2026-06-09

## Issue

The current ordinary Tableau-0 Group-3 implementation uses primitive `false` as
the contradiction theorem target:

```text
forall p. not tableau-proof(s, code(false), p)
```

The local literature research note from 2026-05-14 records a stricter target
for Willard's older minimal `IS(A)` form: no semantic-tableau proof of `0 = 1`
from the system itself. Later Level-1 systems use a stronger no-pair-of-proofs
statement over a relevant sentence class and its negation.

## Decision Link

ADR-0086 records the correction:

- Tableau-0 `:contradiction-code` must denote `0 = 1`;
- Group-3 reconstruction from `s` must use the same theorem code;
- `s` remains a finite descriptor whose `AxiomConj(s)` reconstruction includes
  Group-3 intensionally, rather than a literal recursive serialization of the
  full SelfCons formula in the beta payload.

## Internalization Note

This issue does not license a host shortcut in `AxiomConj`. The proof predicate
must still recover the axiom conjunction through object-code relations:

1. read the public system code into bytes;
2. decode finite beta formulas and reflected clauses from those bytes;
3. add fixed Group-0 and Group-1 formulas by profile;
4. synthesize the profile's Group-3 sentence from the same public system code.

This fixed-point `AxiomConj(s)` construction is a Track 1 obligation. It is not
enough to say that a later Track 2 correspondence proof might justify a bridge:
if the executable predicate does not accurately form the literature axiom basis
and SelfCons sentence, Track 1 is incomplete. Track 2 is reserved for studying
modified deductive apparatuses or variants and how those would be formalized as
different SJAS systems.

## Implementation Result

Implemented ADR-0086 on 2026-06-09. The public builder now generates
`:contradiction-code` for `(eq 0 1)`, and the kernel Tableau-0 Group-3
reconstruction embeds the same canonical formula-code bytes:

```text
[5 25 0 0 25 1 0 1]
```

The focused red tests failed before implementation:

```text
sjas-tableau0-selfcons-targets-zero-equals-one
Ran 1 tests containing 4 assertions.
3 failures, 0 errors.

sjas-tableau0-axiomconj-reconstructs-zero-one-selfcons-target
Ran 1 tests containing 3 assertions.
1 failures, 0 errors.
```

After implementation, the same selectors passed, the public formula-bearing
SelfCons certificate passed against the revised target, and the broad gates
passed:

```text
sjas-tableau-proof-accepts-formula-bearing-selfcons-certificate
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.
elapsed 8:29.61 maxrss 1961424KB

lein test-proflog-fast
Ran 167 tests containing 659 assertions.
0 failures, 0 errors.
elapsed 6:07.70 maxrss 463668KB

lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
elapsed 14:02.94 maxrss 576208KB

sjas-profile-source-audit-rejects-host-proof-checker-route
Ran 1 tests containing 128 assertions.
0 failures, 0 errors.

git diff --check
clean
```
