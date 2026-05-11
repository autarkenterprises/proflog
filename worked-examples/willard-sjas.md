# Willard SJAS MVP Example

This example tracks `test/proflog/willard_sjas_test.clj` and the SJAS MVP substrate in `proflog.willard-sjas`.

Run the focused regression:

```text
lein test-proflog-sjas
```

Current result:

```text
Ran 9 tests containing 61 assertions.
0 failures, 0 errors.
real 30.95 s
```

## Hand-Written Intent

The MVP is not a full mechanization of Willard's consistency-preservation
metatheorem. It is the first executable SJAS-lang substrate:

```text
language:
  constants zero, one, two, three, four, six
  total function symbols add, dbl, pred, sub, div, max, log, root, count
  relation symbol mult(x, y, z), not a mul(x, y) function

user beta:
  one = one

reflected program clause:
  demo(x) :- x = one

external application clause:
  external-demo(x) :- x = zero
```

The reflected `demo` clause is part of the generated finite SJAS basis. It is
encoded as a Group-2b user extension and changes the system id and Group-3
self-consistency formula. The external clause is ordinary Proflog code around
the SJAS; it can be queried, but it is not listed in `axiom-member` and does
not change the `SelfCons` claim.

## Frontend Builder

The source-facing MVP builder accepts Clojure-readable prefix sections:

```clojure
(def source-system
  (sjas/system-source
    {:profile :willard-sjas-tableau0}
    (language
      (relations (demo 1)
                 (external-demo 1)))
    (beta
      (= one one))
    (reflected
      (|- (demo x)
          (= x one)))
    (external
      (|- (external-demo x)
          (= x zero)))))
```

The `language` section adds user-visible constants, functions, and relations to
the fixed SJAS U-grounding vocabulary. `beta` formulas lower through
`proflog.frontend/q`. `reflected` and `external` clauses lower through
`proflog.frontend/clauses`, preserving ordinary frontend helper inlining and
variable binding.

Use `(forall<= [x] bound body)` or `(exists<= [x] bound body)` in frontend
formulas; they compile to the same bounded AST nodes that `proflog.normalize`
lowers with relational `leq/2` guards.

The lower-level builder also accepts backend formulas and clauses directly,
which is useful for tests and generated examples:

```clojure
(require '[proflog.ast :as ast]
         '[proflog.normalize :as normalize]
         '[proflog.willard-sjas :as sjas])

(def beta
  (ast/eq-lit sjas/one sjas/one))

(def reflected-demo
  (ast/nom x
    (ast/clause 'demo [x]
      (ast/eq-lit (ast/var-term x) sjas/one))))

(def external-demo
  (ast/nom x
    (ast/clause 'external-demo [x]
      (ast/eq-lit (ast/var-term x) sjas/zero))))

(def system
  (sjas/system
    {:profile :willard-sjas-tableau0
     :relations {'demo 1
                 'external-demo 1}
     :beta [beta]
     :reflected-clauses [reflected-demo]
     :external-clauses [external-demo]}))
```

The builder supplies the pieces users should not hand-write:

- Group-Zero and Group-1 finite MVP axioms;
- Group-2 beta axiom entries;
- Group-2b reflected user-clause entries;
- stable formula-code constants;
- `axiom-member(system, formula-code)` clauses;
- miniature `tableau-proof/3` and `closed-branch/1` relations;
- Group-3 `SelfCons0` or `SelfCons1`.

## Generated Kernel Shape

The base language contains U-grounding function symbols and proof-coding
relations. Multiplication is a graph relation:

```clojure
(get-in sjas/tableau0-profile-language [:functions 'add])
;; => 2

(get-in sjas/tableau0-profile-language [:functions 'mul])
;; => nil

(get-in sjas/tableau0-profile-language [:relations 'mult])
;; => 3
```

A generated system stores an axiom list like:

```clojure
(map :group (:axioms system))
;; => (:group-zero :group-zero
;;     :group-one :group-one :group-one
;;     :group-two
;;     :group-two-b
;;     :group-three)
```

Each entry has a stable object-language code:

```clojure
(-> system :group-three :code)
;; => (app sjas_formula_group-three_0_...)
```

That code is available inside the program:

```clojure
(query/query-succeeds
  (:program system)
  (sjas/axiom-member (:system-code system)
                     (-> system :group-three :code))
  1
  64)
;; => one proof
```

## Formula Classes

Bounded quantifiers remain visible to the SJAS classifier before lowering:

```clojure
(ast/nom x
  (sjas/delta-star-0?
    (sjas/bounded-forall x sjas/two
      (sjas/lt (ast/var-term x) sjas/three))))
;; => true
```

NNF lowering then turns bounded quantifiers into ordinary quantifiers guarded
by the `leq/2` relation, so the kernel does not need a separate bounded
quantifier rule:

```clojure
(ast/nom x
  (normalize/to-nnf
    (sjas/bounded-forall x sjas/two
      (ast/pos-lit (ast/app-term 'p (ast/var-term x))))))
;; => forall x. (not leq(x, two)) or p(x)
```

Unbounded existential quantification under a universal is rejected as
Pi-star-1:

```clojure
(ast/nom x y
  (sjas/pi-star-1?
    (ast/forall-form x
      (ast/exists-form y
        (sjas/lt (ast/var-term y) (ast/var-term x))))))
;; => false
```

## Evaluation

Closed graph facts are ordinary Proflog relation queries:

```clojure
(query/query-succeeds
  (:program system)
  (sjas/mult sjas/two sjas/three sjas/six)
  1
  64)
;; => one proof
```

Answer mode can synthesize a missing multiplicand from the generated finite
`mult/3` graph:

```clojure
(ast/nom x
  (answers/query-answers
    (:program system)
    (sjas/mult (ast/var-term x) sjas/two sjas/four)
    [x]
    {:proof-limit 1
     :fuel 8}))
;; first binding => x = two
```

The selected proof profile is visible in proof evidence:

```clojure
(first
  (sjas/query-succeeds system beta
    {:proof-limit 1
     :fuel 64}))
;; => (profiled willard-sjas-tableau0 ...)
```

The Level-1 profile is selected the same way:

```clojure
(sjas/system
  {:profile :willard-sjas-level1
   :beta [beta]
   :reflected-clauses [reflected-demo]})
```

The MVP Level-1 profile uses plain semantic tableaux as the reflected deduction
method `D`. It does not claim Tab-1 theorem reuse.

## Certificate Predicate

The miniature certificate checker is relation-backed. A generated proof code of
the form `mini-closed(formula-code)` is accepted only when the formula is an
axiom member of the generated system:

```clojure
(let [code (-> system :group-three :code)]
  (query/query-succeeds
    (:program system)
    (sjas/tableau-proof (:system-code system)
                        code
                        (sjas/mini-closed-certificate code))
    1
    64))
;; => one proof
```

A malformed certificate does not succeed under the bounded rejection probe used
by the focused test:

```clojure
(let [code (-> system :group-three :code)]
  (query/query-succeeds
    (:program system)
    (sjas/tableau-proof (:system-code system)
                        code
                        (sjas/malformed-certificate code))
    1
    4))
;; => ()
```

## Bounded Contradiction Probe

The focused suite runs a Level-1 contradiction probe with fuel `4`. Current
result: `:not-found`, with duration recorded in the result map. This is an
implementation boundary check, not evidence that the generated system is
mathematically consistent.

## Shortcomings

- The MVP certificate relation is deliberately small. It accepts and rejects
  concrete miniature certificate shapes, but it is not a complete tableau proof
  checker.
- The Level-1 profile reflects plain semantic tableaux as `D`; Tab-1/proof-list
  theorem reuse is not implemented.
- U-grounding arithmetic examples are finite relation-backed graph facts, not a
  full library of Willard grounding-function algorithms.
- Passing bounded contradiction probes do not prove Willard's external
  consistency-preservation theorem.
