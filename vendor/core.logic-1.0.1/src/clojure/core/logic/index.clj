(ns clojure.core.logic.index
  "ADR-0107: a pure indexed relational lookup over a fixed finite table with
   non-negative integer keys.

   The lookup is deterministic on a ground key and enumerating on a free key, by
   *structure* rather than by any cut: the key is related to its fixed-width bit
   decomposition (a finite-domain relation), and those bits descend a trie of
   ground logic terms. A ground key follows one path by unification (the other
   child fails at the discriminating `==`); a free key enumerates the trie's
   present leaves exactly once. No `project`, `conda`, `condu`, or host-side
   lookup is used.

   This lives in its own namespace because it depends on `clojure.core.logic.fd`,
   which the base `clojure.core.logic` cannot (fd is built on top of it)."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :as l :refer [== conde fresh lcons]]
            [clojure.core.logic.fd :as fd]))

(defn- bit-width
  "Number of bits needed to represent keys 0..max-key (at least 1)."
  [max-key]
  (loop [w 1]
    (if (> (bit-shift-left 1 w) max-key) w (recur (inc w)))))

(defn int-index
  "Build a descent trie from `entries`, a seq of `[non-neg-int-key value]`.

   Returns `{:width w :tree trie}` where `trie` is a perfect binary tree of
   nested 2-vectors `[left right]` of depth `w`; each leaf is `[:present value]`
   for a present key or `[:absent]` otherwise. Built once from ground host data."
  [entries]
  (let [m (reduce (fn [acc [k v]] (assoc acc k v)) {} entries)
        max-key (reduce max 0 (keys m))
        width (bit-width max-key)
        build (fn build [prefix depth]
                (if (= depth width)
                  (if (contains? m prefix) [:present (get m prefix)] [:absent])
                  [(build (bit-shift-left prefix 1) (inc depth))
                   (build (bit-or (bit-shift-left prefix 1) 1) (inc depth))]))]
    {:width width :tree (build 0 0)}))

(defn- fixed-bitso
  "Relate `bits` to a fresh list of exactly `n` finite-domain {0,1} vars.
   `n` is a ground host integer, so this only constructs goals (no search)."
  [bits n]
  (if (zero? n)
    (== bits '())
    (fresh [b rest]
      (== bits (lcons b rest))
      (fd/in b (fd/domain 0 1))
      (fixed-bitso rest (dec n)))))

(defn- bits-valueo
  "Relate MSB-first `bits` to non-negative integer `value` via fd, so a ground
   value determines the bits and a free value enumerates them."
  [bits value]
  (letfn [(acco [bits acc value]
            (conde
              [(== '() bits) (== acc value)]
              [(fresh [b rest acc2]
                 (== (lcons b rest) bits)
                 (fd/in acc2 (fd/interval 0 Integer/MAX_VALUE))
                 (fd/eq (= acc2 (+ (* 2 acc) b)))
                 (acco rest acc2 value))]))]
    (acco bits 0 value)))

(defn- descendo
  "Descend `tree` by `bits`, unifying the reached leaf with `leaf`.
   A ground bit selects exactly one child (the other branch fails at `== b _`)."
  [tree bits leaf]
  (conde
    [(== '() bits) (== tree leaf)]
    [(fresh [b rest left right child]
       (== (lcons b rest) bits)
       (== [left right] tree)
       (conde
         [(== b 0) (== child left)]
         [(== b 1) (== child right)])
       (descendo child rest leaf))]))

(defn int-indexo
  "Relate integer `key` to `value` through the trie `idx` built by `int-index`.

   Deterministic on a ground key (O(width)); enumerates present entries on a
   free key; backward on a ground value. Pure: no project/conda/host cut."
  [key value idx]
  (let [{:keys [width tree]} idx]
    (fresh [bits leaf]
      (fd/in key (fd/interval 0 (dec (bit-shift-left 1 width))))
      (fixed-bitso bits width)
      (bits-valueo bits key)
      (descendo tree bits leaf)
      (== leaf [:present value]))))
