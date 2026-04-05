;; ============================================================================
;; Group Verifier Helpers
;; ============================================================================
;;
;; Shared GV program builders and finite-table specs used by both the test
;; suite and the manual associativity bench harness.

(ns cljtap.gv-assoc
  (:require [clojure.core.logic :refer [lvar]]
            [clojure.core.logic.nominal :as nominal :refer [tie]]))

(defn gv-term
  "Convert a domain element symbol to a Proflog constant term."
  [sym]
  ['app sym])

(defn gv-and*
  "Build a right-associated conjunction from a sequence of formulas."
  [formulas]
  (reduce (fn [acc fml] ['and fml acc]) (reverse formulas)))

(defn gv-or*
  "Build a right-associated disjunction from a sequence of formulas."
  [formulas]
  (reduce (fn [acc fml] ['or fml acc]) (reverse formulas)))

(defn gv-forall*
  "Build nested ∀ quantifiers from a sequence of noms and a body."
  [noms body]
  (reduce (fn [acc n] ['forall (tie n acc)]) body (reverse noms)))

(defn gv-exists*
  "Build nested ∃ quantifiers from a sequence of noms and a body."
  [noms body]
  (reduce (fn [acc n] ['exists (tie n acc)]) body (reverse noms)))

(defn gv-op-eq-inline
  "op(x,y)=z expressed as equalities from the operation table."
  [spec x y z]
  (gv-or*
    (for [[[a b] c] (:op spec)]
      (gv-and* [['eq x (gv-term a)]
                ['eq y (gv-term b)]
                ['eq z (gv-term c)]]))))

(defn gv-neg-op-eq-inline
  "¬op(x,y,z) in NNF — the negation of gv-op-eq-inline."
  [spec x y z]
  (gv-and*
    (for [[[a b] c] (:op spec)]
      (gv-or* [['neq x (gv-term a)]
               ['neq y (gv-term b)]
               ['neq z (gv-term c)]]))))

(defn gv-in-domain-inline
  "x ∈ domain expressed as equalities."
  [spec x]
  (gv-or* (for [d (:domain spec)] ['eq x (gv-term d)])))

(defn gv-not-in-domain-inline
  "x ∉ domain in NNF."
  [spec x]
  (gv-and* (for [d (:domain spec)] ['neq x (gv-term d)])))

(defn gv-identity-program
  "gv_identity() ← ∀x.(¬D(x) ∨ (op(e,x,x) ∧ op(x,e,x)))."
  [spec x]
  (let [e  (gv-term (:identity spec))
        vx ['var x]]
    [['gv_identity []
      (gv-forall* [x]
        ['or (gv-not-in-domain-inline spec vx)
             ['and (gv-op-eq-inline spec e vx vx)
                   (gv-op-eq-inline spec vx e vx)]])]]))

(defn gv-closure-program
  "gv_closure() ← ∀x.∀y.(¬D(x) ∨ ¬D(y) ∨ ∃z.(op(x,y,z) ∧ D(z)))."
  [spec x y z]
  (let [vx ['var x]
        vy ['var y]
        vz ['var z]]
    [['gv_closure []
      (gv-forall* [x y]
        (gv-or* [(gv-not-in-domain-inline spec vx)
                 (gv-not-in-domain-inline spec vy)
                 (gv-exists* [z]
                   ['and (gv-op-eq-inline spec vx vy vz)
                         (gv-in-domain-inline spec vz)])]))]]))

(defn gv-inverses-program
  "gv_inverses() ← ∀x.(¬D(x) ∨ ∃y.(D(y) ∧ op(x,y,e) ∧ op(y,x,e)))."
  [spec x y]
  (let [e  (gv-term (:identity spec))
        vx ['var x]
        vy ['var y]]
    [['gv_inverses []
      (gv-forall* [x]
        ['or (gv-not-in-domain-inline spec vx)
             (gv-exists* [y]
               (gv-and* [(gv-in-domain-inline spec vy)
                         (gv-op-eq-inline spec vx vy e)
                         (gv-op-eq-inline spec vy vx e)]))])]]))

(defn gv-assoc-program
  "Associativity via the full 7-universal formulation."
  [spec x y z w1 w2 w3 w4]
  (let [vx  ['var x]  vy  ['var y]  vz  ['var z]
        vw1 ['var w1] vw2 ['var w2] vw3 ['var w3] vw4 ['var w4]]
    [['gv_assoc []
      (gv-forall* [x y z w1 w2 w3 w4]
        (gv-or* [(gv-neg-op-eq-inline spec vx vy vw1)
                 (gv-neg-op-eq-inline spec vw1 vz vw2)
                 (gv-neg-op-eq-inline spec vy vz vw3)
                 (gv-neg-op-eq-inline spec vx vw3 vw4)
                 ['eq vw2 vw4]]))]]))

(defn gv-assoc-chained-program
  "Associativity with 3 universals plus existential witnesses per concrete
   triple. The prover still resolves the op-lookups itself."
  [spec x y z]
  (let [vx ['var x]
        vy ['var y]
        vz ['var z]
        dom (:domain spec)
        triple-checks
        (for [a dom, b dom, c dom]
          (let [w1-nom (nominal/nom (lvar (gensym "w1_")))
                w2-nom (nominal/nom (lvar (gensym "w2_")))
                w3-nom (nominal/nom (lvar (gensym "w3_")))
                w4-nom (nominal/nom (lvar (gensym "w4_")))
                vw1 ['var w1-nom]
                vw2 ['var w2-nom]
                vw3 ['var w3-nom]
                vw4 ['var w4-nom]]
            (gv-or*
              [['neq vx (gv-term a)]
               ['neq vy (gv-term b)]
               ['neq vz (gv-term c)]
               ['exists
                (tie w1-nom
                     ['and (gv-op-eq-inline spec (gv-term a) (gv-term b) vw1)
                      ['exists
                       (tie w2-nom
                            ['and (gv-op-eq-inline spec vw1 (gv-term c) vw2)
                             ['exists
                              (tie w3-nom
                                   ['and (gv-op-eq-inline spec (gv-term b) (gv-term c) vw3)
                                    ['exists
                                     (tie w4-nom
                                          ['and (gv-op-eq-inline spec (gv-term a) vw3 vw4)
                                           ['eq vw2 vw4]])]])]])]])]])))]
    [['gv_assoc_ch []
      (gv-forall* [x y z]
        (gv-or* [(gv-not-in-domain-inline spec vx)
                 (gv-not-in-domain-inline spec vy)
                 (gv-not-in-domain-inline spec vz)
                 (gv-and* triple-checks)]))]]))

(defn gv-assoc-precomputed-program
  "Pre-computed associativity checker using only 3 universals (x, y, z)."
  [spec x y z]
  (let [vx    ['var x]
        vy    ['var y]
        vz    ['var z]
        op    (:op spec)
        dom   (:domain spec)
        triple-checks
        (for [a dom, b dom, c dom]
          (let [ab   (get op [a b])
                ab-c (get op [ab c])
                bc   (get op [b c])
                a-bc (get op [a bc])]
            (gv-or* [['neq vx (gv-term a)]
                     ['neq vy (gv-term b)]
                     ['neq vz (gv-term c)]
                     ['eq (gv-term ab-c) (gv-term a-bc)]])))]
    [['gv_assoc_pre []
      (gv-forall* [x y z]
        (gv-or* [(gv-not-in-domain-inline spec vx)
                 (gv-not-in-domain-inline spec vy)
                 (gv-not-in-domain-inline spec vz)
                 (gv-and* triple-checks)]))]]))

(def gv-z2
  "Z₂ = ({0,1}, +mod2, identity=0)."
  {:domain   ['zero 'one]
   :op       {['zero 'zero] 'zero
              ['zero 'one]  'one
              ['one  'zero] 'one
              ['one  'one]  'zero}
   :identity 'zero})

(def gv-z1
  "Z₁ = ({e}, trivial operation, identity=e)."
  {:domain   ['e]
   :op       {['e 'e] 'e}
   :identity 'e})

(def gv-non-group
  "A non-group 2-element magma."
  {:domain   ['zero 'one]
   :op       {['zero 'zero] 'zero
              ['zero 'one]  'one
              ['one  'zero] 'zero
              ['one  'one]  'zero}
   :identity 'zero})

(def gv-z4
  "Z4 = ({0,1,2,3}, + mod 4, identity 0)."
  {:domain   ['d0 'd1 'd2 'd3]
   :op       {['d0 'd0] 'd0  ['d0 'd1] 'd1  ['d0 'd2] 'd2  ['d0 'd3] 'd3
              ['d1 'd0] 'd1  ['d1 'd1] 'd2  ['d1 'd2] 'd3  ['d1 'd3] 'd0
              ['d2 'd0] 'd2  ['d2 'd1] 'd3  ['d2 'd2] 'd0  ['d2 'd3] 'd1
              ['d3 'd0] 'd3  ['d3 'd1] 'd0  ['d3 'd2] 'd1  ['d3 'd3] 'd2}
   :identity 'd0})

(def gv-non-group-4
  "A 4-element non-associative magma obtained by perturbing Z4 at 1*2."
  {:domain   ['d0 'd1 'd2 'd3]
   :op       {['d0 'd0] 'd0  ['d0 'd1] 'd1  ['d0 'd2] 'd2  ['d0 'd3] 'd3
              ['d1 'd0] 'd1  ['d1 'd1] 'd2  ['d1 'd2] 'd0  ['d1 'd3] 'd0
              ['d2 'd0] 'd2  ['d2 'd1] 'd3  ['d2 'd2] 'd0  ['d2 'd3] 'd1
              ['d3 'd0] 'd3  ['d3 'd1] 'd0  ['d3 'd2] 'd1  ['d3 'd3] 'd2}
   :identity 'd0})
