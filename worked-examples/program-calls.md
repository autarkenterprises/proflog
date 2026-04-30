# Program Calls

This file covers the expanded regressions in `test/proflog/program_test.clj`.

## Multi-Argument Calls Respect The Clause Environment

Program:

```clojure
pair-eq(x, y) :- x = y
```

Two representative queries now have explicit closure proofs:

```clojure
pair-eq(zero, one)      => (pos-call (free-close))
not pair-eq(zero, zero) => (neg-call (refl-close))
```

The positive call closes because the clause body reduces to `zero = one`,
which fails by constructor clash. The negative call closes because the clause
body reduces to `zero = zero`, which succeeds reflexively.

## Subsidiary Tableaux Stay Isolated

Program:

```clojure
p(x) :- q(x)
```

Caller query:

```clojure
p(zero) and not q(zero)
```

Current result:

```clojure
()
```

There is no proof. The caller branch already contains `not q(zero)`, but that
literal does not get borrowed into the subsidiary tableau opened for the call
to `p(zero)`. This is the intended semantics: procedure calls are checked
against the compiled clause body, not against arbitrary caller-branch context.

## Boundary

The namespace also keeps the complementary negative examples:

- a plain positive call does not close when its clause body remains satisfiable,
- a plain negative call does not close when the clause body remains open,
- and compiled clause lookup still fails cleanly when no matching relation
  exists.
