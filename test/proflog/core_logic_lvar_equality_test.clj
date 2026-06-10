(ns proflog.core-logic-lvar-equality-test
  "ADR-0094 focused regression: the LVar equality contract the fast path
   must preserve.

   The fast path is observationally equivalent by construction, so this
   regression is green before and after the patch; it exists to pin the
   contract so any future change to LVar equality that alters it goes red:
   unique variables compare by identity of id (withMeta copies stay equal,
   fresh variables stay distinct), name-constructed variables compare by
   identity of the name object, non-IVar comparands are unequal, hashing is
   stable across meta copies, and substitution maps resolve equal copies of
   a key to the same binding."
  (:refer-clojure :exclude [==])
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.core.logic :as l :refer [==]]))

(deftest lvar-unique-equality-contract
  (testing "unique variables compare by identity of id"
    (let [a (l/lvar 'a)
          b (l/lvar 'a)]
      (is (= a a))
      (is (= a (with-meta a {:k 1}))
          "withMeta copies share the id field and must stay equal")
      (is (not= a b)
          "fresh variables with the same print name are distinct")
      (is (not= a 'a) "a variable is never equal to a plain symbol")
      (is (not= a nil)))))

(deftest lvar-named-equality-contract
  (testing "non-unique variables compare by identity of the name object"
    (let [n (symbol "shared-name")
          v1 (l/lvar n false)
          v2 (l/lvar n false)]
      (is (= v1 v2)
          "name-constructed variables sharing the name object are equal")
      (is (= v2 v1) "equality must be symmetric"))))

(deftest lvar-hash-and-substitution-key-contract
  (testing "hashing is meta-stable and equal copies resolve map bindings"
    (let [v (l/lvar 'v)
          copy (with-meta v {:tag :copy})
          s (l/ext l/empty-s v 42)]
      (is (= (.hashCode v) (.hashCode copy)))
      (is (= 42 (l/walk* s v)))
      (is (= 42 (l/walk* s copy))
          "an equal copy of the key must walk to the same binding"))))

(deftest lvar-equality-in-unification
  (testing "var-var unification still recognizes identical variables"
    (is (= '(:ok) (l/run 1 [q]
                    (l/fresh [x]
                      (== x x)
                      (== q :ok)))))
    (is (= '(7) (l/run 1 [q]
                  (l/fresh [x y]
                    (== x y)
                    (== y 7)
                    (== q x)))))))
