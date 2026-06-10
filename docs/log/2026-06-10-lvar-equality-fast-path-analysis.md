# LVar Equality Fast Path: Stack Analysis And Proposal

Date: 2026-06-10

## Question

While the uncapped durable probe for the two `subst-prf` negative-exhaustion
selectors was running (3.5+ CPU-hours at the time), the user asked: does JVM
stack analysis of the running probe give insight for a new performance
optimization?

## Profile

Three `jstack` samples of the probe JVM, seconds apart, all landed in the
same innermost loop: `occurs-check-worklist` under `ext`/`unify`, doing
per-node substitution lookups over lvar-dense candidate terms. Two cost
centers recur:

- `Substitutions.walk` performing a `PersistentHashMap` `find` per visited
  node (necessary work: the negative search enumerates candidates full of
  fresh variables, so most nodes are bindable);
- `LVar.equals` executing through
  `LVar.valAt -> KeywordLookupSite -> Keyword.hashCode -> Symbol.hashCode`
  — keyword-lookup indirection inside the engine's hottest comparison.

The ADR-0090 ground-term fast path cannot help here: it removes work for
ground terms, and these terms are the exact complement — variable-dense.
This is also consistent with the 2026-05-01 ADR-0032 count probe
(5.3M `walk*`, 1.66M `occurs-check` calls on one list-family row): the same
loop dominates lvar-dense workloads generally.

## What the original code does

```clojure
(deftype LVar [id unique name oname hash meta]
  ...
  (equals [this o]
    (if (instance? IVar o)
      (if unique
        (identical? id (:id o))
        (identical? name (:name o)))
      false))
  (hashCode [_] hash))
```

The logical contract: two variables are equal when both are `IVar`s and,
for ordinarily constructed variables (`unique` true), their `id` fields are
the same object; for name-constructed variables (`unique` false), their
`name` fields are the same object. `hashCode` returns a field cached at
construction, so hashing is cheap; equality is where the cost hides.

The cost is in how `(:id o)` executes inside the method: a keyword
invocation compiles to a `KeywordLookupSite`; each call goes callsite
indirection -> virtual dispatch into `LVar.valAt` (LVar explicitly
implements `ILookup`) -> the `case k` inside `valAt`, which dispatches by
hashing the keyword (the `Keyword.hashCode`/`Symbol.hashCode` frames seen
live) -> branch -> field. Four layers of machinery to read a field the
class itself declares, on every equality test: every substitution-map
`find` key comparison, the occurs-check worklist's `(= (walk s v) u)`,
`unify`'s var-var fast path, the constraint store.

## The proposed modification

Insert a type-hinted fast path for the dominant LVar-vs-LVar case, keeping
the keyword-lookup branch verbatim as the fallback for any non-LVar `IVar`
(the nominal subsystem's variable types), so cross-type comparisons are
untouched:

```clojure
(equals [this o]
  (if (instance? LVar o)
    (let [o ^LVar o]
      (if unique
        (identical? id (.-id o))
        (identical? name (.-name o))))
    (if (instance? IVar o)
      (if unique
        (identical? id (:id o))
        (identical? name (:name o)))
      false)))
```

`(.-id o)` on a hinted `^LVar` compiles to checkcast + getfield —
JIT-inlinable, no callsite, no `valAt`, no keyword hashing.

Semantics are identical by construction: `LVar.valAt` is defined by the
case table `:id -> id`, `:name -> name`, so for any `LVar` argument the
fast path reads the same objects the fallback would return, and
`identical?` over the same objects yields the same booleans. Equality
results — and therefore substitution lookups, occurs-check outcomes,
unification, disequality stores, and tabling keys — are bit-for-bit
unchanged. The only observable difference is cycles.

## Expected effect and boundaries

A constant-factor optimization of cost per comparison, complementary to
ADR-0090 (which eliminated comparisons for ground terms): 0090 removes the
work when terms contain no variables; this cheapens the work that remains
when terms are full of them. Beneficiaries are exactly the workloads 0090
could not touch: the `subst-prf` negative exhaustions and the historic
lvar-dense list families. It deliberately does not change asymptotics —
the negative searches are wide because the diagonal-substitution failure
space is wide, which is Track 2a apparatus/scheduling territory. The
deeper representational alternative (integer-id-keyed substitutions) is
shelved as upstream-divergent and inelegant.

Implemented as [ADR-0094](../adr/ADR-0094-core-logic-lvar-equality-fast-path.md)
(ADR-0093 was claimed by the parallel core.logic canonical-regression-suite
agent). The durable probe continues in parallel, reniced to priority 19,
supplying the uncontaminated pre-change envelope.
