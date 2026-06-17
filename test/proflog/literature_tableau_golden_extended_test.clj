(ns proflog.literature-tableau-golden-extended-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.literature-tableau-golden :as golden]))

(deftest literature-tableau-extended-branch-growth-envelopes
  (testing "performance-disposition entries stay within modest proof-step envelopes"
    (doseq [{:keys [id suite proflog-expectation] :as entry} golden/inventory-entries
            :when (and (= :extended suite)
                       (some? (golden/formula-for-entry entry))
                       proflog-expectation)
            :let [{:keys [observation step-count]}
                  (golden/prove-with-steps (golden/formula-for-entry entry))]]
      (is (= proflog-expectation observation) (str "semantic mismatch on " id))
      (when (= :closes proflog-expectation)
        (is (<= step-count 64)
            (str "branch-growth envelope exceeded on " id))))))
