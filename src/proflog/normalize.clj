(ns proflog.normalize
  "NNF conversion for the greenfield Proflog implementation.

   The surface layer may use explicit `not` and `implies`; the compiled core
   should retain only NNF connectives, quantifiers, and literals."
  (:require [proflog.ast :as ast]))

(declare to-nnf negate-formula)

(defn negate-formula
  "Return the NNF negation of `formula`.

   This function is total over the greenfield surface language, not only over
   formulas that are already in NNF. That matters because negative procedure
   calls will eventually need a reliable way to negate surface bodies as well as
   previously normalized ones."
  [formula]
  (let [tag (ast/tag-of formula)]
    (case tag
      true (ast/false-form)
      false (ast/true-form)
      pos (ast/neg-lit (second formula))
      neg (ast/pos-lit (second formula))
      eq (ast/neq-lit (second formula) (nth formula 2))
      neq (ast/eq-lit (second formula) (nth formula 2))
      and (ast/or-form (negate-formula (second formula))
                       (negate-formula (nth formula 2)))
      or (ast/and-form (negate-formula (second formula))
                       (negate-formula (nth formula 2)))
      forall (let [tied (second formula)]
               (ast/exists-form (:binding-nom tied)
                                (negate-formula (:body tied))))
      once-forall (let [tied (second formula)]
                    (ast/exists-form (:binding-nom tied)
                                     (negate-formula (:body tied))))
      exists (let [tied (second formula)]
               ;; Negated existential clause bodies are operationally
               ;; single-use: instantiate once on the current branch rather
               ;; than re-enqueueing an ordinary universal indefinitely.
               (ast/once-forall-form (:binding-nom tied)
                                     (negate-formula (:body tied))))
      not (to-nnf (second formula))
      implies (negate-formula (to-nnf formula))
      (throw (ex-info "Unsupported formula for NNF negation"
                      {:formula formula})))))

(defn to-nnf
  "Compile a greenfield surface formula into NNF."
  [formula]
  (let [tag (ast/tag-of formula)]
    (case tag
      true formula
      false formula
      pos formula
      neg formula
      eq formula
      neq formula
      and (ast/and-form (to-nnf (second formula))
                        (to-nnf (nth formula 2)))
      or (ast/or-form (to-nnf (second formula))
                      (to-nnf (nth formula 2)))
      not (negate-formula (second formula))
      implies (to-nnf (ast/or-form (ast/not-form (second formula))
                                   (nth formula 2)))
      forall (let [tied (second formula)]
               (ast/forall-form (:binding-nom tied)
                                (to-nnf (:body tied))))
      once-forall (let [tied (second formula)]
                    (ast/once-forall-form (:binding-nom tied)
                                          (to-nnf (:body tied))))
      exists (let [tied (second formula)]
               (ast/exists-form (:binding-nom tied)
                                (to-nnf (:body tied))))
      (throw (ex-info "Unsupported formula for NNF compilation"
                      {:formula formula})))))
