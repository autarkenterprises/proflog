(ns proflog.sjas-not-dk-qdisproof-test
  "ADR-0142 Phase 3 (step 5 Q-disproof): the structural machinery that refutes a
   FALSE Theorem 2.3 Pi1 instance assembles over the real multiplication system.

   Step 5 of the closure derives `not Dk` by instantiating the diagonal
   `Dk = forall h y z. Subst(nbar,h) => not SemPrf^k(sys,k,h,y,z)` at the
   diagonal witnesses and disproving the resulting Pi1 instance with `Q`
   (Willard's `Q`-disproof of the false instance). The instance

     Subst(nbar, code(Dk)) => not SemPrf^k(sys, k, code(Dk), p, 2^(p+1))

   is FALSE because both `Subst(nbar, code(Dk))` (Goedel diagonalization, Eq 7)
   and the bounded proof `SemPrf^k(code(Dk), p, 2^(p+1))` hold, so its consequent
   fails. Refuting an implication is the beta-rule: branch into `not antecedent`
   and `consequent`, and BOTH must close. The two leaves are the two halves of
   the `Q`-disproof:

     - `not Subst(nbar, code(Dk))` closes by EVALUATING the genuine `subst-code`
       gate (`sjas-subst-code-structural-closeo`, the construct-and-check
       counterpart of the search-layer subst-code close).
     - `not SemPrf^k(...)` closes by the bounded-proof V-route
       (`sjas-semprfk-alpha-structural-closeo`).

   These tests pin both mechanisms and their beta-split assembly over the EXACT
   generated `:willard-sjas-total-multiplication` system.

   HONEST SCOPE. This is the structural CORE of step 5, not a closed `not Dk`:
     1. The negated-antecedent leaf is over the REAL diagonal `Subst(nbar,
        code(Dk))`, so that half is verbatim.
     2. The consequent leaf uses a real route-axiom bounded proof as the
        SemPrf^k witness. In the real instance the theorem is `code(Dk)`, whose
        bounded proof is the step-4 combination tree (still open). The checker
        does not care that the two leaves name different theorems -- the
        implication's refutation closes each leaf independently -- so this
        validates the beta-split topology and both Q-disproof rules, while the
        coupling (same `code(Dk)` in both leaves) and the real proof `p` remain
        the documented residual, together with witness-providing
        gamma-instantiation (`Dk |- instance`)."
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.sjas-tree-builder :as tb]
            [proflog.willard-sjas :as sjas]
            [proflog.willard-sjas-code :as sjas-code]))

(defn- mul+pow-system []
  (sjas/system {:profile :willard-sjas-total-multiplication
                :functions (assoc sjas/total-multiplication-functions 'pow 2)
                :relations sjas/total-multiplication-willard-relations
                :beta [(ast/eq-lit sjas/one sjas/one)]
                :code-format :u-grounding}))

(defn- diagonal [system] (sjas/theorem23-diagonal system sjas/one))

(defn- route-witness
  "A real, checker-valid bounded proof: a Group-2 route axiom proved by its
   `sjas-axiom` certificate, with its proof value (so `2^(value+1)` is a true
   bound and `2^value` is too small)."
  [system]
  (let [route (first (filter #(= :group-two (:group %)) (:axioms system)))
        cert (sjas/proof-certificate 'sjas-axiom {:code-format :u-grounding})
        value (sjas-code/bytes->u-grounding-code-value
                (sjas-code/u-grounding-code-term-bytes cert))]
    {:theorem-code (:code route) :cert cert :value value}))

(defn- pow-bound [exp] (ast/app-term 'pow (sjas/numeral 2) (sjas/numeral exp)))

(defn- neg-semprfk
  [system {:keys [theorem-code cert]} bound]
  (ast/neg-lit (second (sjas/semprfk-alpha (:system-code system) sjas/one
                                           theorem-code cert bound))))

(deftest subst-qdisproof-closes-the-real-diagonal-locator
  (testing "(neg Subst(nbar, code(Dk))) self-closes by evaluating the subst-code gate; a wrong target does not"
    (let [system (mul+pow-system)
          diag (diagonal system)
          nbar (:skeleton-code diag)
          dk (:diagonal-code diag)
          neg-subst-true (ast/neg-lit (ast/app-term 'subst-code nbar dk))
          neg-subst-wrong (ast/neg-lit (ast/app-term 'subst-code nbar (sjas/numeral 7)))]
      (is (tb/valid-tree? system neg-subst-true
                          (tb/flex-tableau-node system neg-subst-true) 200)
          "Subst(nbar, code(Dk)) holds by diagonalization, so its negation closes (the Q-disproof of the Subst conjunct)")
      (is (not (tb/valid-tree? system neg-subst-wrong
                               (tb/flex-tableau-node system neg-subst-wrong) 200))
          "the gate is genuinely evaluated: a false Subst leaves its negation un-closable"))))

(deftest ^:slow false-pi1-instance-refutation-assembles-both-q-disproof-branches
  ;; ^:slow: the implication beta-split decodes the giant code(Dk) numeral through
  ;; both Q-disproof leaves (~1 min over the real diagonal). The leaf-level bound
  ;; check (too-small tower stays open) is already covered, fast, by
  ;; proflog.sjas-semprfk-tree-closure-test/too-small-tower-bound-does-not-close.
  (testing "the implication beta-split closes the negated antecedent (subst eval) and the consequent (V-route) over the real system"
    (let [system (mul+pow-system)
          diag (diagonal system)
          nbar (:skeleton-code diag)
          dk (:diagonal-code diag)
          w (route-witness system)
          subst-pos (sjas/subst-code nbar dk)
          neg-subst (ast/neg-lit (ast/app-term 'subst-code nbar dk))
          consequent (neg-semprfk system w (pow-bound (inc (:value w))))
          ;; refute  Subst(nbar,code(Dk)) => consequent
          target (ast/implies-form subst-pos consequent)
          proof (tb/flex-tableau-node system target
                  ;; left branch: (not antecedent) -> dualize -> (neg subst) -> subst Q-disproof
                  (tb/flex-tableau-node system (ast/not-form subst-pos)
                    (tb/flex-tableau-node system neg-subst))
                  ;; right branch: consequent -> SemPrf^k V-route
                  (tb/flex-tableau-node system consequent))]
      (is (tb/valid-tree? system target proof 50)
          "the false-instance refutation closes both Q-disproof branches"))))
