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
   own residual and call-depth behavior above that core.

   For a reader of Fitting's paper, this file is best understood as the shared
   operational scaffolding around the tableau rules:

   - direct complementary literal closure,
   - object-language admissibility checks for procedure calls,
   - bounded search bookkeeping,
   - and branch-state maintenance that is logically secondary to the core
     tableau rules but operationally necessary in this implementation."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer [== conde fail fresh lcons membero project]]
            [proflog.ast :as ast]
            [proflog.equality :as equality]
            [proflog.subst :as subst]))

;; Reading guide
;; -------------
;;
;; Most of this namespace falls into one of two categories:
;;
;; 1. genuinely relational helpers shared by kernel and answer overlay,
;; 2. small host-side support routines used only for operational bookkeeping.
;;
;; The second category is deliberately kept narrow and explicit so the semantic
;; heart of the prover remains in `proflog.kernel`, `proflog.answer-overlay`,
;; and `proflog.equality`.

;; ---------------------------------------------------------------------------
;; Complementary literal closure
;; ---------------------------------------------------------------------------
;;
;; This is the direct branch-closure rule for saved positive / negative atom
;; pairs. Unlike purely syntactic closure, the greenfield kernel allows the two
;; atoms to close after walking and unifying their arguments under the current
;; equality substitution.

(defn selecto
  "Relational selection from one pending-agenda list.

   `x` is one chosen element of `lst`, and `rest` is `lst` with exactly that
   one occurrence removed.

   The agenda-based kernel rewrite uses this as its fairness hook: instead of
   always expanding the leftmost pending formula, the prover may expose several
   next-step choices to core.logic and let the underlying search interleave
   them relationally."
  [x lst rest]
  (conde
   [(fresh [tail]
           (== (lcons x tail) lst)
           (== tail rest))]
   [(fresh [head tail tail-rest]
           (== (lcons head tail) lst)
           (== (lcons head tail-rest) rest)
           (selecto x tail tail-rest))]))

(defn complementary-lito
  "Succeed when `lit` closes directly against a saved complementary atom.

   The resulting bindings are returned through `sigma-out`, so later proof
   obligations on the same branch can observe them.

   In Fitting-style terms, this is the ordinary literal-closure rule lifted to
   the explicit equality state of the greenfield kernel."
  [lit lits sigma sigma-out proof]
  (conde
   ;; Current positive literal closes against a saved negative literal.
   [(fresh [atom opposite atom-proof]
           (== (list 'pos atom) lit)
           (membero (list 'neg opposite) lits)
           (equality/atom-unifyo atom opposite sigma sigma-out atom-proof)
           (== '(close) proof))]
   ;; Symmetric negative-current / positive-saved case.
   [(fresh [atom opposite atom-proof]
           (== (list 'neg atom) lit)
           (membero (list 'pos opposite) lits)
           (equality/atom-unifyo atom opposite sigma sigma-out atom-proof)
           (== '(close) proof))]))

;; ---------------------------------------------------------------------------
;; Host-side normalization helpers
;; ---------------------------------------------------------------------------
;;
;; The next few functions are intentionally not the core semantic relations.
;; They are host-side mirrors of the equality walking logic, used only for
;; support tasks such as pruning already-false disequalities. Keeping them here
;; avoids duplicating larger relational searches for simple maintenance checks.

(defn walk-term-pure
  "Purely walk one term through the explicit substitution `sigma`.

   This is the host-side analogue of `proflog.equality/walk*o`. It exists so
   support code can inspect the already-determined branch state without opening
   additional relational search."
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
  "Host-side structural equality on already-walked terms.

   This is the support-layer analogue of `proflog.equality/same-termo`, but it
   assumes both inputs have already been normalized by `walk-term-pure`."
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
  "Return the saved disequalities that are already false under `sigma`.

   A saved disequality becomes contradictory exactly when its two sides walk to
   the same term under the current equality substitution."
  [neqs sigma]
  (vec
    (filter (fn [[left right]]
              (same-walked-term?
                (walk-term-pure left sigma)
                (walk-term-pure right sigma)))
            neqs)))

(defn prune-contradictory-neqs
  "Drop saved disequalities that have become false under `sigma`.

   Operationally, once a disequality has already served to close a branch, the
   remaining proof search should not keep carrying it around as stale state."
  [neqs sigma]
  (apply list
         (remove (set (contradictory-neq-pairs neqs sigma))
                 neqs)))

(defn prune-contradictory-neqso
  "Relate `neqs-out` to `neqs` with all already-false disequalities removed.

   This is one of the intentionally narrow `project` boundaries in the support
   layer: it computes a deterministic maintenance result from an already-known
   branch state."
  [neqs sigma neqs-out]
  (project [neqs sigma]
    (== (prune-contradictory-neqs neqs sigma) neqs-out)))

(defn stable-neqso
  "Succeed when every saved disequality remains genuinely open under `sigma`.

   The kernel uses this guard before continuing past a positive equality step:
   if some saved disequality has already collapsed to reflexivity, the branch
   should have closed instead of continuing."
  [neqs sigma]
  (project [neqs sigma]
    (if (empty? (contradictory-neq-pairs neqs sigma))
      (== 'stable 'stable)
      fail)))

(declare l-ground-term*o)

;; ---------------------------------------------------------------------------
;; Object-language admissibility (`L`-groundness)
;; ---------------------------------------------------------------------------
;;
;; Fitting's Procedure Call Rule applies only to atoms in the object language
;; `L`, not to atoms still containing delta parameters from existential
;; expansion. The greenfield kernel expresses that restriction structurally:
;;
;; - free proof variables `(var ...)` are allowed,
;; - constructor applications recurse through their arguments,
;; - unresolved rigid parameters `(par ...)` are rejected.

(defn l-ground-termo
  "Succeed when `term` is in the object language `L`, i.e. contains no `par`.

   This relation is structural rather than projected: explicit object-language
   variables are admissible, constructor terms recurse through their arguments,
   and any unresolved `(par ...)` term causes failure.

   That is exactly the operational condition the procedure-call rules need:
   proof variables may still stand for unknown object terms, but rigid delta
   witnesses must not leak into ordinary object-language calls."
  [term]
  (conde
   ;; An object-language variable is always admissible.
   [(fresh [binding-nom]
           (== (list 'var binding-nom) term))]
   ;; Constructor term: admissible only if every argument is also in `L`.
   [(fresh [head args]
           (== (lcons 'app (lcons head args)) term)
           (l-ground-term*o args))]))

(defn l-ground-term*o
  "Succeed when every term in `terms` stays inside the object language `L`.

   List-valued companion to `l-ground-termo`."
  [terms]
  (conde
   [(== '() terms)]
   [(fresh [head tail]
           (== (lcons head tail) terms)
           (l-ground-termo head)
           (l-ground-term*o tail))]))

(defn- declaration-order
  "Stable ordering for declared symbols during object-language enumeration."
  [entry]
  [(val entry) (str (key entry))])

(defn object-language-nullary-terms
  "Return the declared nullary object-language terms for `prog`.

   ADR-0018 uses these terms as explicit universal instantiation candidates
   for finite constant-only witness checks. Non-nullary constructor expansion
   remains a bounded answer-layer concern."
  [prog]
  (if (map? prog)
    (->> (get-in prog [:language :functions])
         (filter (fn [[_ arity]] (zero? arity)))
         (sort-by declaration-order)
         (mapv (fn [[sym _]] (ast/app-term sym))))
    []))

(defn object-language-nullary-termo
  "Relate `term` to one of the supplied declared nullary terms.

   This is an explicit, finite instantiation surface used by `once-forall`.
   It does not introduce internal parameters, and it fails when the term list
   is empty."
  [terms term]
  (if (seq terms)
    (membero term (apply list terms))
    fail))

;; ---------------------------------------------------------------------------
;; Bounded search bookkeeping
;; ---------------------------------------------------------------------------
;;
;; The next helpers are operational rather than logical. They keep the kernel's
;; bounded exploration policy explicit and shared between ordinary proof search
;; and the answer overlay.

(defn step-fuelo
  "Consume one unit of bounded proof-search micro-fuel.

   ADR-0016 charges this relation at every non-closing branch-progress step:
   agenda expansion, formula storage, quantifier instantiation, and recursive
   procedure calls. Immediate branch closures still work at fuel `0`, but a
   fuel slice can no longer perform an unbounded amount of structural work once
   the current branch formulas are fixed.

   `nil` means unbounded search. A budget of `0` blocks any further bounded
   expansions while still allowing direct closure on the current branch.

   This is another intentional `project` boundary: once the fuel value is known
   on the host side, the next budget is a deterministic operational choice."
  [fuel next-fuel]
  (project [fuel]
    (cond
      (nil? fuel) (== next-fuel nil)
      (> fuel 0) (== next-fuel (dec fuel))
      :else fail)))

(defn next-call-depth
  "Decrease the answer-mode call unfolding budget when it is bounded.

   Unlike `step-fuelo`, this helper is purely host-side because it is only used
   to derive the next answer-layer recursion budget once the current one is
   already known."
  [call-depth]
  (when (some? call-depth)
    (max 0 (dec call-depth))))

;; ---------------------------------------------------------------------------
;; Proof-variable discipline
;; ---------------------------------------------------------------------------
;;
;; A saved disequality may close by forcing equality only through proof-local
;; gamma variables. It must not silently bind user-facing answer variables.
;; This helper expresses that discipline explicitly.

(defn proof-bindingso
  "Succeed when every binding added during proof search targets a γ-introduced
   proof variable rather than an explicit user-level `(var ...)`.

   The kernel uses this when closing a symbolic disequality by unification:
   only genuinely proof-local variables may be instantiated as part of that
   contradiction witness."
  [bindings proof-vars]
  (conde
   ;; No new bindings to check.
   [(== '() bindings)]
   ;; Head binding must target a nom introduced by gamma on this proof branch.
   [(fresh [binding-nom value rest]
           (== (lcons [binding-nom value] rest) bindings)
           (membero binding-nom proof-vars)
           (proof-bindingso rest proof-vars))]))
