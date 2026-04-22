(ns proflog.quantified-programs-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.language :as language]
            [proflog.query :as query]
            [proflog.query-test :as qt]))

(def quantified-language
  (language/language
    {:constants ['zero]
     :functions {'s 1}
     :relations {'boxed-zero 1
                 'zero-only 1}}))

(defn zero-only-program
  []
  (ast/nom x y
    (language/compile-program
      quantified-language
      [(ast/clause 'zero-only [x]
                   (ast/forall-form
                     y
                     (ast/or-form
                       (ast/neq-lit (ast/var-term x) (ast/var-term y))
                       (ast/eq-lit (ast/var-term y) (ast/app-term 'zero)))))])))

(defn boxed-zero-program
  []
  (ast/nom x y z
    (language/compile-program
      quantified-language
      [(ast/clause 'boxed-zero [x]
                   (ast/exists-form
                     y
                     (ast/and-form
                       (ast/eq-lit (ast/var-term x) (ast/var-term y))
                       (ast/forall-form
                         z
                         (ast/or-form
                           (ast/neq-lit (ast/var-term y) (ast/var-term z))
                           (ast/eq-lit (ast/var-term z) (ast/app-term 'zero)))))))])))

(deftest original-p1-quantified-clause-handles-deeper-ground-cases
  (testing "the original forall-based P1 clause now executes deeper success and failure cases directly"
    (let [program (qt/p1-program)]
      (is (seq
            (query/query-succeeds
              program
              (ast/pos-lit (ast/app-term 'even (qt/numeral 2)))
              1
              32)))
      (is (seq
            (query/query-fails
              program
              (ast/pos-lit (ast/app-term 'odd (qt/numeral 0)))
              1
              8))))))

(deftest universally-quantified-clause-bodies-support-ground-success-and-failure
  (testing "a forall-based clause body can distinguish the intended singleton case"
    (let [program (zero-only-program)]
      (is (seq
            (query/query-succeeds
              program
              (ast/pos-lit (ast/app-term 'zero-only (qt/numeral 0)))
              1
              16)))
      (is (seq
            (query/query-fails
              program
              (ast/pos-lit (ast/app-term 'zero-only (qt/numeral 1)))
              1
              16))))))

(deftest mixed-exists-and-forall-clause-bodies-support-ground-success-and-failure
  (testing "a clause body combining existential and universal structure executes end to end"
    (let [program (boxed-zero-program)]
      (is (seq
            (query/query-succeeds
              program
              (ast/pos-lit (ast/app-term 'boxed-zero (qt/numeral 0)))
              1
              32)))
      (is (seq
            (query/query-fails
              program
              (ast/pos-lit (ast/app-term 'boxed-zero (qt/numeral 1)))
              1
              32))))))
