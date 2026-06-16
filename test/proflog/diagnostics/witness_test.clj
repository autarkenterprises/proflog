(ns proflog.diagnostics.witness-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.diagnostics.witness :as witness]
            [proflog.kernel :as kernel]
            [proflog.literature-tableau-golden :as golden]))

(defn- lit
  [sym polarity]
  (if (= polarity :pos)
    (ast/pos-lit (ast/app-term sym))
    (ast/neg-lit (ast/app-term sym))))

(deftest open-supported-branch-yields-witness-assignment
  (testing "ground propositional open branches produce expected assignments"
    (let [formula (ast/and-form (lit 'p :pos) (lit 'q :neg))
          result (witness/extract-witness formula)]
      (is (= :witness (:status result)))
      (is (= { 'p true 'q false} (:assignment result))))))

(deftest closed-branch-returns-closed-status
  (testing "contradictory formulas are closed rather than witnessed"
    (let [formula (ast/and-form (lit 'p :pos) (lit 'p :neg))]
      (is (= :closed (:status (witness/extract-witness formula))))
      (is (seq (kernel/prove formula 1))))))

(deftest contradictory-literal-set-is-rejected
  (testing "explicit contradictory literal data does not produce a witness"
    (is (= :closed
           (:status (witness/extract-witness
                      {:literals [[ 'p :pos] ['p :neg]]}))))))

(deftest unsupported-constructs-return-explicit-status
  (testing "quantified and non-flat formulas are reported as unsupported"
    (ast/nom x
      (is (= :unsupported
             (:status (witness/extract-witness
                        (ast/forall-form x (lit 'p :pos)))))))
    (is (= :unsupported
           (:status (witness/extract-witness
                      (ast/or-form (lit 'p :pos) (lit 'q :pos))))))))

(deftest witness-extraction-does-not-change-proof-search
  (testing "witness diagnostics remain read-only"
    (let [formula (ast/and-form (lit 'p :pos) (lit 'p :neg))
          before (kernel/prove formula 1)
          _ (witness/extract-witness formula)
          after (kernel/prove formula 1)]
      (is (= before after)))))

(deftest golden-suite-open-branch-uses-witness-diagnostics
  (testing "selected ADR-0112 open examples admit witness assignments"
    (doseq [id [:comprehensive/test-simple-atom
                :literature/test-fitting-satisfiable-example
                :formulas/p]]
      (let [entry (golden/entry-by-id id)
            formula (golden/formula-for-entry entry)
            result (witness/extract-witness formula)]
        (is (= :witness (:status result)) (str "expected witness for " id))
        (is (map? (:assignment result)))))))
