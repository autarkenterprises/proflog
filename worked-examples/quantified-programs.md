# Quantified Programs

This file covers `test/proflog/quantified_programs_test.clj`.

## Deeper `P1` Quantified Checks

The current greenfield quantified suite explicitly exercises the original
`forall`-based odd clause on deeper ground inputs:

```clojure
even(2) => succeeds
odd(0)  => fails
```

The `odd(0)` failure proof is:

```clojure
(pos-call
 (univ
  (split
   (neg-call (conj (neq-close (eq-bind))))
   (refl-close))))
```

Operationally:

1. the positive call opens the universal body,
2. the kernel instantiates it,
3. both branches of the resulting disjunction close.

## `zero-only`

The singleton program is:

```clojure
zero-only(x) :- forall y. (x != y or y = zero)
```

### `zero-only(0)` succeeds

Proof term:

```clojure
(neg-call
 (witness
  (conj
   (eq-step (par-bind) (refl-close)))))
```

The negated universal body chooses the contradictory witness `y = 0`, which
forces a reflexive equality.

### `zero-only(1)` fails

Proof term:

```clojure
(pos-call
 (univ
  (split
   (neq-close (eq-bind))
   (free-close))))
```

Choosing `y = 1` defeats the universal condition:

- the disequality branch closes because `x = y`,
- the equality-to-zero branch closes because `1 != 0`.

## `boxed-zero`

The mixed existential/universal example is documented in detail in
[boxed-zero.md](./boxed-zero.md).

The current open query result is:

```clojure
boxed-zero(x) => x = 0
```

with no residual obligations and explicit proof terms in the exported answer
record.

### Operational Note

The greenfield kernel keeps `boxed-zero` executable by representing the
negation of an existential clause body as an internal single-use universal
`once-forall` form. The justification is operational and local to the
greenfield prover: this branch obligation should instantiate once on the
current branch, not re-enqueue as an ordinary `forall` and spin away the
query.

## Current Boundary

This namespace proves that quantified clause bodies now execute directly in the
greenfield kernel, but it is still lighter than the legacy quantified
specification families such as:

- `sorted2`
- `subset`

Those are phase-2 and phase-3 targets on `ADR-0009`.

## `subset`

The greenfield subset family uses zero-arity relations over the finite domain
`{a, b, c}`:

```clojure
sub-ab-abc() :- forall x. ((x != a and x != b) or (x = a or x = b or x = c))
sub-abc-ab() :- forall x. ((x != a and x != b and x != c) or (x = a or x = b))
sub-a-a()    :- forall x. (x != a or x = a)
```

Current committed cases:

```clojure
sub-ab-abc() => succeeds
sub-abc-ab() => fails
sub-a-a()    => succeeds
```

This family matters because it exercises quantified finite-domain reasoning
without any recursive list structure. If it lands cleanly, it narrows the
remaining quantified gap to the still-blocked sortedness and graph-property
families rather than leaving quantified specifications absent altogether.
