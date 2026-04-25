(ns proflog.kernel-support
  "Shared branch-state relations used by the pure kernel and answer overlay.

   ADR-0015 separates answer-mode execution from the ordinary proof kernel, but
   both layers still rely on the same structural machinery for:
   - complementary literal closure,
   - L-ground admissibility,
   - bounded fuel stepping,
   - saved disequality maintenance,
   - and proof-variable-only disequality closure.

   Keeping these utilities in one namespace preserves a single semantic
   definition for the proof core while allowing the answer overlay to add its
   own residual and call-depth behavior above that core."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer [== conde fail fresh lcons membero project]]
            [proflog.ast :as ast]
            [proflog.equality :as equality]
            [proflog.subst :as subst]))

(defn complementary-lito
  "Succeed when `lit` closes directly against a saved complementary atom.

   The resulting bindings are returned through `sigma-out`, so later proof
   obligations on the same branch can observe them."
  [lit lits sigma sigma-out proof]
  (conde
   [(fresh [atom opposite atom-proof]
           (== (list 'pos atom) lit)
           (membero (list 'neg opposite) lits)
           (equality/atom-unifyo atom opposite sigma sigma-out atom-proof)
           (== '(close) proof))]
   [(fresh [atom opposite atom-proof]
           (== (list 'neg atom) lit)
           (membero (list 'pos opposite) lits)
           (equality/atom-unifyo atom opposite sigma sigma-out atom-proof)
           (== '(close) proof))]))

(defn walk-term-pure
  "Purely walk one term through the explicit substitution `sigma`."
  [term sigma]
  (case (ast/tag-of term)
    var (if-let [value (subst/lookup-binding sigma (second term))]
          (recur value sigma)
          term)
    par (if-let [value (subst/lookup-binding sigma (second term))]
          (recur value sigma)
          term)
    app (apply ast/app-term
               (second term)
               (map #(walk-term-pure % sigma) (nnext term)))
    term))

(defn same-walked-term?
  "Host-side structural equality on already-walked terms."
  [left right]
  (let [left-tag (ast/tag-of left)
        right-tag (ast/tag-of right)]
    (and (= left-tag right-tag)
         (case left-tag
           var (= (second left) (second right))
           par (= (second left) (second right))
           app (and (= (second left) (second right))
                    (= (count (nnext left)) (count (nnext right)))
                    (every? true? (map same-walked-term? (nnext left) (nnext right))))
           (= left right)))))

(defn contradictory-neq-pairs
  "Return the saved disequalities that are already false under `sigma`."
  [neqs sigma]
  (vec
    (filter (fn [[left right]]
              (same-walked-term?
                (walk-term-pure left sigma)
                (walk-term-pure right sigma)))
            neqs)))

(defn prune-contradictory-neqs
  "Drop saved disequalities that have become false under `sigma`."
  [neqs sigma]
  (apply list
         (remove (set (contradictory-neq-pairs neqs sigma))
                 neqs)))

(defn prune-contradictory-neqso
  "Relate `neqs-out` to `neqs` with all already-false disequalities removed."
  [neqs sigma neqs-out]
  (project [neqs sigma]
    (== (prune-contradictory-neqs neqs sigma) neqs-out)))

(defn stable-neqso
  "Succeed when every saved disequality remains genuinely open under `sigma`."
  [neqs sigma]
  (project [neqs sigma]
    (if (empty? (contradictory-neq-pairs neqs sigma))
      (== 'stable 'stable)
      fail)))

(declare l-ground-term*o)

(defn l-ground-termo
  "Succeed when `term` is in the object language `L`, i.e. contains no `par`.

   This relation is structural rather than projected: explicit object-language
   variables are admissible, constructor terms recurse through their arguments,
   and any unresolved `(par ...)` term causes failure."
  [term]
  (conde
   [(fresh [binding-nom]
           (== (list 'var binding-nom) term))]
   [(fresh [head args]
           (== (lcons 'app (lcons head args)) term)
           (l-ground-term*o args))]))

(defn l-ground-term*o
  "Succeed when every term in `terms` stays inside the object language `L`."
  [terms]
  (conde
   [(== '() terms)]
   [(fresh [head tail]
           (== (lcons head tail) terms)
           (l-ground-termo head)
           (l-ground-term*o tail))]))

(defn step-fuelo
  "Consume one unit of bounded search control.

   The budget is reserved for the potentially unbounded expansion points:
   quantifier instantiation and recursive procedure calls. Structural branch
   processing remains unrestricted under one budget slice once the current
   branch formulas are fixed.

   `nil` means unbounded search. A budget of `0` blocks any further bounded
   expansions while still allowing direct closure on the current branch."
  [fuel next-fuel]
  (project [fuel]
    (cond
      (nil? fuel) (== next-fuel nil)
      (> fuel 0) (== next-fuel (dec fuel))
      :else fail)))

(defn next-call-depth
  "Decrease the answer-mode call unfolding budget when it is bounded."
  [call-depth]
  (when (some? call-depth)
    (max 0 (dec call-depth))))

(defn proof-bindingso
  "Succeed when every binding added during proof search targets a γ-introduced
   proof variable rather than an explicit user-level `(var ...)`."
  [bindings proof-vars]
  (conde
   [(== '() bindings)]
   [(fresh [binding-nom value rest]
           (== (lcons [binding-nom value] rest) bindings)
           (membero binding-nom proof-vars)
           (proof-bindingso rest proof-vars))]))
