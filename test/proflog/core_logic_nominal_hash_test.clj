(ns proflog.core-logic-nominal-hash-test
  (:refer-clojure :exclude [== hash])
  (:require [clojure.core.logic :refer [== conde fresh lcons run*]]
            [clojure.core.logic.nominal :as nominal]
            [clojure.test :refer [deftest is testing]]))

(defn guarded-lookupo
  "Association-list lookup for nominal keys, matching core.logic's nominal examples."
  [binding-nom env value]
  (fresh [skipped-key skipped-value rest]
    (conde
      [(== (lcons [binding-nom value] rest) env)]
      [(== (lcons [skipped-key skipped-value] rest) env)
       (nominal/hash binding-nom skipped-key)
       (guarded-lookupo binding-nom rest value)])))

(deftest nominal-hash-rejects-delayed-self-alias
  (testing "LOGIC-101-style delayed vars in nom/hash still reject self-aliasing"
    (is (= []
           (run* [q]
             (nominal/fresh [wanted]
               (fresh [key skipped]
                 (nominal/hash key skipped)
                 (== key skipped)
                 (== skipped wanted)
                 (== q true))))))))

(deftest guarded-nominal-lookup-prunes-skipped-key-alias
  (testing "core.logic nominal/hash supplies the missing lookup-recursion guard"
    (is (= [:first]
           (run* [q]
             (nominal/fresh [wanted]
               (fresh [key skipped out]
                 (guarded-lookupo key
                                  (lcons [skipped :first]
                                         (lcons [wanted :second] '()))
                                  out)
                 (== key skipped)
                 (== skipped wanted)
                 (== q out))))))))

(deftest nominal-suspension-short-circuits-after-failing-freshness
  (testing "ADR-0147 -suspc nil guard: a failed swap-name freshness check is logical failure, not a nil-state crash"
    (is (= []
           (run* [q]
             (nominal/fresh [left right]
               (fresh [term]
                 ;; Build the exact delayed-suspension shape that exposed the
                 ;; core.logic bug: the suspension sees the same logic variable
                 ;; on both sides, then the state later binds that variable to
                 ;; the first swapped nom. The first freshness check fails; the
                 ;; patched -suspc loop must short-circuit nil rather than
                 ;; applying the second freshness check to a nil state.
                 (nominal/suspc term term [left right])
                 (== term left)
                 (== q true))))))))
