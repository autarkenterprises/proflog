(ns proflog.query-extended-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.query :as query]
            [proflog.query-test :as qt]))

(deftest bounded-success-query-helper-returns-control-on-timeout
  (testing "bounded success queries eventually return an empty result when the budget expires"
    ;; Keep this in the extended suite: the finite-fuel timeout contract is
    ;; operational rather than hard real-time, so loaded machines can stretch
    ;; the last admitted slice well past the nominal timeout.
    (is (= '()
           (query/query-succeeds-within
             (qt/p2-program)
             (ast/pos-lit (ast/app-term 'win (qt/numeral 0)))
             1
             25)))))
