(ns proflog.diagnostics.proof-trace-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.diagnostics.proof-trace :as trace]
            [proflog.kernel :as kernel]))

(defn contradiction-formula
  []
  (ast/and-form
    (ast/pos-lit (ast/app-term 'p))
    (ast/neg-lit (ast/app-term 'p))))

(defn open-formula
  []
  (ast/pos-lit (ast/app-term 'p)))

(deftest closed-proof-renders-stable-structured-trace
  (testing "known closed proofs expose rule applications and closure explanation"
    (let [proof (first (kernel/prove (contradiction-formula) 1))
          trace (trace/proof-trace-edn proof)]
      (is (= :ok (:status trace)))
      (is (pos? (:step-count trace)))
      (is (some '#{conj} (map :tag (:steps trace))))
      (is (= :contradictory-literals (get-in trace [:closure :reason])))
      (is (= trace (trace/proof-trace-edn proof))))))

(deftest formatted-trace-is-deterministic
  (testing "human-readable formatting is stable for the same proof artifact"
    (let [proof (first (kernel/prove (contradiction-formula) 1))
          text-a (trace/format-proof-trace (trace/proof-trace-edn proof))
          text-b (trace/format-proof-trace (trace/proof-trace-edn proof))]
      (is (= text-a text-b))
      (is (clojure.string/includes? text-a "status: ok"))
      (is (clojure.string/includes? text-a "closure: contradictory-literals")))))

(deftest incomplete-artifacts-return-explicit-statuses
  (testing "malformed or incomplete artifacts do not throw incidental host errors"
    (is (= :unsupported (:status (trace/proof-trace-edn 42))))
    (is (= :insufficient-data (:status (trace/proof-trace-edn nil))))
    (is (= :insufficient-data (:status (trace/proof-trace-edn '(unknown-step)))))))

(deftest open-proof-trace-reports-steps-without-closure
  (testing "open or incomplete searches still render recognized partial structure"
    (let [partial '(conj (savefml unknown-tail))
          trace (trace/proof-trace-edn partial)]
      (is (= :ok (:status trace)))
      (is (nil? (:closure trace)))
      (is (pos? (:step-count trace))))))

(deftest renderer-does-not-change-proof-search-results
  (testing "diagnostic rendering is read-only with respect to kernel answers"
    (let [formula (contradiction-formula)
          before (kernel/prove formula 1)
          _ (trace/render-proof-trace (first before))
          after (kernel/prove formula 1)]
      (is (= before after)))))
