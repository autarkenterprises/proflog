(ns proflog.sjas-cut-composition-test
  "ADR-0142 criterion 6: Theorem 2.2 proof-composition transformation.

   Demonstrates and verifies Willard 2002 JSL2 Theorem 2.2 (the cut-elimination
   corollary) as the concrete with-cut composition used three times in the proof
   of Theorem 2.3. The leaf sub-proofs are genuine kernel entailments; the only
   added inference is the analytic cut, whose soundness is excluded middle."
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.language :as language]
            [proflog.sjas-cut-composition :as cut]))

(def lang
  (language/language {:constants ['c] :functions {} :relations {'p 1 'q 1 'r 1}}))

(defn- at [rel] (ast/pos-lit (ast/app-term rel (ast/app-term 'c))))
(def Pc (at 'p))
(def Qc (at 'q))
(def Rc (at 'r))

(defn- alpha
  "Compile alpha = { p(c), q(c), r(x) :- p(x) /\\ q(x) }. With `:include-q? false`
   the q fact is dropped, so alpha no longer proves q(c)."
  [& {:keys [include-q?] :or {include-q? true}}]
  (ast/nom x
    (language/compile-program
      lang
      (cond-> [(ast/clause 'p [x] (ast/eq-lit (ast/var-term x) (ast/app-term 'c)))]
        include-q? (conj (ast/clause 'q [x]
                                     (ast/eq-lit (ast/var-term x) (ast/app-term 'c))))
        true (conj (ast/clause 'r [x]
                               (ast/and-form
                                 (ast/pos-lit (ast/app-term 'p (ast/var-term x)))
                                 (ast/pos-lit (ast/app-term 'q (ast/var-term x))))))))))

(def ^:private impl-fml (ast/implies-form (ast/and-form Pc Qc) Rc))

(deftest theorem-2-2-cut-composition-is-sound-and-verified
  (testing "from alpha|-P, alpha|-Q, alpha|-(P&Q=>R), compose-by-cut yields a verified alpha|-R"
    (let [program (alpha)
          eL (cut/entailment program Pc)
          eT (cut/entailment program Qc)
          eImpl (cut/entailment program impl-fml)
          composite (cut/compose-by-cut eL eT eImpl Rc)
          report (cut/validate-cut-composition program composite)]
      (is (:closed? eL) "alpha proves Lambda=P")
      (is (:closed? eT) "alpha proves Theta=Q")
      (is (:closed? eImpl) "alpha proves Lambda /\\ Theta => Xi")
      (is (:valid? report) "the cut composition validates")
      (is (= 5 (:with-cut-size report))
          "with-cut proof = two cut nodes plus three lemma leaves")
      (is (:closed? (cut/entailment program Rc))
          "the composed conclusion R is a genuine theorem of alpha, cross-checked by the kernel"))))

(deftest theorem-2-2-rejects-open-leaf
  (testing "the composition is invalid if any premise leaf does not close"
    (let [program (alpha)
          composite (cut/compose-by-cut (cut/entailment program Pc)
                                        (cut/entailment program Qc)
                                        (cut/entailment program impl-fml)
                                        Rc)
          report-no-q (cut/validate-cut-composition (alpha :include-q? false) composite)]
      (is (not (:valid? report-no-q))
          "without the Q lemma closing under the weaker alpha, the composition is rejected")
      (is (false? (:theta-closed? report-no-q))
          "validation re-derives the open leaf rather than trusting the stored flag"))))

(deftest theorem-2-2-rejects-wrong-implication-shape
  (testing "the implication leaf must be exactly (Lambda /\\ Theta => Xi)"
    (let [program (alpha)
          ;; third leaf proves P, not the required implication
          bad (cut/compose-by-cut (cut/entailment program Pc)
                                  (cut/entailment program Qc)
                                  (cut/entailment program Pc)
                                  Rc)
          report (cut/validate-cut-composition program bad)]
      (is (not (:valid? report)))
      (is (not (:impl-shape-ok? report))
          "a malformed implication shape is detected even though that leaf closes"))))

(deftest cut-free-expansion-boundary-is-documented
  (testing "the namespace records that the cut-free expansion is the open research boundary"
    (is (= :with-cut-composition (:provided cut/cut-free-expansion-boundary)))
    (is (= :cut-free-tableau
           (:guaranteed-but-not-materialized cut/cut-free-expansion-boundary)))))
