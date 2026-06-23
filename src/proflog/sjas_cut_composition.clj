(ns proflog.sjas-cut-composition
  "Willard 2002 JSL2 Theorem 2.2 as an explicit, verified proof-COMPOSITION.

   Theorem 2.2 (a corollary of Gentzen's Cut Elimination Theorem): if
   `alpha |-_S Lambda`, `alpha |-_S Theta`, and `alpha |-_S Lambda /\\ Theta => Xi`,
   then `alpha |-_S Xi`. Willard's proof of Theorem 2.3 invokes Theorem 2.2 three
   times (Eqs 6, 8, 9) to assemble `D*`, `DK == D*`, and `DK`.

   For semantic tableaux the combined proof `t` is constructed with a CUT
   (lemma) step and its cut-free form is only guaranteed to *exist* by Gentzen's
   theorem -- `t`'s cut-free length can be super-exponentially larger than the
   combined lengths of the three inputs (JSL2 line 303-306). This namespace
   therefore realizes the *with-cut* composition concretely and verifiably, and
   treats the cut-free expansion as the documented research boundary
   (`cut-free-expansion-boundary`).

   Honesty contract. The three leaf sub-proofs are genuine kernel entailments
   witnessed by the ordinary tableau prover (`proflog.query/query-succeeds`),
   never assumed. The only inference the composition adds over those leaves is
   the analytic CUT on a formula `A` (case split `A` vs `not A`), whose soundness
   is the trivial excluded-middle fact: a branch set is unsatisfiable if both
   `branch + A` and `branch + not A` are. `validate-cut-composition` re-runs the
   kernel on every leaf and checks the structural threading; it does not trust a
   supplied :closed? flag."
  (:require [proflog.ast :as ast]
            [proflog.query :as query]))

(defn entailment
  "Witness `alpha |-_S goal` by running the ordinary kernel tableau prover over
   `program` (the compiled `alpha`). Returns
   `{:goal goal :closed? bool :proof <first kernel proof or nil>}`.

   `:closed?` reflects the actual prover result, so it cannot be forged: any
   downstream validation re-derives it."
  ([program goal]
   (entailment program goal {}))
  ([program goal {:keys [proof-limit fuel] :or {proof-limit 1 fuel 400}}]
   (let [proofs (query/query-succeeds program goal proof-limit fuel)]
     {:goal goal
      :closed? (boolean (seq proofs))
      :proof (first proofs)})))

(defn compose-by-cut
  "Build the Theorem 2.2 cut-composition proof object for `xi` from entailments
   of `Lambda`, `Theta`, and `Lambda /\\ Theta => Xi`.

   Tree shape (root is the refutation goal `not Xi`):

     cut on Lambda
       |- branch {not Lambda}        closed by the Lambda lemma
       |- branch {Lambda}: cut on Theta
            |- branch {not Theta}              closed by the Theta lemma
            |- branch {Lambda, Theta, not Xi}  closed by the implication lemma
                                               (= refutation of Lambda/\\Theta/\\not Xi)

   The object is pure data; `validate-cut-composition` decides its correctness."
  [ent-lambda ent-theta ent-impl xi]
  {:rule :cut
   :conclusion xi
   :cut-formula (:goal ent-lambda)
   :neg-branch {:rule :lemma :closes (:goal ent-lambda) :entailment ent-lambda}
   :pos-branch {:rule :cut
                :cut-formula (:goal ent-theta)
                :neg-branch {:rule :lemma
                             :closes (:goal ent-theta)
                             :entailment ent-theta}
                :pos-branch {:rule :lemma
                             :closes (:goal ent-impl)
                             :entailment ent-impl}}})

(defn composition-size
  "Count nodes in a cut-composition object (the with-cut proof size)."
  [node]
  (case (:rule node)
    :lemma 1
    :cut (+ 1
            (composition-size (:neg-branch node))
            (composition-size (:pos-branch node)))
    0))

(defn- leaf-entailments
  "Return the three leaf entailments [lambda theta impl] of a cut composition."
  [composite]
  [(get-in composite [:neg-branch :entailment])
   (get-in composite [:pos-branch :neg-branch :entailment])
   (get-in composite [:pos-branch :pos-branch :entailment])])

(defn validate-cut-composition
  "Validate a Theorem 2.2 cut composition against `program` (= `alpha`).

   Re-derives each leaf entailment with the ordinary prover (never trusting the
   stored :closed? flag), checks that the implication leaf's goal is exactly
   `Lambda /\\ Theta => Xi` for this composition's `Lambda`, `Theta`, `Xi`, and
   returns a report. When `:valid?` is true the composition is a sound
   with-cut derivation of `alpha |- Xi`.

   `formula=` decides goal identity (default `=`; pass a canonicalizer for
   formulas containing binders)."
  ([program composite]
   (validate-cut-composition program composite =))
  ([program composite formula=]
   (let [[lam theta impl] (leaf-entailments composite)
         xi (:conclusion composite)
         expected-impl (ast/implies-form
                         (ast/and-form (:goal lam) (:goal theta))
                         xi)
         lam* (entailment program (:goal lam))
         theta* (entailment program (:goal theta))
         impl* (entailment program (:goal impl))
         impl-shape-ok? (formula= expected-impl (:goal impl))
         leaves-closed? (and (:closed? lam*) (:closed? theta*) (:closed? impl*))]
     {:valid? (boolean (and impl-shape-ok? leaves-closed?))
      :impl-shape-ok? impl-shape-ok?
      :lambda-closed? (:closed? lam*)
      :theta-closed? (:closed? theta*)
      :impl-closed? (:closed? impl*)
      :conclusion xi
      :with-cut-size (composition-size composite)})))

(def cut-free-expansion-boundary
  "Documentation of the part of Theorem 2.2 that is NOT materialized here.

   `compose-by-cut` builds the proof *with* an analytic cut. Theorem 2.2's full
   content is that a *cut-free* semantic-tableau proof of `Xi` also exists
   (Gentzen). That cut-free proof is what an ordinary cut-free checker would
   accept directly, but its size can be super-exponential in the combined leaf
   sizes (JSL2 Sketch, line 303-306), so it is not constructed. This is the
   genuine research boundary flagged by ADR-0142 criterion 6: a verified
   composition transformation is provided; the explicit super-exponential
   cut-free expansion is left open."
  {:provided :with-cut-composition
   :guaranteed-but-not-materialized :cut-free-tableau
   :reason :gentzen-cut-elimination-superexponential-blowup
   :source "Willard 2002 JSL2 Theorem 2.2, proof sketch"})
