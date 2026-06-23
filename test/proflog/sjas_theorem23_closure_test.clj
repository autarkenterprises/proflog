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
      ;; The three Theorem 2.2 applications + condition B are cut-composition steps
      ;; (verified with-cut; cut-free expansion is the documented boundary).
      (is (= #{:B :step1 :step3 :step4} (:cut-composition-steps status)))
      (is (every? #(= :cut-composition (by-id %)) [:B :step1 :step3 :step4]))
      ;; Step 5 (not Dk) rests on the documented open boundary.
      (is (= :open-boundary (by-id :step5)))
      (is (= :with-cut-composition (:provided (:open-boundary status))))
      ;; D* (Eq 5) is a universal over the diagonal code.
      (is (= 'forall (ast/tag-of (:d-star status)))))))

(deftest theorem23-closure-makes-no-bot-derivation-claim
  (testing "the assembler reports a partial structure, never a closed BOT derivation"
    (let [status (t23/theorem23-closure-status (mul-system) sjas/one)
          statuses (set (map :status (:steps status)))]
      ;; Honesty guard: at least one step is explicitly open; not all checker-accepted.
      (is (contains? statuses :open-boundary)
          "the closure must not present itself as fully checker-accepted")
      (is (not= #{:checker-accepted} statuses)
          "a uniformly checker-accepted status would be an overclaim"))))
