(ns proflog.combinatory-logic
  "SKI combinatory logic programs for ADR-0046.

   The semantics are ordinary Proflog clauses written through the ADR-0010
   frontend. Host helpers in this namespace build object-language terms for
   examples; they do not reduce or evaluate SKI terms."
  (:require [proflog.ast :as ast]
            [proflog.frontend :as pf]))

;; -----------------------------------------------------------------------------
;; Object-language signature
;;
;; `ap/2` is the application constructor. `scomb`, `kcomb`, and `icomb` are the
;; primitive combinators. The remaining constants are inert data used by worked
;; examples and tests.
;; -----------------------------------------------------------------------------

(def ski-language
  (pf/language
    (constants zero
               scomb kcomb icomb
               a b c)
    (functions (s 1)
               (ap 2))
    (relations (step 2)
               (eval-for 3))))

(defn c
  "Construct a nullary object-language constant."
  [sym]
  (ast/app-term sym))

(defn ap
  "Construct object-language application."
  [left right]
  (ast/app-term 'ap left right))

(defn numeral
  "Construct a Peano numeral term for bounded evaluation examples."
  [n]
  (if (zero? n)
    (c 'zero)
    (ast/app-term 's (numeral (dec n)))))

(defn true-term
  "Boolean true encoded as the K combinator."
  []
  (c 'kcomb))

(defn false-term
  "Boolean false encoded as K I."
  []
  (ap (c 'kcomb) (c 'icomb)))

(defn choose
  "Build `(boolean then else)` in curried SKI application form."
  [boolean-term then-term else-term]
  (ap (ap boolean-term then-term) else-term))

(defn skk
  "Build `S K K x`, the usual SKI identity expression."
  [x]
  (ap (ap (ap (c 'scomb) (c 'kcomb)) (c 'kcomb)) x))

;; -----------------------------------------------------------------------------
;; Program
;; -----------------------------------------------------------------------------

(def ski-program
  (pf/proflog ski-language
    (|- (step before after)
      (exists [x]
        (and (= before (ap icomb x))
             (= after x))))

    (|- (step before after)
      (exists [x y]
        (and (= before (ap (ap kcomb x) y))
             (= after x))))

    (|- (step before after)
      (exists [x y z]
        (and (= before (ap (ap (ap scomb x) y) z))
             (= after (ap (ap x z) (ap y z))))))

    ;; Reduce the function side of an application. This is enough to evaluate
    ;; ordinary left-associated SKI programs such as `((K I) a) b`, while still
    ;; leaving the reduction strategy explicit in the Proflog program.
    (|- (step before after)
      (exists [function argument reduced-function]
        (and (= before (ap function argument))
             (step function reduced-function)
             (= after (ap reduced-function argument)))))

    (|- (eval-for steps start final)
      (and (= steps zero)
           (= start final)))

    (|- (eval-for steps start final)
      (exists [rest middle]
        (and (= steps (s rest))
             (step start middle)
             (eval-for rest middle final))))))

(defn program
  "Return the compiled SKI program."
  []
  ski-program)
