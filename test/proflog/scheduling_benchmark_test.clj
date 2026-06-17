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

(deftest branch-growth-envelopes-hold-for-closed-benchmarks-only
  (testing "proof-step envelopes apply only when a closing proof exists"
    (doseq [benchmark bench/benchmark-catalog
            :let [measured (bench/measure-benchmark benchmark)]]
      (is (bench/semantic-preservation-ok? measured)
          (str "semantic check must pass before envelope on " (:id benchmark)))
      (if (:branch-growth-applicable measured)
        (is (bench/branch-growth-ok? measured)
            (str "branch-growth envelope exceeded on " (:id benchmark)))
        (is (nil? (:step-count measured))
            (str "open benchmark must not fabricate step counts on " (:id benchmark)))))))

(deftest closed-proof-step-count-regression
  (testing "a closed benchmark fails if baseline proof depth grows without changing semantics"
    (let [measured (bench/measure-benchmark
                     (first (filter #(= :single-pending-goal-closed (:id %))
                                    bench/benchmark-catalog)))]
      (is (:branch-growth-applicable measured))
      (is (= :closes (:observation measured)))
      (is (= 3 (:step-count measured))
          "baseline closed contradiction proof depth is pinned at 3 steps"))))

(deftest run-benchmark-surfaces-semantic-failures-first
  (testing "a wrong semantic expectation is visible before branch-growth passes"
    (let [broken (assoc (first bench/benchmark-catalog)
                        :semantic (if (= :open (:semantic (first bench/benchmark-catalog)))
                                    :closes
                                    :open))
          result (bench/run-benchmark broken)]
      (is (false? (:semantic-ok result)))
      (is (not= (:expected result) (:observation result))))))

(deftest fast-scheduling-benchmarks-run
  (testing "fast-suite benchmarks preserve semantics"
    (doseq [benchmark (bench/fast-benchmarks)
            :let [result (bench/run-benchmark benchmark)]]
      (is (:semantic-ok result) (str "fast semantic failure on " (:id benchmark)))
      (when (:branch-growth-applicable result)
        (is (:branch-growth-ok result) (str "fast envelope failure on " (:id benchmark)))))))
