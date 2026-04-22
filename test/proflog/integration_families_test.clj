(ns proflog.integration-families-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.language :as language]
            [proflog.query :as query]))

(def tc-language
  (language/language
    {:constants ['a 'b 'c]
     :relations {'edge 2
                 'tc 2}}))

(def arithmetic-language
  (language/language
    {:constants ['zero]
     :functions {'s 1}
     :relations {'plus 3}}))

(defn numeral
  [n]
  (if (zero? n)
    (ast/app-term 'zero)
    (ast/app-term 's (numeral (dec n)))))

(defn tc-program
  []
  (ast/nom x y z
    (language/compile-program
      tc-language
      [(ast/clause 'edge [x y]
                   (ast/and-form
                     (ast/eq-lit (ast/var-term x) (ast/app-term 'a))
                     (ast/eq-lit (ast/var-term y) (ast/app-term 'b))))
       (ast/clause 'edge [x y]
                   (ast/and-form
                     (ast/eq-lit (ast/var-term x) (ast/app-term 'b))
                     (ast/eq-lit (ast/var-term y) (ast/app-term 'c))))
       (ast/clause 'tc [x y]
                   (ast/or-form
                     (ast/pos-lit (ast/app-term 'edge
                                                (ast/var-term x)
                                                (ast/var-term y)))
                     (ast/exists-form
                       z
                       (ast/and-form
                         (ast/pos-lit (ast/app-term 'edge
                                                    (ast/var-term x)
                                                    (ast/var-term z)))
                         (ast/pos-lit (ast/app-term 'tc
                                                    (ast/var-term z)
                                                    (ast/var-term y)))))))])))

(defn plus-program
  []
  (ast/nom x y z x1 z1
    (language/compile-program
      arithmetic-language
      [(ast/clause 'plus [x y z]
                   (ast/or-form
                     (ast/and-form
                       (ast/eq-lit (ast/var-term x) (ast/app-term 'zero))
                       (ast/eq-lit (ast/var-term z) (ast/var-term y)))
                     (ast/exists-form
                       x1
                       (ast/exists-form
                         z1
                         (ast/and-form
                           (ast/eq-lit
                             (ast/var-term x)
                             (ast/app-term 's (ast/var-term x1)))
                           (ast/and-form
                             (ast/eq-lit
                               (ast/var-term z)
                               (ast/app-term 's (ast/var-term z1)))
                             (ast/pos-lit
                               (ast/app-term 'plus
                                             (ast/var-term x1)
                                             (ast/var-term y)
                                             (ast/var-term z1)))))))))])))

;; The transitive-closure family still has an operational boundary on its
;; negative cases when expressed through a separate auxiliary `edge/2` relation.
;; The Peano `plus/3` family, however, can already sustain a materially deeper
;; ground regression slice and should use it.

(deftest transitive-closure-handles-direct-edges
  (testing "transitive closure discharges the direct edge cases promptly"
    (let [program (tc-program)]
      (is (seq
            (query/query-succeeds
              program
              (ast/pos-lit (ast/app-term 'tc (ast/app-term 'a) (ast/app-term 'b)))
              1
              8)))
      (is (seq
            (query/query-succeeds
              program
              (ast/pos-lit (ast/app-term 'tc (ast/app-term 'b) (ast/app-term 'c)))
              1
              8))))))

(deftest peano-addition-handles-the-base-case
  (testing "addition discharges the zero-left base case promptly"
    (let [program (plus-program)]
      (is (seq
            (query/query-succeeds
              program
              (ast/pos-lit (ast/app-term 'plus (numeral 0) (numeral 2) (numeral 2)))
              1
              8))))))

(deftest peano-addition-handles-non-base-ground-success-cases
  (testing "addition also closes several recursive ground truths in the extended suite"
    (let [program (plus-program)]
      (is (seq
            (query/query-succeeds
              program
              (ast/pos-lit (ast/app-term 'plus (numeral 1) (numeral 0) (numeral 1)))
              1
              16)))
      (is (seq
            (query/query-succeeds
              program
              (ast/pos-lit (ast/app-term 'plus (numeral 1) (numeral 1) (numeral 2)))
              1
              32)))
      (is (seq
            (query/query-succeeds
              program
              (ast/pos-lit (ast/app-term 'plus (numeral 2) (numeral 1) (numeral 3)))
              1
              64)))
      (is (seq
            (query/query-succeeds
              program
              (ast/pos-lit (ast/app-term 'plus (numeral 2) (numeral 3) (numeral 5)))
              1
              128))))))

(deftest peano-addition-refutes-wrong-ground-sums
  (testing "addition refutes wrong sums through direct failure proofs"
    (let [program (plus-program)]
      (is (seq
            (query/query-fails
              program
              (ast/pos-lit (ast/app-term 'plus (numeral 1) (numeral 1) (numeral 1)))
              1
              64)))
      (is (seq
            (query/query-fails
              program
              (ast/pos-lit (ast/app-term 'plus (numeral 0) (numeral 1) (numeral 0)))
              1
              32)))
      (is (seq
            (query/query-fails
              program
              (ast/pos-lit (ast/app-term 'plus (numeral 1) (numeral 2) (numeral 2)))
              1
              64))))))
