(ns proflog.core-logic-indexed-lookup-test
  "ADR-0107 contract for a pure indexed relational lookup over a fixed finite
   table. The lookup must be deterministic on a ground key (no spurious choice
   points / no double-count), sound and enumerating on a free key, and agree
   with a linear `membero` baseline -- all by structure, with no `project`,
   `conda`, `condu`, or host-side cut."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :as l :refer [== fresh or* run run*]]
            [clojure.core.logic.index :as idx]
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
  (let [tbl (idx/int-index entries)]
    (testing "forward (ground key) yields exactly its value, no double-count"
      (is (= '(:c) (run* [v] (idx/int-indexo 2 v tbl))))
      (is (= 1 (count (run* [v] (idx/int-indexo 2 v tbl))))))
    (testing "forward on an absent key fails"
      (is (= '() (run* [v] (idx/int-indexo 42 v tbl)))))
    (testing "backward (ground value) yields its key"
      (is (= '(5) (run* [k] (idx/int-indexo k :f tbl)))))
    (testing "free key/value enumerates every entry exactly once"
      (is (= (set entries)
             (set (run* [q] (fresh [k v] (idx/int-indexo k v tbl) (== [k v] q))))))
      (is (= (count entries)
             (count (run* [q] (fresh [k v] (idx/int-indexo k v tbl) (== [k v] q)))))))
    (testing "agrees with the linear baseline in every mode"
      (is (= (set (run* [q] (fresh [k v] (linear-lookupo k v) (== [k v] q))))
             (set (run* [q] (fresh [k v] (idx/int-indexo k v tbl) (== [k v] q))))))
      (is (= (run* [v] (linear-lookupo 6 v))
             (run* [v] (idx/int-indexo 6 v tbl)))))))
