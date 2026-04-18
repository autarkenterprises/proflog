(ns proflog.query-test
  (:refer-clojure :exclude [==])
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.language :as language]
            [proflog.query :as query]))

(def query-language
  (language/language
    {:constants ['zero]
     :functions {'s 1}
     :relations {'p 1
                 'even 1
                 'odd 1
                 'win 1
                 'undef 1}}))

(defn numeral
  [n]
  (if (zero? n)
    (ast/app-term 'zero)
    (ast/app-term 's (numeral (dec n)))))

(defn p1-program
  []
  (ast/nom x y
    (language/compile-program
      query-language
      [(ast/clause 'even [x]
                   (ast/or-form
                     (ast/eq-lit (ast/var-term x) (ast/app-term 'zero))
                     (ast/exists-form y
                                      (ast/and-form
                                        (ast/eq-lit (ast/var-term x)
                                                    (ast/app-term 's (ast/var-term y)))
                                        (ast/pos-lit (ast/app-term 'odd (ast/var-term y)))))))
       (ast/clause 'odd [x]
                   (ast/forall-form y
                                    (ast/implies-form
                                      (ast/pos-lit (ast/app-term 'even (ast/var-term y)))
                                      (ast/neq-lit (ast/var-term x) (ast/var-term y)))))])))

(defn status-program
  []
  (ast/nom x
    (language/compile-program
      query-language
      [(ast/clause 'p [x]
                   (ast/eq-lit (ast/var-term x) (ast/app-term 'zero)))])))

(defn p2-program
  []
  (ast/nom x y
    (language/compile-program
      query-language
      [(ast/clause 'win [x]
                   (ast/exists-form y
                                    (ast/and-form
                                      (ast/or-form
                                        (ast/eq-lit (ast/var-term x)
                                                    (ast/app-term 's (ast/var-term y)))
                                        (ast/eq-lit (ast/var-term x)
                                                    (ast/app-term 's
                                                                  (ast/app-term 's
                                                                                (ast/var-term y)))))
                                      (ast/neg-lit (ast/app-term 'win (ast/var-term y))))))])))

(deftest query-status-distinguishes-success-failure-and-unresolved
  (testing "undefined declared relations stay unresolved while defined ones succeed or fail"
    (let [program (status-program)]
      (is (= :succeeds
             (query/query-status
               program
               (ast/pos-lit (ast/app-term 'p (numeral 0)))
               {:timeout-ms 200})))
      (is (= :fails
             (query/query-status
               program
               (ast/pos-lit (ast/app-term 'p (numeral 1)))
               {:timeout-ms 200})))
      (is (= :unresolved
             (query/query-status
               program
               (ast/pos-lit (ast/app-term 'undef (numeral 0)))
               {:timeout-ms 200}))))))

(deftest fitting-p1-even-zero-succeeds
  (testing "P1 proves even(0)"
    (is (seq
          (query/query-succeeds-within
            (p1-program)
            (ast/pos-lit (ast/app-term 'even (numeral 0)))
            1
            300)))))

(deftest fitting-p1-odd-one-succeeds
  (testing "P1 proves odd(s(0))"
    (is (seq
          (query/query-succeeds-within
            (p1-program)
            (ast/pos-lit (ast/app-term 'odd (numeral 1)))
            1
            300)))))

(deftest fitting-p2-win-three-fails
  (testing "P2 refutes win(3)"
    (is (seq
          (query/query-fails-within
            (p2-program)
            (ast/pos-lit (ast/app-term 'win (numeral 3)))
            1
            300)))))
