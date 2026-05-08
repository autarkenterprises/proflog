(ns proflog.combinatory-logic-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.combinatory-logic :as ski]
            [proflog.frontend :as pf]
            [proflog.proof :as proof]
            [proflog.query :as query]))

(defn- succeeds?
  [formula fuel]
  (seq (query/query-succeeds (ski/program) formula 1 fuel)))

(defn- proof-backed?
  [proofs]
  (and (seq proofs)
       (some #(or (proof/contains-step? % 'neg-call)
                  (proof/contains-step? % 'pos-call)
                  (proof/contains-step? % 'neg-call-guarded-alt)
                  (proof/contains-step? % 'pos-call-guarded-alt))
             proofs)))

(deftest ski-root-reductions-close-through-the-kernel
  (testing "I x => x"
    (is (proof-backed?
          (succeeds?
            (ast/pos-lit (ast/app-term 'step
                                       (ski/ap (ski/c 'icomb) (ski/c 'a))
                                       (ski/c 'a)))
            32))))
  (testing "K x y => x"
    (is (proof-backed?
          (succeeds?
            (ast/pos-lit (ast/app-term 'step
                                       (ski/ap (ski/ap (ski/c 'kcomb) (ski/c 'a))
                                               (ski/c 'b))
                                       (ski/c 'a)))
            32))))
  (testing "S x y z => x z (y z)"
    (is (proof-backed?
          (succeeds?
            (ast/pos-lit (ast/app-term 'step
                                       (ski/ap (ski/ap (ski/ap (ski/c 'scomb)
                                                              (ski/c 'kcomb))
                                                       (ski/c 'kcomb))
                                               (ski/c 'a))
                                       (ski/ap (ski/ap (ski/c 'kcomb)
                                                       (ski/c 'a))
                                               (ski/ap (ski/c 'kcomb)
                                                       (ski/c 'a)))))
            48)))))

(deftest ski-skk-identity-fully-evaluates
  (is (succeeds?
        (ast/pos-lit (ast/app-term 'eval-for
                                   (ski/numeral 2)
                                   (ski/skk (ski/c 'a))
                                   (ski/c 'a)))
        64)))

(deftest ski-boolean-true-fully-evaluates
  (is (succeeds?
        (ast/pos-lit (ast/app-term 'eval-for
                                   (ski/numeral 1)
                                   (ski/choose (ski/true-term)
                                               (ski/c 'a)
                                               (ski/c 'b))
                                   (ski/c 'a)))
        64)))

(deftest ski-boolean-false-fully-evaluates
  (is (succeeds?
        (ast/pos-lit (ast/app-term 'eval-for
                                   (ski/numeral 2)
                                   (ski/choose (ski/false-term)
                                               (ski/c 'a)
                                               (ski/c 'b))
                                   (ski/c 'b)))
        96)))

(deftest ski-omega-quine-reproduces-itself-through-a-guided-trace
  (let [sii (ski/sii)
        i-sii (ski/ap (ski/c 'icomb) sii)
        omega (ski/omega)
        expanded (ski/ap i-sii i-sii)
        left-contracted (ski/ap sii i-sii)]
    (is (proof-backed?
          (succeeds?
            (ski/reduction-trace-formula
              [omega
               expanded
               left-contracted
               omega]
              {:relation 'full-step})
            160)))))

(deftest ski-answer-mode-exports-a-reduced-term
  (let [records (pf/run (ski/program) [result]
                  (eval-for (s (s zero))
                            (ap (ap (ap scomb kcomb) kcomb) a)
                            result)
                  {:fuel 64
                   :call-depth 4
                   :proof-limit 4
                   :max-raw-proof-limit 16})
        expected (ski/c 'a)]
    (is (some #(= expected (-> % :bindings first second))
              records))
    (is (some #(and (= expected (-> % :bindings first second))
                    (empty? (:residuals %)))
              records))))

(deftest combinatory-logic-namespace-does-not-contain-a-host-evaluator
  (let [source (slurp "src/proflog/combinatory_logic.clj")]
    (is (str/includes? source "pf/proflog"))
    (is (not (str/includes? source "query/query-succeeds")))
    (is (not (str/includes? source "answers/query-answers")))
    (is (not (re-find #"defn-?\s+(step|eval|eval-for|reduce|rewrite)" source)))))
