(ns proflog.literature-tableau-golden-extended-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.literature-tableau-golden :as golden]
            [proflog.scheduling-benchmarks :as scheduling]))

(defn atom-lit
  [name]
  (ast/pos-lit (ast/app-term (symbol name))))

(defn or*
  [& formulas]
  (reduce ast/or-form formulas))

(defn and*
  [& formulas]
  (reduce ast/and-form formulas))

(deftest ^:slow branch-growth-benchmark-has-durable-structural-evidence
  (testing "The Fitting branch-bound example records branch-growth evidence for an open tableau case."
    (let [record (golden/translation-for-source-test "test_fitting_branch_bound_example")
          formula (golden/formula-for-key (:formula-key record))
          measurement (scheduling/measure-formula
                       {:benchmark-id :fitting-branch-bound
                        :formula formula
                        :expected :open
                        :origin "Fitting branch-bound example"})]
      (is (< 1 (:estimated-branches measurement) 50))
      (is (pos? (:expansion-count measurement)))
      (is (nil? (:closed-proof-step-count measurement)))
      (is (= :open (:result measurement))))))

(deftest ^:slow large-branching-open-formula-remains-measurable
  (testing "Open cases with many beta choices no longer report a fabricated zero-step benchmark."
    (let [formula (and* (or* (atom-lit "p0") (atom-lit "p1"))
                        (or* (atom-lit "p0") (atom-lit "p2"))
                        (or* (atom-lit "p1") (atom-lit "p3"))
                        (or* (atom-lit "p2") (atom-lit "p3")))
          measurement (scheduling/measure-formula
                       {:benchmark-id :large-open-branching
                        :formula formula
                        :expected :open
                        :origin "ADR-0115 open-case branch growth regression"})]
      (is (= :open (:result measurement)))
      (is (< 1 (:estimated-branches measurement) 50))
      (is (<= (:estimated-branches measurement) 50))
      (is (pos? (:expansion-count measurement)))
      (is (nil? (:closed-proof-step-count measurement))))))
