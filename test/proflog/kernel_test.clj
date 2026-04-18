(ns proflog.kernel-test
  (:refer-clojure :exclude [==])
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.core.logic :refer [run]]
            [proflog.ast :as ast]
            [proflog.kernel :as kernel]))

(defn provable?
  "True when the greenfield kernel finds at least one closed tableau."
  [formula]
  (seq (kernel/prove formula 1)))

(defn not-provable?
  "True when the greenfield kernel finds no proof within the given bound."
  ([formula] (not-provable? formula 1))
  ([formula n]
   (empty? (kernel/prove formula n))))

(deftest direct-complementary-closure
  (testing "a positive and negative copy of the same atom close immediately"
    (is (provable?
          (ast/and-form
            (ast/pos-lit (ast/app-term 'p))
            (ast/neg-lit (ast/app-term 'p)))))))

(deftest beta-splitting-requires-both-branches-to-close
  (testing "a disjunction closes only when both branches close"
    (is (provable?
          (ast/or-form
            (ast/and-form
              (ast/pos-lit (ast/app-term 'p))
              (ast/neg-lit (ast/app-term 'p)))
            (ast/and-form
              (ast/pos-lit (ast/app-term 'q))
              (ast/neg-lit (ast/app-term 'q))))))
    (is (not-provable?
          (ast/or-form
            (ast/and-form
              (ast/pos-lit (ast/app-term 'p))
              (ast/neg-lit (ast/app-term 'p)))
            (ast/pos-lit (ast/app-term 'q)))))))

(deftest gamma-rule-instantiates-universals
  (testing "a universal formula can close against a contrary ground literal"
    (ast/nom x
      (is (provable?
            (ast/and-form
              (ast/forall-form x
                               (ast/pos-lit
                                 (ast/app-term 'value (ast/var-term x))))
              (ast/neg-lit (ast/app-term 'value (ast/app-term 'zero)))))))))

(deftest delta-rule-introduces-one-rigid-witness
  (testing "an existential witness can close a contradiction inside its body"
    (ast/nom x
      (is (provable?
            (ast/exists-form
              x
              (ast/and-form
                (ast/pos-lit (ast/app-term 'value (ast/var-term x)))
                (ast/neg-lit (ast/app-term 'value (ast/var-term x))))))))))

(deftest proveo-accepts-a-partially-specified-proof-shape
  (testing "the kernel relation can fill the tail of a constrained proof skeleton"
    (is (= ['(savefml (close))]
           (run 1 [tail]
             (kernel/proveo
               (ast/and-form
                 (ast/pos-lit (ast/app-term 'p))
                 (ast/neg-lit (ast/app-term 'p)))
               '() '() '() (list 'conj tail)))))))
