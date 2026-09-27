(ns lcert.minting-agent-test
  "Step 2 of proflog ADR-0143: a minting agent tiles on a lazy supply
  promised at 10^100 tokens, spending only what it mints (METATHEORY.md
  §2.4).  (Not part of jpt4/sjas.)

  The agent is handed a list of codes and a raw supply.  It mints each
  successor's certificate from the supply with the parser on caseR, checks
  it, reflects it under the charged cap, and runs it on the rest.  The chain
  here is two minting agents and then a leaf agent that acts (2)."
  (:require [clojure.test :refer [deftest is testing]]
            [lcert.syntax :as s]
            [lcert.typing :as t]
            [lcert.eval :as ev]
            [lcert.core :as lc]
            [lcert.examples :as ex]
            [lcert.ho-forms :as f]))

(def ^:private huge (.pow (biginteger 10) 100))

(defn- code-of [form]
  (binding [s/*reflect-class* :first-order] (lc/certify 0 form)))

(defn- run-chain
  "Run `agent-form` on the code list and supply under cap `cap`, with the
  first-order reflect rule and the charged cap.  Returns the result, the
  number of supply nodes materialized, and the reflect events."
  [agent-form codes supply cap]
  (let [events (atom []) mat (atom 0)]
    (binding [s/*reflect-class* :first-order
              ev/*charged-cap* true
              ev/*trace* #(swap! events conj %)
              ev/*materialized* mat]
      (let [agent (#'ev/ev (ev/erase (t/check-top 0 (s/parse-term [] agent-form))) [] cap {:erase? true})
            result ((agent (f/code-list codes)) supply)]
        {:result result
         :materialized @mat
         :reflects (filterv #(and (= :reflect (:event %)) (:runs %)) @events)}))))

(deftest the-minting-agent-is-certified
  (let [{:keys [code type nodes budget]} (code-of f/mint-agent-form)]
    (is (= (s/parse-type [] f/mint-agent-type) type))
    (is (zero? budget) "a closed program: it declares no tokens of its own")
    (is (binding [s/*reflect-class* :first-order] (lc/check code f/mint-agent-type)))
    (is (< nodes 25000) "its certificate is smaller than the delegating agent's 21,135 + parser")))

(deftest minting-on-a-supply-promised-at-10-to-the-100
  (let [agent (code-of f/mint-agent-form)
        leaf (code-of f/mint-leaf-form)
        codes [(:code agent) (:code agent) (:code leaf)]
        spent (+ (* 2 (:nodes agent)) (:nodes leaf))
        {:keys [result materialized reflects]} (run-chain f/mint-agent-form codes (ev/lazy-supply huge) huge)]
    (testing "two minting agents, then the leaf agent's action 2"
      (is (= [:pv 2 :star] result))
      (is (= 3 (count reflects))))
    (testing "exactly the minted certificates' nodes were materialized, out of 10^100"
      (is (= spent materialized)))
    (testing "each reflection's charged cap is its caller's less the certificate it burns"
      (is (= huge (biginteger (:cap (first reflects)))))
      (doseq [{:keys [cap nodes m decoded-cap]} reflects]
        (is (= decoded-cap (+ (- cap nodes) m))))
      (is (= (- huge spent) (biginteger (:decoded-cap (last reflects))))))))

(deftest the-definable-destructor-cannot-use-a-lazy-supply
  (testing "built on the definable out, the same agent walks the whole supply at its first
            step, and the materialization guard stops it"
    (let [walker (f/mint-agent-with ex/parse-form)
          leaf (code-of f/mint-leaf-form)]
      (binding [ev/*materialize-limit* 5000]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"materializ"
                              (run-chain walker [(:code leaf)] (ev/lazy-supply huge) huge)))))))
