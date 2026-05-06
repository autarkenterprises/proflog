# Legacy Subsumption Parity Examples

These examples document the focused ADR-40 suite in
`test/proflog/legacy_subsumption_test.clj`. They are intended as tutorial
material for non-trivial Proflog use cases that now have greenfield coverage
matching or exceeding the old legacy rows.

Run them with:

```text
timeout -k 5s 900s lein test-proflog-legacy-subsumption
```

Current result:

```text
Ran 3 tests containing 63 assertions.
0 failures, 0 errors.
elapsed 120.54 s
```

## Group Verifier Rows

The group examples use finite multiplication tables translated into ordinary
closed Proflog formulas. The identity law has the shape:

```clojure
(forall [x]
  (or (not-in-domain x)
      (and (op e x x)
           (op x e x))))
```

Closure uses a nested existential witness:

```clojure
(forall [x y]
  (or (not-in-domain x)
      (not-in-domain y)
      (exists [z]
        (and (op x y z)
             (in-domain z)))))
```

Inverses require a domain member `y` that multiplies with `x` to the identity
on both sides:

```clojure
(forall [x]
  (or (not-in-domain x)
      (exists [y]
        (and (in-domain y)
             (op x y e)
             (op y x e)))))
```

The ADR-40 test first reuses the exact `Z2` legacy probe scenarios, then
rebuilds the same laws over the larger cyclic group `Z3`. The successful proofs
must contain both `profiled` and `equality-fragment`, proving that they close
through the generic finite equality-fragment profile rather than a group-name
shortcut.

| Row | Outcome | Runtime |
|---|---|---:|
| `z2-identity` | succeeds | `856.145 ms` |
| `z2-closure` | succeeds | `77.389 ms` |
| `z2-inverses` | succeeds | `53.596 ms` |
| `z3 identity` | succeeds | `962.273 ms` |
| `z3 closure` | succeeds | `2059.279 ms` |
| `z3 inverses` | succeeds | `811.033 ms` |

## Finite-Domain Rows

The finite-domain examples encode category facts and invariants as ordinary
Proflog clauses over constructors. The legacy disjointness row asserts that
every value is either not warm-red or not one of the cool colors:

```clojure
(forall [x]
  (or (x != red)
      (and (x != green)
           (x != blue))))
```

The extended row checks a larger two-by-two disjointness condition over
`red`, `orange`, `green`, `blue`, and `yellow`.

Totality is intentionally different. The test asks whether every value is one
of a finite list of named constants. In open Proflog semantics, that query is
not closed just because the examples mention a finite-looking set; it remains
unresolved.

| Row | Outcome | Runtime |
|---|---|---:|
| `warm/cool disjoint` | success proof exists | `3.282 ms` |
| `extended finite-domain disjointness` | succeeds | `26.541 ms` |
| `finite totality is undefined` | unresolved | `2454.314 ms` |
| `extended finite totality is undefined` | unresolved | `3401.862 ms` |

Shortcoming: `warm/cool disjoint` has profiled proof evidence, but the bounded
two-sided `query-status` probe reports `:inconsistent` for this universal
encoding because the failure semidecision also finds a closure. The test records
that by checking the success proof directly.

## Peano Plus Rows

The Peano program matches the legacy PA definition: recursion is on the second
argument.

```clojure
plus(x, zero, x).
plus(x, s(y1), s(z1)) :- plus(x, y1, z1).
```

The direct forward rows run through the ordinary proof kernel:

| Row | Outcome | Runtime |
|---|---|---:|
| `PA10 forward 3 + 4 = 7` | succeeds | `70586.114 ms` |
| `PA11 / extended forward 4 + 3 = 7` | succeeds | `11456.428 ms` |

These timings explain the earlier probe asymmetry. A first ADR-40 fixture draft
accidentally recursed on the first argument, making `4 + 3 = 7` look worse than
`3 + 4 = 7`. After restoring the legacy second-argument recursion, `3 + 4 = 7`
is correctly the slower row because its second argument has four successors.

The open answer rows run through the constructor-recursive profile over the
compiled guarded clause. This still uses the translated Proflog formula
representation, but it is a profiled answer path rather than the default public
answer exporter.

| Row | Answer | Runtime |
|---|---|---:|
| `PA12 ? + 3 = 5` | `2` | `29.909 ms` |
| extended `? + 3 = 6` | `3` | `11.843 ms` |
| `PA13/PA15 3 + ? = 5` | `2` | `14.453 ms` |
| extended `3 + ? = 6` | `3` | `15.640 ms` |
| `PA14 3 + 4 = ?` | `7` | `7.692 ms` |
| extended `4 + 3 = ?` | `7` | `6.319 ms` |
| `PA16 x + x = 4` | `2` | `7.086 ms` |
| extended `x + x = 6` | `3` | `11.063 ms` |
| `PA17 x + x = 3` | no answer | `6.738 ms` |
| `PA18 / extended x + x = 5` | no answer | `10.846 ms` |
| `PA19 all pairs summing to 3` | `[0,3] [1,2] [2,1] [3,0]` | `9.105 ms` |
| extended all pairs summing to 4 | `[0,4] [1,3] [2,2] [3,1] [4,0]` | `11.712 ms` |
| `PA20 fixed addend 2` | includes `[0,2] [1,3] [2,4] [3,5]` | `50.322 ms` |
| extended fixed addend 3 | includes `[0,3] [1,4] [2,5] [3,6]` | `53.246 ms` |

Shortcoming: PA12 through PA20 are covered as constructor-recursive profiled
answer rows, not as a claim that raw default `query-answers` can cheaply
enumerate every Peano stream. The suite deliberately records that operational
boundary while still validating each accepted answer against the compiled
Proflog clause.
