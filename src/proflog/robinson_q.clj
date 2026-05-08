(ns proflog.robinson-q
  "Robinson arithmetic Q as Proflog data.

   Q is expressed over function symbols `zero`, `s/1`, `add/2`, and `mul/2`.
   The formulas here can be used in two different ways: as ordinary antecedent
   assumptions for the existing tableau kernel, or as the language selected by
   the `:robinson-q` deduction-modulo proof profile."
  (:require [proflog.ast :as ast]
            [proflog.language :as language]))

(def zero
  "The Q constant `zero` as a term."
  (ast/app-term 'zero))

(defn s
  "Construct the Q successor term."
  [term]
  (ast/app-term 's term))

(defn add
  "Construct a Q addition term."
  [left right]
  (ast/app-term 'add left right))

(defn mul
  "Construct a Q multiplication term."
  [left right]
  (ast/app-term 'mul left right))

(defn numeral
  "Construct the standard Q numeral for a non-negative host integer."
  [n]
  (when (neg? n)
    (throw (ex-info "Robinson Q numerals are non-negative"
                    {:n n})))
  (if (zero? n)
    zero
    (s (numeral (dec n)))))

(defn eq
  "Construct an equality formula."
  [left right]
  (ast/eq-lit left right))

(defn neq
  "Construct a disequality formula."
  [left right]
  (ast/neq-lit left right))

(def language
  "Robinson Q's ordinary first-order language."
  (language/language
    {:constants ['zero]
     :functions {'s 1
                 'add 2
                 'mul 2}
     :relations {}}))

(def profile-language
  "The same Q language with the deduction-modulo profile selected."
  (language/language
    {:constants ['zero]
     :functions {'s 1
                 'add 2
                 'mul 2}
     :relations {}
     :proof-profile :robinson-q}))

(defn- and*
  "Conjoin a finite collection of formulae."
  [formulae]
  (case (count formulae)
    0 (ast/true-form)
    1 (first formulae)
    (reduce ast/and-form formulae)))

(def q1
  "Q1: every successor is distinct from zero."
  (ast/nom x
    (ast/forall-form
      x
      (neq (s (ast/var-term x)) zero))))

(def q2
  "Q2: successor is injective."
  (ast/nom x y
    (ast/forall-form
      x
      (ast/forall-form
        y
        (ast/implies-form
          (eq (s (ast/var-term x)) (s (ast/var-term y)))
          (eq (ast/var-term x) (ast/var-term y)))))))

(def q3
  "Q3: every nonzero value has a predecessor."
  (ast/nom x y
    (ast/forall-form
      x
      (ast/implies-form
        (neq (ast/var-term x) zero)
        (ast/exists-form
          y
          (eq (ast/var-term x) (s (ast/var-term y))))))))

(def q4
  "Q4: right-zero addition."
  (ast/nom x
    (ast/forall-form
      x
      (eq (add (ast/var-term x) zero)
          (ast/var-term x)))))

(def q5
  "Q5: right-successor addition."
  (ast/nom x y
    (ast/forall-form
      x
      (ast/forall-form
        y
        (eq (add (ast/var-term x) (s (ast/var-term y)))
            (s (add (ast/var-term x) (ast/var-term y))))))))

(def q6
  "Q6: right-zero multiplication."
  (ast/nom x
    (ast/forall-form
      x
      (eq (mul (ast/var-term x) zero)
          zero))))

(def q7
  "Q7: right-successor multiplication."
  (ast/nom x y
    (ast/forall-form
      x
      (ast/forall-form
        y
        (eq (mul (ast/var-term x) (s (ast/var-term y)))
            (add (mul (ast/var-term x) (ast/var-term y))
                 (ast/var-term x)))))))

(def q3-add-one-predecessor
  "A Q3-dependent theorem: every nonzero value is one more than something.

   Q5 and Q4 reduce `add(y, s(zero))` to `s(y)`, so this theorem is Q3
   expressed through the addition symbols. It is used to test that the
   deduction-modulo profile can use Q3 inside a larger refutation, not only to
   prove Q3's own direct refutation shape.
   "
  (ast/nom x y
    (ast/forall-form
      x
      (ast/implies-form
        (neq (ast/var-term x) zero)
        (ast/exists-form
          y
          (eq (add (ast/var-term y) (s zero))
              (ast/var-term x)))))))

(def axioms
  "The seven Robinson Q axiom formulas with stable labels."
  [[:q1 q1]
   [:q2 q2]
   [:q3 q3]
   [:q4 q4]
   [:q5 q5]
   [:q6 q6]
   [:q7 q7]])

(def axiom-formula
  "The conjunction Q1 and ... and Q7."
  (and* (map second axioms)))

(defn q-implies
  "Build the ordinary-theory theorem shape `Q1 and ... and Q7 -> theorem`."
  [theorem]
  (ast/implies-form axiom-formula theorem))

(def ordinary-program
  "An empty program over Q's ordinary language."
  (language/compile-program language []))

(def profile-program
  "An empty program over Q's `:robinson-q` profiled language."
  (language/compile-program profile-language []))
