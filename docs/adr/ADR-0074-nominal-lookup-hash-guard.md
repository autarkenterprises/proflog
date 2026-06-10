# ADR-0074: Nominal Lookup Hash Guard

- Status: completed
- Date: 2026-05-27
- Branch: `adr-0073-sjas-correspondence-program`
- AAR: [AAR-0074](../aar/AAR-0074-nominal-lookup-hash-guard.md)

## Context

The αleanTAP paper uses nominal binders to avoid `copy_term/2`: universal
formula templates retain nominal variables, and tableau expansion substitutes
fresh logic variables through an environment keyed by noms. Core.logic's
nominal implementation documents the same model and includes `nom/hash` as the
freshness constraint: `a#t` means the nom `a` does not occur free in term `t`.

Core.logic's own changelog records `LOGIC-101`, "fix surprising behavior with
vars in nom/hash", in the 0.8.0-beta5 to 0.8.0-rc1 interval. The project uses
core.logic 1.0.1 by default and carries a 1.1.1 source overlay; both include the
post-LOGIC-101 nominal implementation. The remaining defect is not currently in
core.logic's `hash` relation. It is in local association-list lookup relations
that recurse past a nominal key without constraining the searched key to be
fresh for, and therefore distinct from, the skipped key.

The bug is visible only in relational modes. If the environment contains a
logic-variable key, an unguarded lookup can skip that binding, later instantiate
the skipped key to the searched key, and return a later binding from an
environment that has become duplicate-keyed. That violates the finite nominal
environment invariant used by αleanTAP substitution and by Proflog's relational
substitution/equality layers.

Core.logic's nominal test-suite example for typed lambda-calculus lookup uses
the missing shape:

```clojure
(nom/hash x xc)
```

before recursing past an environment binding keyed by `xc`.

## Decision

Every association-list lookup whose keys are object-language noms must guard
the recursive branch with nominal freshness against the skipped key:

```clojure
(fresh [skipped-key skipped-value rest]
  (== (lcons [skipped-key skipped-value] rest) env)
  (nominal/hash binding-nom skipped-key)
  (lookupo binding-nom rest value))
```

For the legacy αleanTAP namespaces that refer `hash` directly, the same guard is
written as `(hash a skipped-key)`.

This is stricter than ordinary constructor disequality in the relevant modes:
it preserves the invariant that environment keys are noms and keeps delayed
aliasing from making a skipped binding equal to the lookup key. Existing
`unboundo` relations already carry disequality constraints and are not the
source of this bug.

## Consequences

- The legacy αleanTAP-E and αleanTAP-EP substitution environments no longer
  allow a relational lookup to bypass a binding whose key later aliases the
  searched key.
- Greenfield substitution, equality, and first-order helper lookups follow the
  same nominal finite-map discipline.
- Core.logic itself is not patched for this issue unless a failing regression
  against `nom/hash` or `tie` unification is found. The current evidence is
  that the local bug is failure to use `nom/hash`, not incorrect implementation
  of that relation.
- The fix may prune previously reported relational answers that depended on
  duplicate-key environments. Those answers were outside the nominal
  environment invariant.

## Test Obligations

- Add red tests for αleanTAP-E, αleanTAP-EP, greenfield substitution,
  greenfield equality, and the first-order component showing that lookup returns
  only the first binding when a skipped logic-variable key is later unified with
  the searched key.
- Add a core.logic-facing regression showing that `nom/hash` rejects the
  corresponding delayed aliasing case, documenting that the underlying
  freshness primitive is sufficient.
- Run the focused failing tests before implementation and rerun them after the
  guard is added.
- Run the normal fast greenfield suite after the focused tests pass.
