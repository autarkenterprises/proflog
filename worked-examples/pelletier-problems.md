# Pelletier Problems

ADR-0022 ports the upstream `namin/leanTAP` Pelletier benchmark through the
greenfield kernel as pure theorem proving. The test helper builds this branch:

```clojure
(normalize/to-nnf
  (conjoin (concat axioms [(ast/not-form theorem)])))
```

With no axioms, this is just the NNF negation of the theorem. With axioms, it
is the NNF of `axiom-1 and ... and axiom-n and not(theorem)`.

## Propositional Example: Problem 1

Upstream:

```clojure
(<=> (=> p q) (=> (not q) (not p)))
```

Greenfield builder:

```clojure
(iff (implies p q)
     (implies (not* q) (not* p)))
```

The helper expands `iff` as both implications, negates the theorem, and gives
the kernel a closed branch equivalent to:

```clojure
(or (and (or (neg p) (pos q))
         (and (neg q) (pos p)))
    (and (or (pos q) (neg p))
         (and (pos p) (neg q))))
```

This matches the shape already mirrored by the legacy
`test/cljtap/alphaleantap_ep_test.clj` slice, but runs through
`proflog.kernel/prove` rather than the legacy `cljtap` prover.

## Quantified Example: Problem 18

Upstream:

```clojure
(E y (A x (=> (f y) (f x))))
```

Greenfield builder:

```clojure
(ast/nom y x
  (exists y
          (forall x
                  (implies (pred 'f (v y))
                           (pred 'f (v x))))))
```

The proof branch is the normalized negation of that theorem. The greenfield
kernel closes it using its ordinary quantifier, branch-literal, and equality
state machinery. No Proflog program clauses are present.

## Current ADR-0022 Status

- `ported-passing`: Problems 1-11 and 13-20.
- `ported-too-slow`: Problem 12. Its formula is ported, but no proof was found
  within a 120 second fresh-process probe.
- `not-yet-ported`: Problems 21-46.

Dedicated selectors keep these tiers separate:

```bash
lein test-proflog-pelletier-prompt
lein test-proflog-pelletier
lein test-proflog-pelletier-exploratory
```
