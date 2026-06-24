(ns proflog.sjas-theorem23-closure-test
  "ADR-0142 criterion 6b: the Theorem 2.3 closure assembles with an honest,
   per-step verification status that localizes the research boundary."
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.sjas-theorem23-closure :as t23]
            [proflog.willard-sjas :as sjas]))

(defn- mul-system []
  (sjas/system {:profile :willard-sjas-total-multiplication
                :functions sjas/total-multiplication-functions
                :relations sjas/total-multiplication-willard-relations
                :beta [(ast/eq-lit sjas/one sjas/one)]
                :code-format :u-grounding}))

(deftest theorem23-closure-localizes-the-boundary
  (testing "the six-step Theorem 2.3 closure assembles with honest per-step status"
    (let [status (t23/theorem23-closure-status (mul-system) sjas/one)
          by-id (into {} (map (juxt :id :status) (:steps status)))]
      ;; Conditions A and C are reflected axioms of the generated system.
      (is (= :reflected-axiom (by-id :A)))
      (is (= :reflected-axiom (by-id :C)))
      ;; Step 2 (Subst(nbar, code(Dk)), Eq 7) is checker-accepted, validated live
      ;; against the exact generated system by the relational subst-code.
      (is (= :checker-accepted (by-id :step2)))
      (is (contains? (:checker-accepted status) :step2-subst-eq7))
      ;; Phase 0: the three Theorem 2.2 applications + condition B are cut-free
      ;; tableau trees to construct (no cut rule / trusted-base growth).
      (is (= #{:B :step1 :step3 :step4} (:tree-construction-steps status)))
      (is (every? #(= :tree-construction (by-id %)) [:B :step1 :step3 :step4]))
      ;; Step 5 (not Dk): its bounded-proof witness is now checker-accepted via the
      ;; symbolic pow bound (Phase 1); the step overall is partial.
      (is (= :partial (by-id :step5)))
      (is (contains? (:checker-accepted status)
                     :step5-bounded-proof-witness-symbolic-pow-bound))
      ;; The recorded progress reflects Phase 0/1.
      (is (contains? (:resolved-since-aar status) :phase0))
      (is (contains? (:resolved-since-aar status) :phase1))
      ;; D* (Eq 5) is a universal over the diagonal code.
      (is (= 'forall (ast/tag-of (:d-star status)))))))

(deftest theorem23-closure-makes-no-bot-derivation-claim
  (testing "the assembler reports a partial structure, never a closed BOT derivation"
    (let [status (t23/theorem23-closure-status (mul-system) sjas/one)
          statuses (set (map :status (:steps status)))]
      ;; Honesty guard: steps remain to construct (tree-construction) or finish
      ;; (partial); the status is not uniformly checker-accepted.
      (is (or (contains? statuses :tree-construction)
              (contains? statuses :partial))
          "the closure must not present itself as fully checker-accepted")
      (is (not= #{:checker-accepted} statuses)
          "a uniformly checker-accepted status would be an overclaim")
      ;; The open boundary still enumerates remaining work.
      (is (seq (:remaining (:open-boundary status)))
          "remaining tree-construction work is explicitly listed"))))
