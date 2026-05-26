(ns proflog.sjas-correspondence-test
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest is testing]]
            [proflog.sjas-correspondence :as correspondence]
            [proflog.willard-sjas-code :as sjas-code]))

(deftest proof-symbol-audit-classifies-every-encoded-certificate-symbol
  (testing "the Track 2a audit covers every proof symbol that SJAS can encode"
    (is (= #{}
           (set/difference (set sjas-code/proof-symbols)
                           (set (keys correspondence/proof-symbol-classifications)))))))

(deftest proof-symbol-audit-exposes-relevant-and-unresolved-constructors
  (testing "tableau structure is relevant and equality/procedure/profile bridges remain unresolved"
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'split))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'close))))
    (is (= :unresolved
           (:status (correspondence/classify-proof-symbol 'eq-step))))
    (is (= :unresolved
           (:status (correspondence/classify-proof-symbol 'pos-call))))
    (is (= :unresolved
           (:status (correspondence/classify-proof-symbol 'profiled))))))

(deftest proof-term-audit-reports-obligations-for-actual-proof-trees
  (testing "a decoded proof term can be summarized by Track 2a correspondence obligations"
    (let [audit (correspondence/audit-proof-term
                  '(split
                     close
                     (pos-call (eq-step close))))]
      (is (= #{'split 'close}
             (:relevant-symbols audit)))
      (is (= #{'pos-call 'eq-step}
             (:unresolved-symbols audit)))
      (is (= #{}
             (:unclassified-symbols audit))))))
