(ns proflog.core-logic-indexed-lookup-test
  "ADR-0107 contract for a pure indexed relational lookup over a fixed finite
   table. The lookup must be deterministic on a ground key (no spurious choice
   points / no double-count), sound and enumerating on a free key, and agree
   with a linear `membero` baseline -- all by structure, with no `project`,
   `conda`, `condu`, or host-side cut."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :as l :refer [== fresh or* run run*]]
            [clojure.test :refer [deftest is testing]]))

(def ^:private entries
  "[key value] pairs with small non-negative integer keys."
  [[0 :a] [1 :b] [2 :c] [3 :d] [4 :e] [5 :f] [6 :g] [7 :h]])

(defn- linear-lookupo
  "Baseline: the current `or*`-style linear table relation."
  [k v]
  (or*
    (map (fn [[ek ev]] (fresh [] (== k ek) (== v ev))) entries)))

(deftest indexed-lookup-matches-linear-baseline-in-all-modes
  (let [idx (l/int-indexo-build entries)]
    (testing "forward (ground key) yields exactly its value, no double-count"
      (is (= '(:c) (run* [v] (l/int-indexo 2 v idx))))
      (is (= 1 (count (run* [v] (l/int-indexo 2 v idx))))))
    (testing "forward on an absent key fails"
      (is (= '() (run* [v] (l/int-indexo 42 v idx)))))
    (testing "backward (ground value) yields its key"
      (is (= '(5) (run* [k] (l/int-indexo k :f idx)))))
    (testing "free key/value enumerates every entry exactly once"
      (is (= (set entries)
             (set (run* [q] (fresh [k v] (l/int-indexo k v idx) (== [k v] q))))))
      (is (= (count entries)
             (count (run* [q] (fresh [k v] (l/int-indexo k v idx) (== [k v] q)))))))
    (testing "agrees with the linear baseline in every mode"
      (is (= (set (run* [q] (fresh [k v] (linear-lookupo k v) (== [k v] q))))
             (set (run* [q] (fresh [k v] (l/int-indexo k v idx) (== [k v] q))))))
      (is (= (run* [v] (linear-lookupo 6 v))
             (run* [v] (l/int-indexo 6 v idx)))))))
