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
