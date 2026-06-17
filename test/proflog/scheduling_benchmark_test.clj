(ns proflog.scheduling-benchmark-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.scheduling-benchmarks :as bench]))

(deftest benchmark-catalog-covers-required-case-kinds
  (testing "ADR-0115 requires single, multi, deterministic, nondeterministic, and branching cases"
    (let [kinds (set (map :kind bench/benchmark-catalog))]
      (is (= (bench/required-kinds) kinds)))))

(deftest semantic-expectations-precede-branch-growth
  (testing "every benchmark records an independent semantic baseline"
    (doseq [benchmark bench/benchmark-catalog]
      (is (contains? #{:open :closes} (:semantic benchmark))
          (str "missing semantic baseline on " (:id benchmark)))
      (let [measured (bench/measure-benchmark benchmark)]
        (is (contains? #{:open :closes} (:observation measured)))
        (is (boolean (bench/semantic-preservation-ok? measured))
            (str "semantic mismatch on " (:id benchmark)))))))

(deftest branch-growth-envelopes-hold-for-baseline-behavior
  (testing "proof-step counts stay within recorded envelopes at baseline"
    (doseq [benchmark bench/benchmark-catalog]
      (let [measured (bench/measure-benchmark benchmark)]
        (is (bench/semantic-preservation-ok? measured)
            (str "semantic check must pass before envelope on " (:id benchmark)))
        (is (bench/branch-growth-ok? measured)
            (str "branch-growth envelope exceeded on " (:id benchmark)))))))

(deftest run-benchmark-surfaces-semantic-failures-first
  (testing "a wrong semantic expectation is visible before branch-growth passes"
    (let [broken (assoc (first bench/benchmark-catalog)
                          :semantic (if (= :open (:semantic (first bench/benchmark-catalog)))
                                      :closes
                                      :open))
          result (bench/run-benchmark broken)]
      (is (false? (:semantic-ok result)))
      (is (not= (:expected result) (:observation result))))))

(deftest ^:slow extended-scheduling-benchmarks-run
  (testing "extended-suite benchmarks preserve semantics under larger envelopes"
    (doseq [benchmark (bench/extended-benchmarks)
            :let [result (bench/run-benchmark benchmark)]]
      (is (:semantic-ok result) (str "extended semantic failure on " (:id benchmark)))
      (is (:branch-growth-ok result) (str "extended envelope failure on " (:id benchmark))))))

(deftest fast-scheduling-benchmarks-run
  (testing "fast-suite benchmarks preserve semantics"
    (doseq [benchmark (bench/fast-benchmarks)
            :let [result (bench/run-benchmark benchmark)]]
      (is (:semantic-ok result) (str "fast semantic failure on " (:id benchmark)))
      (is (:branch-growth-ok result) (str "fast envelope failure on " (:id benchmark))))))
