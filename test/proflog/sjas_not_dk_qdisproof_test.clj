(ns proflog.sjas-not-dk-qdisproof-test
  "ADR-0142 Phase 3 (step 5, premise-clash): how the not-Dk tree binds its
   instantiation witnesses, using ONLY existing relational tableau rules.

   Step 5 derives `not Dk` by refuting

     P1 ^ P2 ^ Dk

   where `Dk = forall h y z. Subst(nbar,h) => not SemPrf^k(sys,k,h,y,z)` and the
   premises are `P1 = Subst(nbar, code(Dk))` (Eq 7, checker-accepted at step 2) and
   `P2 = SemPrf^k(code(Dk), p, 2^(p+1))` (the bounded proof of Dk, = step 4). The
   refutation is the ordinary cut-free tableau: alpha-expand the premises onto the
   branch, gamma-expand `Dk` with FRESH branch variables, beta-split the
   implication, and close each branch by a COMPLEMENTARY-LITERAL CLASH against a
   premise. The fresh gamma variable is bound to the witness by that clash:
   `not Subst(nbar, v_h)` clashes `P1` and binds `v_h := code(Dk)`;
   `not SemPrf^k(code(Dk), v_y, v_z)` clashes `P2` and binds `v_y := p`,
   `v_z := 2^(p+1)`. These are exactly Willard's `(p,q,r)`.

   There is NO need for a `witness-providing gamma` checker rule (an earlier note
   claimed there was): the witnesses come from the premises via clash. These tests
   pin that mechanism over a small universal so it is fast and unmistakable. The
   only missing input to the full not-Dk tree is `P2` itself -- the bounded proof
   of `Dk`, i.e. step 4."
  (:require [clojure.core.logic :as l]
            [clojure.core.logic.nominal :as nominal]
            [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.sjas-tree-builder :as tb]
            [proflog.willard-sjas :as sjas]))

(defn- mul-system []
  (sjas/system {:profile :willard-sjas-total-multiplication
                :functions sjas/total-multiplication-functions
                :relations sjas/total-multiplication-willard-relations
                :beta [(ast/eq-lit sjas/one sjas/one)]
                :code-format :u-grounding}))

(defn- subst-universal
  "`forall h. (neg Subst(arg, h))` -- a universal whose only refutation needs a
   witness `h` with `Subst(arg, h)` to clash against."
  [arg]
  (let [h (nominal/nom (l/lvar 'wh))]
    (ast/forall-form h (ast/neg-lit (ast/app-term 'subst-code arg (ast/var-term h))))))

(def ^:private canonical-neg-subst-one-v0
  "Canonical gamma child `(neg Subst((app \"1\"), (var v0)))`: the universal's body
   with the bound variable presented as the fresh branch variable `v0`."
  (list 'neg (list 'app 'subst-code (list 'app (symbol "1")) (list 'var 'v0))))

(deftest premise-clash-binds-the-universal-witness
  (testing "a positive Subst premise on the branch binds a fresh forall witness by complementary clash"
    (let [system (mul-system)
          univ (subst-universal sjas/one)
          premise (sjas/subst-code sjas/one (sjas/numeral 5))     ; Subst(1, 5)
          target (ast/and-form premise univ)
          ;; and -> premise on branch -> expand univ -> gamma child (neg Subst(1, v0)) clashes premise
          proof (tb/flex-tableau-node system target
                  (tb/flex-tableau-node system premise
                    (tb/flex-tableau-node system univ
                      (tb/canonical-flex-tableau-node system canonical-neg-subst-one-v0))))]
      (is (tb/valid-tree? system target proof 80)
          "the fresh forall witness v0 is bound to 5 by clashing (neg Subst(1,v0)) against the premise Subst(1,5)")
      ;; Soundness guard: WITHOUT the premise the universal has no witness to clash.
      (is (not (tb/valid-tree? system univ
                               (tb/flex-tableau-node system univ
                                 (tb/canonical-flex-tableau-node system canonical-neg-subst-one-v0))
                               80))
          "the universal alone does not refute -- the premise is what binds the witness"))))

(deftest ^:slow wrong-premise-leaves-the-universal-open
  ;; ^:slow: a correctly-failing refutation exhausts the closure search.
  (testing "a premise over a different argument cannot clash the universal's fixed argument"
    (let [system (mul-system)
          univ (subst-universal sjas/one)                          ; universal over arg = 1
          premise-wrong (sjas/subst-code (sjas/numeral 9) (sjas/numeral 5)) ; Subst(9, 5)
          target (ast/and-form premise-wrong univ)
          proof (tb/flex-tableau-node system target
                  (tb/flex-tableau-node system premise-wrong
                    (tb/flex-tableau-node system univ
                      (tb/canonical-flex-tableau-node system canonical-neg-subst-one-v0))))]
      (is (not (tb/valid-tree? system target proof 80))
          "Subst(9,5) cannot clash (neg Subst(1,v0)): the clash respects the universal's fixed argument"))))
