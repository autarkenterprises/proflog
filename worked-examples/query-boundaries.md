# Query Boundaries

This file covers the expanded regressions in `test/proflog/query_extended_test.clj`.

The key point of this namespace is contractual, not cosmetic: the bounded
query helpers are operational probes over finite search slices. They are not
semantic oracles and they are not hard real-time promises.

## Bounded Success Probe Can Return No Result

Query:

```clojure
query-succeeds-within(p2-program, win(0), 1, 25)
```

Current result:

```clojure
()
```

This does not mean `win(0)` is false. It means no success proof was completed
within the admitted search slice.

## Bounded Failure Probe Can Return No Result

Query:

```clojure
query-fails-within(p2-program, win(1), 1, 25)
```

Current result:

```clojure
()
```

Again, this is a timeout-shaped operational result, not a semantic conclusion.

## Easy Proofs Still Surface

The same helpers do return proofs when the obligation is shallow enough:

```clojure
query-succeeds-within(status-program, p(0), 1, 500)
=> (neg-call (refl-close))

query-fails-within(status-program, p(1), 1, 500)
=> (pos-call (free-close))
```

So the helper contract is:

- return proofs when an easy slice succeeds,
- return `()` when the slice budget is exhausted,
- and never confuse that empty operational result with semantic falsity or
  semantic truth.
