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

(defn succeeds-directly?
  ([program query]
   (succeeds-directly? program query nil))
  ([program query fuel]
   (seq
     (if (nil? fuel)
       (query/query-succeeds program query 1)
       (query/query-succeeds program query 1 fuel)))))

(defn fails-directly?
  ([program query]
   (fails-directly? program query nil))
  ([program query fuel]
   (seq
     (if (nil? fuel)
       (query/query-fails program query 1)
       (query/query-fails program query 1 fuel)))))

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

(defn recursive-parity-program
  []
  (ast/nom x y z
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
                   (ast/exists-form z
                                    (ast/and-form
                                      (ast/eq-lit (ast/var-term x)
                                                  (ast/app-term 's (ast/var-term z)))
                                      (ast/pos-lit (ast/app-term 'even (ast/var-term z))))))])))

(deftest query-status-distinguishes-success-failure-and-unresolved
  (testing "undefined declared relations stay unresolved while defined ones succeed or fail"
    (let [program (status-program)]
      (is (= :succeeds
             (query/query-status
               program
               (ast/pos-lit (ast/app-term 'p (numeral 0)))
               {:timeout-ms 1000})))
      (is (= :fails
             (query/query-status
               program
               (ast/pos-lit (ast/app-term 'p (numeral 1)))
               {:timeout-ms 1000})))
      (is (= :unresolved
             (query/query-status
               program
               (ast/pos-lit (ast/app-term 'undef (numeral 0)))
               {:timeout-ms 1000}))))))

(deftest fitting-p1-even-zero-succeeds
  (testing "P1 proves even(0) by direct proof search"
    (is (succeeds-directly?
          (p1-program)
          (ast/pos-lit (ast/app-term 'even (numeral 0)))))))

(deftest fitting-p1-odd-one-succeeds
  (testing "P1 proves odd(s(0)) once the recursive branch gets enough fuel"
    (is (succeeds-directly?
          (p1-program)
          (ast/pos-lit (ast/app-term 'odd (numeral 1)))
          16))))

(deftest fitting-p2-win-three-fails
  (testing "P2 refutes win(3) by direct proof search"
    (is (fails-directly?
          (p2-program)
          (ast/pos-lit (ast/app-term 'win (numeral 3)))))))

(deftest fitting-p2-small-positions-follow-the-nim-pattern
  (testing "P2 directly proves the expected winners and refutes the expected losers"
    (let [program (p2-program)]
      (is (fails-directly?
            program
            (ast/pos-lit (ast/app-term 'win (numeral 0)))))
      (is (succeeds-directly?
            program
            (ast/pos-lit (ast/app-term 'win (numeral 1)))))
      (is (succeeds-directly?
            program
            (ast/pos-lit (ast/app-term 'win (numeral 2)))))
      (is (fails-directly?
            program
            (ast/pos-lit (ast/app-term 'win (numeral 3)))))
      (is (succeeds-directly?
            program
            (ast/pos-lit (ast/app-term 'win (numeral 4)))))
      (is (succeeds-directly?
            program
            (ast/pos-lit (ast/app-term 'win (numeral 5)))
            16)))))

(deftest recursive-parity-higher-ground-cases-succeed
  (testing "the simpler mutually recursive parity program proves higher even and odd numerals"
    (let [program (recursive-parity-program)]
      (is (succeeds-directly?
            program
            (ast/pos-lit (ast/app-term 'even (numeral 2)))
            8))
      (is (succeeds-directly?
            program
            (ast/pos-lit (ast/app-term 'odd (numeral 3)))
            8))
      (is (succeeds-directly?
            program
            (ast/pos-lit (ast/app-term 'even (numeral 4)))
            16)))))

(deftest recursive-parity-opposite-ground-cases-fail
  (testing "the simpler mutually recursive parity program refutes opposite-parity numerals"
    (let [program (recursive-parity-program)]
      (is (fails-directly?
            program
            (ast/pos-lit (ast/app-term 'odd (numeral 0)))
            8))
      (is (fails-directly?
            program
            (ast/pos-lit (ast/app-term 'even (numeral 1)))
            8))
      (is (fails-directly?
            program
            (ast/pos-lit (ast/app-term 'odd (numeral 2)))
            8))
      (is (fails-directly?
            program
            (ast/pos-lit (ast/app-term 'even (numeral 3)))
            16)))))

(deftest bounded-success-query-helper-returns-control-on-timeout
  (testing "bounded success queries return an empty result instead of hanging the caller"
    (let [worker (future
                   (query/query-succeeds-within
                     (p2-program)
                     (ast/pos-lit (ast/app-term 'win (numeral 0)))
                     1
                     25))
          result (deref worker 750 ::timed-out)]
      (when (= ::timed-out result)
        (future-cancel worker))
      (is (not= ::timed-out result))
      (is (= '() result)))))
