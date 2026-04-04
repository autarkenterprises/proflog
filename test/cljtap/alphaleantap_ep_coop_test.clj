;; ============================================================================
;; αleanTAP-EP Cooperative Execution Tests
;; ============================================================================

(ns cljtap.alphaleantap-ep-coop-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.core.logic :as logic]
            [clojure.core.logic.nominal :as nominal]
            [cljtap.alphaleantap-ep :as ref]
            [cljtap.alphaleantap-ep-exec :as exec]
            [cljtap.alphaleantap-ep-fast :as fast]))

(defn- fresh-nom
  [label]
  (nominal/nom (logic/lvar label)))

(deftest test-CO01-partial-query-cuts-over-mid-proof
  (testing "A non-ground query starts in relational mode and cuts over once
            equality has grounded the residual branch."
    (let [orig fast/prove-branch-fast-results
          hits (atom 0)
          x    (fresh-nom 'x)
          q    (logic/lvar 'q)
          program
          (vector
            ['r [x]
             ['and
              ['eq ['var x] ['app 'a]]
              ['and
               ['pos ['app 'p]]
               ['neg ['app 'p]]]]])
          formula ['pos ['app 'r q]]]
      (with-redefs [fast/prove-branch-fast-results
                    (fn [& args]
                      (swap! hits inc)
                      (apply orig args))]
        (is (seq (ref/prove program formula 1 nil)))
        (is (pos? @hits))))))

(deftest test-CO02-partial-program-cuts-over-mid-proof
  (testing "A partially specified program stays symbolic until its body term is
            synthesized enough to discharge the remaining branch with the fast engine."
    (let [orig fast/prove-branch-fast-results
          hits (atom 0)
          c    (logic/lvar 'c)
          program
          (vector
            ['r []
             ['and
              ['eq c ['app 'a]]
              ['and
               ['pos ['app 'p]]
               ['neg ['app 'p]]]]])
          formula ['pos ['app 'r]]]
      (with-redefs [fast/prove-branch-fast-results
                    (fn [& args]
                      (swap! hits inc)
                      (apply orig args))]
        (is (seq (exec/prove program formula 1 nil)))
        (is (pos? @hits))))))

(deftest test-CO03-cutover-reuses-nonempty-lemma-thread
  (testing "Direct cutover passes a non-empty lemma thread into the fast engine."
    (let [results
          (logic/run 1 [q]
            (logic/fresh [proof lem-out]
              (ref/fast-cutovero ['pos ['app 'p]]
                                 '()
                                 '()
                                 '()
                                 '()
                                 proof
                                 nil
                                 (list ['neg ['app 'p]])
                                 lem-out)
              (logic/== q [proof lem-out])))]
      (is (seq results))
      (is (= [['lem-close]
              (list ['pos ['app 'p]]
                    ['neg ['app 'p]])]
             (first results))))))
