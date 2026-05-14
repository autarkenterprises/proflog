# Willard SJAS Base-64 Coding and Substitution-Proof Profile Example

This example documents ADR-0058 through ADR-0065 and the focused regression in
`test/proflog/willard_sjas_test.clj`. It demonstrates the Willard-style SJAS
builder, binary U-grounding arithmetic, reflected axiom Godel-code terms, and
kernel-checked proof certificates. ADR-0062 made the self-consistency
demonstration non-vacuous; ADR-0063 then removed the remaining host-side
proof-target table by making `tableau-proof/3` consume compact base-64 code
terms rather than opaque formula labels. ADR-0064 adds the Level-1
`subst-prf/4` predicate, so generated `SelfCons1` formulas now use the
substitution-proof vocabulary rather than raw `tableau-proof/3`. ADR-0065
corrects the fixed-point substitution argument: Level-1 Group-3 now cites the
code of its own `Gamma_1(g)` skeleton, not the system code.

Run the focused regression:

```text
lein test-proflog-sjas
```

Current result:

```text
Ran 20 tests containing 162 assertions.
0 failures, 0 errors.
real 406.83 s
```

The explicit slow fixed-point selector is also available:

```text
lein test-proflog-sjas-slow
;; Ran 1 tests containing 3 assertions.
;; real 82.81 s
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
         '[proflog.frontend :as frontend]
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

The builder supplies stable formula-code terms, generated
`axiom-member(system, formula-code)` facts, Group-Zero through Group-3 records,
and the compiled program with the selected SJAS proof profile. A code is not a
hash-like constant; it is a first-order term of the shape
`(code-N b0 ... bN-1)`, where each `bi` is one base-64 byte written as a small
binary SJAS numeral.

## Query-Triggered Evaluation

Constructing an SJAS system does not itself search for proofs. It builds a
finite reflected theory plus an executable Proflog program. Evaluation begins
only when the caller asks a query.

A reflected-only program makes the user clause part of the SJAS basis:

```clojure
(def reflected-only
  (sjas/system-source
    {:profile :willard-sjas-tableau0}
    (language
      (relations (demo 1)))
    (beta
      (= 1 1))
    (reflected
      (|- (demo x)
          (= x 1)))))

(frequencies (map :group (:axioms reflected-only)))
;; => {:group-zero 2,
;;     :group-one 3,
;;     :group-two 1,
;;     :group-two-b 1,
;;     :group-three 1}
```

The system value above is only compiled data. A direct executable query starts
the ordinary Procedure Call Rule over the compiled program:

```clojure
(query/query-succeeds
  (:program reflected-only)
  (frontend/q (demo 1))
  1
  96)
;; => one proof
```

The SJAS theorem helper starts a different query: it asks the kernel to prove
the formula from the generated SJAS axiom basis. Because `demo` was reflected,
the same source clause is also represented as a Group-2b axiom formula.

```clojure
(sjas/query-succeeds
  reflected-only
  (frontend/q (demo 1))
  {:proof-limit 1
   :fuel 96})
;; => one proof
```

An external clause is still executable Proflog, but it is not part of the
reflected SJAS axiom basis:

```clojure
(def with-external
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

(= (:system-code reflected-only) (:system-code with-external))
;; => true

(= (:code (:group-three reflected-only))
   (:code (:group-three with-external)))
;; => true
```

The external clause does not change the system code or the generated Group-3
self-consistency claim. It is still queryable as ordinary Proflog context:

```clojure
(query/query-succeeds
  (:program with-external)
  (frontend/q (external-demo 0))
  1
  96)
;; => one proof
```

It may also be used by ordinary procedure-call reasoning during an SJAS-wrapped
query, but it is not citeable by the internal `tableau-proof/3` predicate as an
`axiom-member` of this SJAS:

```clojure
(sjas/query-succeeds
  with-external
  (frontend/q (external-demo 0))
  {:proof-limit 1
   :fuel 96})
;; => one proof
```

## Group-2b Restrictions

Users do not add arbitrary Group-2b formulas directly in the current frontend.
They add `reflected` clauses, and the builder converts each clause

```text
head :- body
```

into the universally closed axiom formula:

```text
forall parameters. body -> head
```

Those reflected clauses are subject to the ordinary Proflog clause restrictions:

- the reflected extension is finite at system-construction time;
- clause heads are relation calls with variable-only parameters;
- function, constant, and relation symbols must be declared in the SJAS
  language or supplied by the SJAS base signature;
- arities must match their declarations;
- internal proof parameters such as `par` are not admissible in user source;
- source-level `:=` helpers may be used only as non-recursive inline
  abbreviations before a real `|-` clause is emitted.

The implementation does not currently prove that a reflected Group-2b clause is
true, conservative, or consistent before admitting it. It is a trusted finite
extension of the reflected system. Adding a false or explosive reflected clause
changes the theory, changes Group-3, and can make later queries succeed for the
wrong reason.

For Willard-aligned examples, reflected Group-2b clauses should therefore be
small, explicit, and kept within the intended arithmetic/proof-coding fragment.
Broader admissibility checks, such as rejecting nonconforming reflected
extensions before Group-3 generation, are a future hardening step rather than a
current guarantee.

## Composite: Beta Axiom vs Reflected Procedure

The `composite` examples below use a deliberately small witness definition:

```text
forall x. mult(2, 2, x) -> composite(x)
```

This proves that `4` is composite without turning the example into an open
factorization benchmark. The mathematically broader definition
`exists y z. y != 1 and z != 1 and mult(y,z,x)` is expressible, but the current
profile does not close that general factor-synthesis proof within the focused
test budget; an exploratory 120 s wrapper produced no result for that broader
form.

First, put the definition in Group 2 `beta`:

```clojure
(def beta-composite-system
  (sjas/system-source
    {:profile :willard-sjas-tableau0}
    (language
      (relations (composite 1)))
    (beta
      (forall [x]
        (implies
          (mult (dbl 1) (dbl 1) x)
          (composite x))))))

(frequencies (map :group (:axioms beta-composite-system)))
;; => {:group-zero 2,
;;     :group-one 3,
;;     :group-two 1,
;;     :group-three 1}
```

The theorem-level query succeeds because the Group 2 axiom can be used in the
generated SJAS basis:

```clojure
(sjas/query-succeeds
  beta-composite-system
  (frontend/q (composite (dbl (dbl 1))))
  {:proof-limit 1
   :fuel 64})
;; => one proof
```

The direct executable query does not succeed, because no `composite/1`
procedure clause exists:

```clojure
(query/query-succeeds
  (:program beta-composite-system)
  (frontend/q (composite (dbl (dbl 1))))
  1
  64)
;; => ()
```

Now put the same definition in `reflected`:

```clojure
(def reflected-composite-system
  (sjas/system-source
    {:profile :willard-sjas-tableau0}
    (language
      (relations (composite 1)))
    (reflected
      (|- (composite x)
          (mult (dbl 1) (dbl 1) x)))))

(frequencies (map :group (:axioms reflected-composite-system)))
;; => {:group-zero 2,
;;     :group-one 3,
;;     :group-two-b 1,
;;     :group-three 1}
```

The ordinary Procedure Call Rule can execute `composite/1`:

```clojure
(query/query-succeeds
  (:program reflected-composite-system)
  (frontend/q (composite (dbl (dbl 1))))
  1
  64)
;; => one proof
```

Because the clause is reflected as Group-2b, the SJAS theorem helper can also
prove the same claim from the reflected axiom basis:

```clojure
(sjas/query-succeeds
  reflected-composite-system
  (frontend/q (composite (dbl (dbl 1))))
  {:proof-limit 1
   :fuel 64})
;; => one proof
```

The executable version can also synthesize the answer:

```clojure
(ast/nom x
  (sjas/query-answers
    reflected-composite-system
    (ast/pos-lit (ast/app-term 'composite (ast/var-term x)))
    [x]
    {:proof-limit 1
     :fuel 64}))
;; first binding => x = (app dbl (app dbl (app 1)))
```

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

Generated code terms are visible at the object-language boundary:

```clojure
(require '[proflog.willard-sjas-code :as sjas-code])

(sjas-code/code-term? (:system-code system))
;; => true

(sjas-code/code-term-bytes (:code (:group-three system)))
;; => a base-64 byte vector for the generated Group-3 formula
```

The compiled program intentionally leaves `:sjas/proof-targets` nil. The proof
profile instead decodes `system-code`, `theorem-code`, and `proof-code` through
the generated code registry and the proof byte decoder. `SelfCons0` therefore
mentions a real contradiction code, not a magic table key; if an inconsistent
beta basis proves `false`, the resulting certificate can be checked against
`contradiction-code`.

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

`tableau-proof/3` no longer accepts a miniature `mini-closed` placeholder or a
host theorem-target label. A certificate is a compact base-64 object-language
encoding of a Proflog kernel proof term:

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

The proof predicate also supports a formal axiom-citation proof line:

```clojure
(let [beta-record (first (filter #(= :group-two (:group %)) (:axioms system)))
      certificate (sjas/proof-certificate 'sjas-axiom)]
  (query/query-succeeds
    (:program system)
    (sjas/tableau-proof (:system-code system)
                        (:code beta-record)
                        certificate)
    1
    96))
;; => one proof
```

This is not a host theorem lookup. The checker decodes the proof code and then
checks the generated object-language `axiom-member(system-code, theorem-code)`
facts for the active reflected system. The same `sjas-axiom` certificate is
rejected for `contradiction-code` in the consistent demo system.

ADR-0062/0063 add two self-justification checks over real code terms:

```clojure
(let [group3-proof (first
                     (sjas/query-succeeds
                       system
                       (:formula (:group-three system))
                       {:proof-limit 1
                        :fuel 96}))
      certificate (sjas/proof-certificate group3-proof)]
  (query/query-succeeds
    (:program system)
    (sjas/tableau-proof (:system-code system)
                        (:code (:group-three system))
                        certificate)
    1
    160))
;; => one proof
```

This proves the generated self-consistency sentence as a theorem of the generated
SJAS and validates the resulting certificate against the Group-3 formula code.
That proof is expected to be axiom-like: Group-3 is one of the proper axioms of
the reflected system.

The contradiction code is tested with an intentionally inconsistent control
system:

```clojure
(def inconsistent-system
  (sjas/system
    {:profile :willard-sjas-tableau0
     :beta [(ast/false-form)]}))

(let [contradiction-proof (first
                            (sjas/query-succeeds
                              inconsistent-system
                              (ast/false-form)
                              {:proof-limit 1
                               :fuel 96}))
      certificate (sjas/proof-certificate contradiction-proof)]
  (query/query-succeeds
    (:program inconsistent-system)
    (sjas/tableau-proof (:system-code inconsistent-system)
                        sjas/contradiction-code
                        certificate)
    1
    160))
;; => one proof
```

The point of the control is not to recommend false beta axioms. It demonstrates
that `contradiction-code` denotes a real proof target: when the reflected basis
is explicitly inconsistent, the kernel can build and check an actual
contradiction certificate.

## Substitution-Proof Predicate

ADR-0064 adds the Level-1 proof predicate used by the generated Group-3
sentence:

```text
subst-prf(system-code, substitution-code, theorem-code, proof-code)
```

For the current finite `IS#_D(beta)` implementation, the generated substitution
boundary records identity substitutions for ordinary closed formulas in the
active system and one fixed-point entry for Level-1 Group-3:

```text
system-code, selfcons-skeleton-code -> group-three-code
```

The public predicate is still important because it gives the Level-1 formula
the right object-language vocabulary, and it gives later work a stable place to
replace finite entries with a general code-level `Subst` relation.

The focused test exercises the path with a real certificate:

```clojure
(let [level1-system (demo-system :willard-sjas-level1)
      beta-record (first (filter #(= :group-two (:group %))
                                 (:axioms level1-system)))
      beta-proof (first
                   (sjas/query-succeeds
                     level1-system
                     (:formula beta-record)
                     {:proof-limit 1
                      :fuel 96}))
      certificate (sjas/proof-certificate beta-proof)]
  (query/query-succeeds
    (:program level1-system)
    (sjas/subst-prf (:system-code level1-system)
                    (:system-code level1-system)
                    (:code beta-record)
                    certificate)
    1
    160))
;; => one proof
```

The same test rejects the certificate when the theorem code is replaced by the
Group-3 code, and rejects a malformed `refl-close` certificate for the beta
theorem. A structural check also verifies that the generated Level-1 Group-3
formula contains `neg-pair/2` and `subst-prf/4`, but no raw
`tableau-proof/3`.

ADR-0065 adds the fixed-point check. The generated Level-1 system exposes the
code of the skeleton `Gamma_1(g)`:

```clojure
(let [level1-system (demo-system :willard-sjas-level1)
      group3-record (:group-three level1-system)
      certificate (sjas/proof-certificate 'sjas-axiom)]
  (query/query-succeeds
    (:program level1-system)
    (sjas/subst-prf (:system-code level1-system)
                    (:selfcons-skeleton-code level1-system)
                    (:code group3-record)
                    certificate)
    1
    220))
;; => one proof
```

The same query with `(:system-code level1-system)` in the substitution-code
position returns `()`. The point of the check is that `SelfCons1` is not merely
using a proof predicate with four arguments; it is using the fixed-point
substitution shape required by Willard's `Gamma_1(n)` construction.

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
- `subst-prf/4` currently uses a finite substitution boundary for one
  `IS#_D(beta)` system: identity entries for ordinary generated formulas plus
  the generated `SelfCons` skeleton-to-Group-3 fixed-point entry. A general
  code-level `Subst` relation over arbitrary formula codes remains the next
  fidelity gap.
- The self-justification demonstration is non-vacuous at the proof-predicate
  boundary, but it is still a finite `IS#_D(beta)`-style executable substrate,
  not a mechanized proof of Willard's consistency-preservation theorem.
- The arithmetic profile is relational, but some reverse modes are still
  operationally expensive. The fixed-point certificate test is marked `^:slow`
  and currently takes about `82.81 s` in isolation; open certificate synthesis
  remains outside the focused suite.
- Passing bounded contradiction probes do not prove Willard's external
  consistency-preservation theorem.
