(ns proflog.scheduling-benchmark-extended-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.scheduling-benchmarks :as scheduling]))

(deftest ^:slow extended-scheduling-benchmarks-run
  (testing "The ADR-0115 extended gate executes every catalog benchmark and records timing."
    (let [results (scheduling/run-benchmarks)]
      (is (seq results))
      (is (every? #(= (:expected %) (:result %)) results))
      (is (every? #(number? (:elapsed-ms %)) results))
      (is (every? #(pos? (:search-measure %)) results))
      (is (true? (get-in (into {} (map (juxt :benchmark-id identity) results))
                         [:branch-bound-fitting :branch-growth-ok]))))))
