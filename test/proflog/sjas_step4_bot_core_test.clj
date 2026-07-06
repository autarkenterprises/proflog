(ns proflog.sjas-step4-bot-core-test
  "ADR-0147: executable BOT-core evidence for the revised step-4 path.

   This test does not claim that the public proof code for Dk has been
   constructed. It pins the next composition target: once that concrete bounded
   proof premise is available, D-star closes against it through the ordinary
   formula-bearing structural checker."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.core.logic :as l]
            [proflog.ast :as ast]
            [proflog.kernel.willard-sjas-profile :as sjas-profile]
            [proflog.sjas-step4-probe :as probe]
            [proflog.sjas-tree-builder :as tb]
            [proflog.willard-sjas :as sjas]
            [proflog.willard-sjas-code :as sjas-code]))

(defn- semprfk-bound-holds?
  "Run the proof-free Definition 2.1 bound relation on explicit public terms."
  [proof bound k]
  (boolean
    (seq
      (l/run 1 [q]
        ((var sjas-profile/with-occurs-check-offo)
         (l/fresh [sigma-out]
           ((var sjas-profile/sjas-semprfk-bound-holdso)
            proof bound k '() sigma-out (l/lvar 'bound-proof))
           (l/== true q)))))))

(defn- leq-holds?
  "Run the structural arithmetic relation used by a negative `leq` leaf."
  [left right]
  (boolean
    (seq
      (l/run 1 [q]
        ((var sjas-profile/with-occurs-check-offo)
         (l/fresh [sigma-out]
           ((var sjas-profile/sjas-relation-holds-coreo)
            'leq (list left right) '() sigma-out)
           (l/== true q)))))))

(defn- normal-equal-holds?
  "Run the proof-free arithmetic equality used by structural tableau leaves."
  [left right]
  (boolean
    (seq
      (l/run 1 [q]
        ((var sjas-profile/with-occurs-check-offo)
         (l/fresh [sigma-out]
           ((var sjas-profile/sjas-normal-equal-coreo)
            left right '() sigma-out)
           (l/== true q)))))))

(deftest theorem23-structural-arithmetic-evaluates-total-multiplication-terms
  (testing "proof-free tableau equality uses the same total-mul interpreter as traced checking"
    (is (normal-equal-holds?
          (sjas/mul-term (sjas/numeral 3) (sjas/numeral 4))
          (sjas/numeral 12))
        "a true multiplication equation closes in the structural checker")
    (is (not (normal-equal-holds?
               (sjas/mul-term (sjas/numeral 3) (sjas/numeral 4))
               (sjas/numeral 11)))
        "the same evaluator rejects a false product")))

(deftest theorem23-structural-arithmetic-evaluates-iterated-log-terms
  (testing "the V1/V2 iterlog term uses Definition 2.1 semantics in structural leaves"
    (is (normal-equal-holds?
          (sjas/iterated-log-term (sjas/numeral 65536) (sjas/numeral 2))
          (sjas/numeral 4))
        "Log(Log(65536)) equals 4")
    (is (not (normal-equal-holds?
               (sjas/iterated-log-term (sjas/numeral 65536) (sjas/numeral 2))
               (sjas/numeral 5)))
        "the structural interpreter rejects a false iterated logarithm")))

(deftest theorem23-uses-jsl2-kbar-not-one
  (testing "Theorem 3.5 chooses kbar=alpha+1, satisfying the V5 order premise"
    (let [system (sjas/total-multiplication-complete-system
                   {:depth 1 :code-format :u-grounding})
          kbar (sjas/theorem23-k-code system)]
      (is (= (ast/app-term 'add (:system-code system) sjas/one) kbar))
      (is (not= sjas/one kbar)
          "the old k=1 shortcut cannot satisfy k>=alpha for a generated system code")
      (is (leq-holds? (:system-code system) kbar)
          "the interpreted arithmetic must validate alpha<=alpha+1")
      (is (= 2 (get-in system [:language :functions 'tower-bound]))
          "the proof vocabulary declares the symbolic K-fold tower constructor"))))

(deftest theorem23-symbolic-tower-bound-matches-materialized-log
  (testing "tower-bound(k,m) denotes the K-fold base-2 tower whose K logs equal m"
    (let [symbolic (sjas/theorem23-tower-bound (sjas/numeral 2) (sjas/numeral 4))
          materialized (sjas/numeral 65536)]
      (is (semprfk-bound-holds? (sjas/numeral 3) symbolic (sjas/numeral 2)))
      (is (semprfk-bound-holds? (sjas/numeral 3) materialized (sjas/numeral 2))
          "Log(Log(65536))=4, so the symbolic and materialized bounds agree")
      (is (not (semprfk-bound-holds? (sjas/numeral 4)
                                      symbolic
                                      (sjas/numeral 2)))
          "the proof-code inequality remains strict")
      (is (not (semprfk-bound-holds? (sjas/numeral 3)
                                      symbolic
                                      (sjas/numeral 3)))
          "the symbolic tower's encoded height must equal the SemPrf superscript"))))

(deftest theorem23-real-kbar-tower-bound-is-checked-without-materialization
  (testing "the real alpha+1 height and proof code stay compact in the public tuple"
    (let [system (sjas/total-multiplication-complete-system
                   {:depth 1 :code-format :u-grounding})
          kbar (sjas/theorem23-k-code system)
          proof (sjas/proof-certificate 'sjas-axiom {:code-format :u-grounding})
          proof-value (sjas-code/bytes->u-grounding-code-value
                        (sjas-code/u-grounding-code-term-bytes proof))
          bound (sjas/theorem23-tower-bound kbar (sjas/numeral (inc proof-value)))]
      (is (semprfk-bound-holds? proof bound kbar)
          "p < Log(tower-bound(kbar,p+1),kbar)=p+1")
      (is (not (semprfk-bound-holds?
                 proof
                 (sjas/theorem23-tower-bound kbar (sjas/numeral proof-value))
                 kbar))
          "tower-bound(kbar,p) yields the rejected equality p<p"))))

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
