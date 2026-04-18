(ns proflog.program-test
  (:refer-clojure :exclude [==])
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.core.logic :refer [run]]
            [proflog.ast :as ast]
            [proflog.kernel :as kernel]
            [proflog.language :as language]
            [proflog.program :as program]
            [proflog.query :as query]))

(def simple-language
  (language/language
    {:constants ['zero 'one]
     :functions {'succ 1}
     :relations {'p 1
                 'q 1}}))

(defn simple-program
  []
  (ast/nom x
    (language/compile-program
      simple-language
      [(ast/clause 'p [x]
                   (ast/eq-lit (ast/var-term x)
                               (ast/app-term 'zero)))])))

(deftest call-clauseo-binds-compiled-parameters-to-actual-arguments
  (testing "the program layer exposes the compiled body plus an argument-binding environment"
    (let [program (simple-program)
          actual (ast/app-term 'succ (ast/app-term 'zero))
          [[env body neg-body]]
          (run 1 [env body neg-body]
            (program/call-clauseo
              program
              (ast/app-term 'p actual)
              env
              body
              neg-body))
          bound-param (ffirst env)]
      (is (= 1 (count env)))
      (is (= actual (second (first env))))
      (is (= (ast/eq-lit (ast/var-term bound-param) (ast/app-term 'zero))
             body))
      (is (= (ast/neq-lit (ast/var-term bound-param) (ast/app-term 'zero))
             neg-body)))))

(deftest positive-and-negative-procedure-calls-close-literals
  (testing "procedure calls close positive literals when bodies fail and negative literals when bodies succeed"
    (let [program (simple-program)]
      (is (seq
            (kernel/prove-program
              program
              (ast/pos-lit (ast/app-term 'p (ast/app-term 'one)))
              1)))
      (is (seq
            (kernel/prove-program
              program
              (ast/neg-lit (ast/app-term 'p (ast/app-term 'zero)))
              1)))
      (is (= :succeeds
             (query/query-status
               program
               (ast/pos-lit (ast/app-term 'p (ast/app-term 'zero)))
               {:timeout-ms 200})))
      (is (= :fails
             (query/query-status
               program
               (ast/pos-lit (ast/app-term 'p (ast/app-term 'one)))
               {:timeout-ms 200}))))))
