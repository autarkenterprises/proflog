# ADR-0086: SJAS Tableau-0 Zero-One SelfCons Target

- Status: completed
- Date: 2026-06-09
- Branch: `adr-0073-sjas-correspondence-program`
- Parent: [ADR-0073](ADR-0073-sjas-internalization-correspondence-program.md)

## Context

ADR-0073 Track 1 completed an executable public `tableau-proof/3(s,t,p)`
demonstration for the ordinary-tableau Group-3 SelfCons certificate. That
implementation used the code of primitive `false` as the contradiction target:

```text
forall p. not tableau-proof(s, code(false), p)
```

The project research note from 2026-05-14 records a sharper literature
requirement. Willard's older minimal `IS(A)` Group-3 self-consistency statement
asserts that there is no semantic-tableau proof of `0 = 1` from the system
itself. Later `IS_D(beta)` / `IS#_D(beta)` / `ISTab-1(beta)` presentations
strengthen the target to Level-1 consistency: no simultaneous proofs of a
`Pi*1` sentence and its coded negation under the selected deductive apparatus.

Therefore primitive `false` is a useful Proflog tableau closure target, but it
is not the literature-compliant minimal Tableau-0 Group-3 target.

A second Track 1 boundary must remain explicit. The encoded public system code
`s` should identify the whole self-justifying system, including Group-3. It does
not have to serialize a literal copy of the full Group-3 formula inside the
finite beta payload. The literature's construction is fixed-point shaped: the
final group talks about Groups 0, 1, 2 plus "this sentence looking at itself."
In Proflog terms, `s` can be a finite descriptor only if the object relation
that consumes it reconstructs `AxiomConj(s)` as the fixed axioms, decoded finite
beta/reflected source, and the profile-determined Group-3 formula referring to
that same `s`. That reconstruction is part of Track 1, not a Track 2
correspondence fallback.

## Decision

Change the ordinary Tableau-0 contradiction target from primitive `false` to
the formula `0 = 1`.

Concretely:

- generated systems expose `:contradiction-code` as the public code of
  `(eq 0 1)`;
- Tableau-0 Group-3 formula construction uses that code;
- kernel-side Group-3 axiom membership and `AxiomConj(s)` reconstruction use
  the byte code for `(eq 0 1)`, not the primitive `false` byte;
- existing negative controls still reject a bare axiom citation of the
  contradiction target unless the finite system actually includes that formula
  as an axiom;
- the fixed-point/descriptor shape of `s` remains unchanged.

This is a semantic correction, not an optimization. It must preserve the
formula-bearing proof predicate and its relational purity.

## Success Criteria

- A focused red test fails on current source because Tableau-0
  `:contradiction-code` is still `code(false)` instead of `code(0 = 1)`.
- The builder, axiom-member citation path, and proof-side `AxiomConj(s)`
  reconstruction all agree on the `0 = 1` target after implementation.
- Existing SelfCons focused selectors pass with the revised target.
- `lein test-proflog-fast` and `lein test-proflog-extended` pass before the
  change is treated as complete.

## Failure Criteria

- Group-3 reconstruction uses host registries or host proof checking to recover
  the `0 = 1` target.
- `s` is changed into an ad hoc recursive byte payload that no longer has a
  finite descriptor interpretation.
- The correction weakens axiom membership by accepting the contradiction target
  as an axiom when it is not in the decoded finite basis.

## Result

Completed on 2026-06-09. The ordinary Tableau-0 builder now exposes
`:contradiction-code` as the public code for `0 = 1`, and kernel-side Group-3
reconstruction uses the same byte string for both compact and U-Grounding
embeddings. The finite descriptor interpretation of `s` is unchanged:
`AxiomConj(s)` still reconstructs fixed groups, decoded finite source, and the
profile-determined Group-3 sentence from the public code.

Red evidence before implementation:

```text
sjas-tableau0-selfcons-targets-zero-equals-one
Ran 1 tests containing 4 assertions.
3 failures, 0 errors.

sjas-tableau0-axiomconj-reconstructs-zero-one-selfcons-target
Ran 1 tests containing 3 assertions.
1 failures, 0 errors.
```

Focused green evidence after implementation:

```text
sjas-tableau0-selfcons-targets-zero-equals-one
Ran 1 tests containing 4 assertions.
0 failures, 0 errors.

sjas-tableau0-axiomconj-reconstructs-zero-one-selfcons-target
Ran 1 tests containing 3 assertions.
0 failures, 0 errors.

sjas-tableau-proof-accepts-formula-bearing-selfcons-certificate
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.
elapsed 8:29.61 maxrss 1961424KB
```

The broad gates passed:

```text
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
