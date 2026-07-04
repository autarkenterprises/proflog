(ns proflog.sjas-step4-bot-core-test
  "ADR-0147: executable BOT-core evidence for the revised step-4 path.

   This test does not claim that the public proof code for Dk has been
   constructed. It pins the next composition target: once that concrete bounded
   proof premise is available, D-star closes against it through the ordinary
   formula-bearing structural checker."
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.sjas-step4-probe :as probe]
            [proflog.sjas-tree-builder :as tb]))

(deftest ^:slow dstar-closes-against-concrete-bounded-proof-premise
  (testing "D-star plus a concrete SemPrf^k premise closes by saved-literal clash"
    (let [{:keys [system target tree fuel]} (:concrete-p2 (probe/step4-cases))]
      (is (tb/valid-tree? system target tree fuel)
          "the public encoded proof tree closes D-star against a concrete bounded-proof premise"))))

(deftest ^:slow real-diagonal-dstar-closes-against-concrete-bounded-proof-premise
  (testing "the real Theorem 2.3 D-star formula closes against a concrete SemPrf^k premise"
    (let [{:keys [system target tree fuel]} (probe/real-concrete-p2-case)]
      (is (tb/valid-tree? system target tree fuel)
          "the public encoded proof tree closes the real D-star formula against a concrete bounded-proof premise"))))
