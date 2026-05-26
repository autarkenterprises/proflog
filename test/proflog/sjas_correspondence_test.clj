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
  (testing "tableau structure and SJAS arithmetic equality are relevant, while kernel equality bridges remain unresolved"
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'split))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'close))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'sjas-equal))))
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

(deftest proof-term-audit-reports-symbols-outside-certificate-alphabet
  (testing "proof terms can contain kernel/profile evidence that the SJAS proof-code encoder cannot currently encode"
    (let [audit (correspondence/audit-proof-term
                  '(conj
                     (sjas-code-arg 1 sjas-code-args-end)
                     (free-close)))]
      (is (= #{'sjas-code-arg 'sjas-code-args-end 'free-close}
             (:unencodable-symbols audit)))
      (is (= (:unencodable-symbols audit)
             (:unclassified-symbols audit))))))

(deftest profile-wrapper-audit-is-path-sensitive
  (testing "profiled wrappers have different relevance depending on their payload role"
    (is (= :probably-irrelevant
           (:status (correspondence/classify-profile-form
                      '(profiled willard-sjas-tableau0 (conj close))))))
    (is (= :relevant
           (:status (correspondence/classify-profile-form
                      '(profiled willard-sjas-arithmetic
                         (sjas-equal (sjas-read-one)
                                     (sjas-read-one)
                                     (sjas-bind-done)))))))
    (is (= :probably-excluded
           (:status (correspondence/classify-profile-form
                      '(profiled first-order (close))))))))

(deftest proof-check-profile-wrapper-audit-allows-relation-specific-payloads
  (testing "SJAS proof-check profile forms carry relation-specific payload arity"
    (is (= :relevant
           (:status (correspondence/classify-profile-form
                      '(profiled willard-sjas-subst-proof-check
                         (sjas-code-bytes)
                         (willard-sjas-subst-code)
                         sjas-axiom)))))
    (let [audit (correspondence/audit-proof-term
                  '(profiled willard-sjas-level1
                     (profiled willard-sjas-subst-proof-check
                       (sjas-code-bytes)
                       (willard-sjas-subst-code)
                       sjas-axiom)))]
      (is (= #{'(profiled willard-sjas-subst-proof-check
                  (sjas-code-bytes)
                  (willard-sjas-subst-code)
                  sjas-axiom)}
             (:relevant-profile-forms audit))))))
