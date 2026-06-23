(ns proflog.scheduling-benchmark-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.scheduling-benchmarks :as scheduling]))

(deftest benchmark-catalog-is-structural-and-source-linked
  (testing "Every benchmark documents the profile-independent formula shape it measures."
    (is (seq scheduling/benchmark-cases))
    (doseq [case scheduling/benchmark-cases]
      (is (:benchmark-id case) case)
      (is (:origin case) case)
      (is (:formula case) case)
      (is (#{:open :closes} (:expected case)) case))))

(deftest open-and-closed-benchmarks-both-carry-nonfabricated-measurements
  (testing "Open cases use structural branch-growth data; closed cases additionally carry proof steps."
    (let [measurements (map scheduling/measure-benchmark scheduling/benchmark-cases)]
      (is (some #(= :open (:expected %)) measurements))
      (is (some #(= :closes (:expected %)) measurements))
      (doseq [measurement measurements]
        (is (= (:expected measurement) (:result measurement)) measurement)
        (is (pos? (:formula-size measurement)) measurement)
        (is (nat-int? (:expansion-count measurement)) measurement)
        (is (pos-int? (:estimated-branches measurement)) measurement)
        (is (pos-int? (:max-depth measurement)) measurement)
        (if (= :closes (:expected measurement))
          (is (pos-int? (:closed-proof-step-count measurement)) measurement)
          (is (nil? (:closed-proof-step-count measurement)) measurement))))))

(deftest branch-growth-thresholds-use-open-case-structural-evidence
  (let [results (scheduling/run-benchmarks)
        by-id (into {} (map (juxt :benchmark-id identity) results))]
    (testing "The branch-growth benchmark is an open tableau case and is checked against its source bound."
      (is (= :open (get-in by-id [:branch-bound-fitting :result])))
      (is (< 1 (get-in by-id [:branch-bound-fitting :estimated-branches]) 50))
      (is (true? (get-in by-id [:branch-bound-fitting :branch-growth-ok]))))
    (testing "No benchmark reports the previous placeholder zero search count."
      (is (not-any? #(= 0 (:search-measure %)) results)))))
