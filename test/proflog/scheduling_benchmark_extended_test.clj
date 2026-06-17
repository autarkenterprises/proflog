(ns proflog.scheduling-benchmark-extended-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.scheduling-benchmarks :as bench]))

(deftest extended-scheduling-benchmarks-run
  (testing "extended-suite benchmarks preserve semantics; closed cases keep step envelopes"
    (doseq [benchmark (bench/extended-benchmarks)
            :let [result (bench/run-benchmark benchmark)]]
      (is (:semantic-ok result) (str "extended semantic failure on " (:id benchmark)))
      (when (:branch-growth-applicable result)
        (is (:branch-growth-ok result) (str "extended envelope failure on " (:id benchmark)))))))
