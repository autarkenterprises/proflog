(ns proflog.core-logic-ground-walk-test
  "ADR-0090 focused regression: ground-term walk fast path in the vendored
   core.logic overlay.

   The fast path must be observable (bound ground trees are tagged and walk
   to the identical stored object; copy-on-write rebuilds return identical
   objects for unchanged children) without changing any unification,
   disequality, or occurs-check semantics, and without ever tagging a term
   that still contains a logic variable or a structure outside the
   conservative ground grammar."
  (:refer-clojure :exclude [==])
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.core.logic :as l :refer [==]]))

(def ^:private ground-key :clojure.core.logic/ground)

(defn- tagged? [v]
  (and (instance? clojure.lang.IMeta v)
       (true? (ground-key (meta v)))))

(deftest ground-walk-results-are-stable-fixed-points
  (testing "walking a ground tree yields a tagged value whose re-walk is identity"
    (let [t (list 1 2 (list 3 4 (list 5)))
          v [1 [2 3] 4]
          c (l/lcons 1 (l/lcons (list 2 3) 7))]
      (doseq [original [t v c]]
        (let [w1 (l/walk* l/empty-s original)
              w2 (l/walk* l/empty-s w1)]
          (is (= original w1) "walked value must be unchanged")
          (is (tagged? w1) "ground walk results must carry the ground tag")
          (is (identical? w1 w2)
              "re-walking a tagged ground result must be constant-time identity"))))))

(deftest ground-walk-tags-bound-ground-trees
  (testing "ext stores ground values tagged so later walks are constant"
    (let [u (l/lvar 'u)
          g (list 1 (list 2 3) [4 5])
          s (l/ext l/empty-s u g)
          w1 (l/walk* s u)
          w2 (l/walk* s u)]
      (is (= g w1) "walked value must be unchanged")
      (is (identical? w1 w2)
          "repeat walks of a bound ground tree must return the same object")
      (is (tagged? w1) "the stored bound ground tree should carry the ground tag"))))

(deftest ground-walk-never-tags-variable-or-foreign-terms
  (testing "terms containing variables or non-grammar structures stay untagged"
    (let [u (l/lvar 'u)
          v (l/lvar 'v)
          s (l/ext l/empty-s u (list 1 v))
          w (l/walk* s u)]
      (is (= (list 1 v) w))
      (is (not (tagged? w))
          "a tree containing an unbound variable must not be tagged"))
    (let [u (l/lvar 'u)
          s (l/ext l/empty-s u {:a 1})
          w (l/walk* s u)]
      (is (= {:a 1} w))
      (is (not (tagged? w))
          "maps are outside the conservative ground grammar and must not be tagged"))))

(deftest ground-walk-preserves-logic-semantics
  (testing "unification, disequality, and occurs rejection are unchanged"
    (is (= '((1 2)) (l/run 1 [q] (l/fresh [a]
                                   (== a (list 1 2))
                                   (== a (list 1 2))
                                   (== q a)))))
    (is (= '((1 3)) (l/run 1 [q]
                      (l/!= q (list 1 2))
                      (== q (list 1 3)))))
    (is (= '() (l/run 1 [q] (== q (list 1 q))))
        "occurs check must still reject x = f(x) through the tagged paths")
    (is (= '(5) (l/run 1 [q] (l/fresh [a b]
                               (== a (list 1 (list 2 b)))
                               (== a (list 1 (list 2 5)))
                               (== q b))))
        "partially ground trees must still unify through rebuilt structure")))
