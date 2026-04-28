(ns proflog.kernel.dispatch-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.kernel :as kernel]
            [proflog.kernel.first-order :as first-order]
            [proflog.kernel.propositional :as propositional]
            [proflog.language :as language]
            [proflog.pelletier-test :as pelletier]
            [proflog.proof :as proof]))

(defn closed-propositional-formula
  []
  (ast/and-form
    (ast/pos-lit (ast/app-term 'p))
    (ast/neg-lit (ast/app-term 'p))))

(deftest pure-propositional-proof-entry-uses-propositional-component
  (testing "kernel/prove dispatches theorem-style pure propositional formulas"
    (let [propositional-calls (atom 0)
          first-order-calls (atom 0)
          original-propositional-prove propositional/prove
          original-first-order-prove first-order/prove]
      (with-redefs [propositional/prove
                    (fn [& args]
                      (swap! propositional-calls inc)
                      (apply original-propositional-prove args))
                    first-order/prove
                    (fn [& args]
                      (swap! first-order-calls inc)
                      (apply original-first-order-prove args))]
        (is (seq (kernel/prove (closed-propositional-formula) 1 2)))
        (is (= 1 @propositional-calls))
        (is (zero? @first-order-calls))))))

(deftest equality-free-first-order-proof-entry-uses-first-order-component
  (testing "kernel/prove dispatches quantified theorem formulas to the first-order layer"
    (let [propositional-calls (atom 0)
          first-order-calls (atom 0)
          original-propositional-prove propositional/prove
          original-first-order-prove first-order/prove]
      (with-redefs [propositional/prove
                    (fn [& args]
                      (swap! propositional-calls inc)
                      (apply original-propositional-prove args))
                    first-order/prove
                    (fn [& args]
                      (swap! first-order-calls inc)
                      (apply original-first-order-prove args))]
        (is (seq
              (kernel/prove
                (pelletier/theorem-branch (pelletier/problem-18))
                1)))
        (is (zero? @propositional-calls))
        (is (= 1 @first-order-calls))))))

(deftest equality-bearing-formulas-stay-on-the-full-kernel
  (testing "equality formulas do not enter the propositional component"
    (let [propositional-calls (atom 0)
          first-order-calls (atom 0)
          original-propositional-prove propositional/prove
          original-first-order-prove first-order/prove]
      (with-redefs [propositional/prove
                    (fn [& args]
                      (swap! propositional-calls inc)
                      (apply original-propositional-prove args))
                    first-order/prove
                    (fn [& args]
                      (swap! first-order-calls inc)
                      (apply original-first-order-prove args))]
        (is (seq (kernel/prove
                   (ast/eq-lit (ast/app-term 'a) (ast/app-term 'b))
                   1)))
        (is (zero? @propositional-calls))
        (is (zero? @first-order-calls))))))

(def nullary-call-language
  (language/language
    {:constants ['a]
     :relations {'p 0}}))

(defn nullary-call-program
  []
  (language/compile-program
    nullary-call-language
    [(ast/clause 'p []
                 (ast/neq-lit (ast/app-term 'a)
                              (ast/app-term 'a)))]))

(deftest program-bearing-proof-search-stays-on-the-full-kernel
  (testing "program calls keep their procedure-call proof tags even for nullary atoms"
    (let [propositional-calls (atom 0)
          first-order-calls (atom 0)
          original-propositional-prove propositional/prove
          original-first-order-prove first-order/prove]
      (with-redefs [propositional/prove
                    (fn [& args]
                      (swap! propositional-calls inc)
                      (apply original-propositional-prove args))
                    first-order/prove
                    (fn [& args]
                      (swap! first-order-calls inc)
                      (apply original-first-order-prove args))]
        (let [proof (first
                      (kernel/prove-program
                        (nullary-call-program)
                        (ast/pos-lit (ast/app-term 'p))
                        1))]
          (is proof)
          (is (proof/contains-step? proof 'pos-call))
          (is (zero? @propositional-calls))
          (is (zero? @first-order-calls)))))))
