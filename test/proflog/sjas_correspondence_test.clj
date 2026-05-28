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
  (testing "tableau structure and direct free equality closure are relevant, while richer kernel equality bridges remain unresolved"
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'split))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'close))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'atom-close))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'occurs-close))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'sjas-equal))))
    (is (= :unresolved
           (:status (correspondence/classify-proof-symbol 'eq-step))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'free-close))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'decompose))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'args))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'refl-close))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'neq-rigid))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'neq-store))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'neq-close))))
    (is (= :unresolved
           (:status (correspondence/classify-proof-symbol 'eq-bind))))
    (is (= :unresolved
           (:status (correspondence/classify-proof-symbol 'eq-refl))))
    (is (= :unresolved
           (:status (correspondence/classify-proof-symbol 'par-bind))))
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

(deftest proof-term-audit-classifies-reachable-code-reader-and-free-closure-tags
  (testing "reachable code-reader and free-constructor closure evidence must be part of the explicit correspondence audit"
    (let [audit (correspondence/audit-proof-term
                  '(conj
                     (sjas-code-arg 1 sjas-code-args-end)
                     (free-close)))]
      (is (= #{}
             (:unencodable-symbols audit)))
      (is (= #{}
             (:unclassified-symbols audit)))
      (is (= #{'conj 'free-close 'sjas-code-arg 'sjas-code-args-end}
             (:relevant-symbols audit)))
      (is (= #{}
             (:unresolved-symbols audit))))))

(deftest proof-term-audit-classifies-u-grounding-canonical-byte-evidence
  (testing "U-Grounding byte-reader evidence must stay inside the explicit proof-code alphabet"
    (let [audit (correspondence/audit-proof-term
                  '(sjas-ug-code-canonical-byte
                     7
                     (sjas-ug-code-byte-cons
                       (sjas-ug-code-mul64-shift)
                       (sjas-ug-code-canonical-byte))))]
      (is (= #{}
             (:unencodable-symbols audit)))
      (is (= #{}
             (:unclassified-symbols audit)))
      (is (= #{'sjas-ug-code-canonical-byte
               'sjas-ug-code-byte-cons
               'sjas-ug-code-mul64-shift}
             (:relevant-symbols audit))))))

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
