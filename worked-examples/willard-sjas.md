# Willard SJAS Binary Profile Example

This example documents ADR-0058 through ADR-0061 and the focused regression in
`test/proflog/willard_sjas_test.clj`. It demonstrates the Willard-style SJAS
builder, binary U-grounding arithmetic, reflected axiom codes, and
kernel-checked proof certificates.

Run the focused regression:

```text
lein test-proflog-sjas
```

Current result:

```text
Ran 11 tests containing 110 assertions.
0 failures, 0 errors.
real 51.22 s
```

## Hand-Written Intent

The object language now uses binary numeral constants:

```text
language:
  constants 0, 1
  total function symbols add, dbl, pred, sub, div, max, log, root, count
  relation symbols mult(x, y, z), leq(x, y), lt(x, y)

user beta:
  1 = 1

reflected program clause:
  demo(x) :- x = 1

external application clause:
  external-demo(x) :- x = 0
```

Larger numerals are terms, not constants. For example, the Clojure helper
`sjas/three` builds this object-language term:

```clojure
(sjas/numeral 3)
;; => (app add (app dbl (app 1)) (app 1))
```

The reflected `demo` clause is part of the generated finite SJAS basis. It is
encoded as a Group-2b user extension and changes the system id and Group-3
self-consistency formula. The external clause is ordinary Proflog code around
the SJAS; it can be queried, but it is not listed in `axiom-member` and does
not change the `SelfCons` claim.

## Frontend Builder

The source-facing builder accepts Clojure-readable prefix sections. Numeric
literals in this SJAS frontend lower to same-spelled object constants:

```clojure
(def source-system
  (sjas/system-source
    {:profile :willard-sjas-tableau0}
    (language
      (relations (demo 1)
                 (external-demo 1)))
    (beta
      (= 1 1))
    (reflected
      (|- (demo x)
          (= x 1)))
    (external
      (|- (external-demo x)
          (= x 0)))))
```

The lower-level builder also accepts backend formulas and clauses directly:

```clojure
(require '[proflog.ast :as ast]
         '[proflog.query :as query]
         '[proflog.willard-sjas :as sjas])

(def beta
  (ast/eq-lit sjas/one sjas/one))

(def reflected-demo
  (ast/nom x
    (ast/clause 'demo [x]
      (ast/eq-lit (ast/var-term x) sjas/one))))

(def system
  (sjas/system
    {:profile :willard-sjas-tableau0
     :relations {'demo 1}
     :beta [beta]
     :reflected-clauses [reflected-demo]}))
```

The builder supplies stable formula-code constants, generated
`axiom-member(system, formula-code)` facts, Group-Zero through Group-3 records,
and the compiled program with the selected SJAS proof profile.

## Generated Kernel Shape

The base language contains only `0` and `1` as numeral constants:

```clojure
(contains? (:constants sjas/tableau0-profile-language) (symbol "0"))
;; => true

(contains? (:constants sjas/tableau0-profile-language) 'two)
;; => false
```

Multiplication is a relation, not a function:

```clojure
(get-in sjas/tableau0-profile-language [:functions 'mul])
;; => nil

(get-in sjas/tableau0-profile-language [:relations 'mult])
;; => 3
```

A generated system stores reflected axiom records:

```clojure
(map :group (:axioms system))
;; => (:group-zero :group-zero
;;     :group-one :group-one :group-one
;;     :group-two
;;     :group-two-b
;;     :group-three)
```

Group-1 arithmetic records are still reflected and code-addressable, but the
profile now treats U-grounding arithmetic as theory behavior. The theorem
helper therefore does not place Group-1 arithmetic equalities into every
ordinary branch as free-constructor equalities.

## Arithmetic Evaluation

Closed arithmetic equations route through the SJAS profile:

```clojure
(query/query-succeeds
  (:program system)
  (ast/eq-lit (sjas/add-term (sjas/numeral 2)
                             (sjas/numeral 3))
              (sjas/numeral 5))
  1
  160)
;; => one profiled proof
```

The focused suite covers the U-grounding functions:

```text
add(2,3) = 5
dbl(6) = 12
pred(0) = 0
pred(5) = 4
sub(2,5) = 0
sub(7,3) = 4
div(7,0) = 7
div(7,3) = 2
max(4,9) = 9
log(1) = 0
log(8) = 3
root(10,2) = 4
root(8,3) = 2
count(13,4) = 3
```

It also checks graph/order relations beyond the old finite MVP facts:

```clojure
(query/query-succeeds
  (:program system)
  (sjas/mult (sjas/numeral 4) (sjas/numeral 3) (sjas/numeral 12))
  1
  160)
;; => one proof

(query/query-succeeds
  (:program system)
  (sjas/mult (sjas/numeral 4) (sjas/numeral 3) (sjas/numeral 11))
  1
  80)
;; => ()
```

Answer mode and partial synthesis use the SJAS answer wrapper so the answer
overlay receives the same profile arithmetic hook:

```clojure
(ast/nom x
  (sjas/query-answers
    system
    (sjas/mult (ast/var-term x) (sjas/numeral 3) (sjas/numeral 12))
    [x]
    {:proof-limit 1
     :fuel 160}))
;; first binding => x = (app dbl (app dbl (app 1)))

(ast/nom z
  (sjas/query-answers
    system
    (ast/eq-lit (sjas/add-term (ast/var-term z) (sjas/numeral 3))
                (sjas/numeral 7))
    [z]
    {:proof-limit 1
     :fuel 160}))
;; first binding => z = 4
```

## Certificate Predicate

`tableau-proof/3` no longer accepts a miniature `mini-closed` placeholder. A
certificate is a structural object-language encoding of a Proflog kernel proof
term:

```clojure
(let [beta-record (first (filter #(= :group-two (:group %)) (:axioms system)))
      beta-proof (first
                   (sjas/query-succeeds system
                                        (:formula beta-record)
                                        {:proof-limit 1
                                         :fuel 96}))
      certificate (sjas/proof-certificate beta-proof)]
  (query/query-succeeds
    (:program system)
    (sjas/tableau-proof (:system-code system)
                        (:code beta-record)
                        certificate)
    1
    160))
;; => one proof
```

The same certificate is rejected for the wrong theorem code, and an unrelated
`refl-close` certificate is rejected for the beta theorem. Internally the
profile decodes the proof-code term and calls `kernel/prove-programo` with that
decoded proof supplied as the proof term, so the checker reuses the existing
pure relational tableau kernel instead of a host-side proof oracle.

## Bounded Contradiction Probe

The focused suite keeps the Level-1 contradiction probe bounded and concrete.
It checks that a chosen certificate candidate does not prove the generated
contradiction code and records the duration in the result map:

```clojure
(sjas/bounded-contradiction-probe system {:fuel 4 :proof-limit 1})
;; => {:result :not-found, :fuel 4, :proof-limit 1, :duration-ms ...}
```

Open proof-code synthesis remains a harder extended search problem. The focused
probe is a regression guard for the checker boundary, not evidence of Willard's
external consistency-preservation metatheorem.

## Shortcomings

- The proof-code encoding covers the current Proflog kernel proof-term language
  used by these examples. It is not a byte-for-byte formalization of every
  historical Willard proof-list encoding.
- Tab-1/proof-list theorem reuse is not implemented or claimed. The Level-1
  profile reflects plain semantic tableaux as the deduction method `D`.
- The arithmetic profile is relational, but some reverse modes are still
  operationally expensive. Slow open certificate synthesis is deliberately not
  in the focused suite.
- Passing bounded contradiction probes do not prove Willard's external
  consistency-preservation theorem.
