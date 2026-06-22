(ns proflog.sjas-boundary-axioms
  "Shared source specification for the ADR-0141 unsafe SJAS profiles.

   The source builder and arithmeticized proof checker both consume these
   formulas. Keeping one definition prevents a boundary certificate from
   checking a weaker host-side approximation of the axioms that were actually
   encoded into the finite system."
  (:require [clojure.core.logic :refer [lvar]]
            [clojure.core.logic.nominal :as nominal]
            [proflog.ast :as ast]
            [proflog.normalize :as normalize]
            [proflog.willard-sjas-code :as sjas-code]))

(def ^:private zero-symbol (symbol "0"))
(def ^:private one-symbol (symbol "1"))
(def zero (ast/app-term zero-symbol))
(def one (ast/app-term one-symbol))
(def two (ast/app-term 'dbl one))

(defn add-term [left right] (ast/app-term 'add left right))
(defn mul-term [left right] (ast/app-term 'mul left right))
(defn leq [left right] (ast/pos-lit (ast/app-term 'leq left right)))
(defn lt [left right] (ast/pos-lit (ast/app-term 'lt left right)))
(defn mult [left right product]
  (ast/pos-lit (ast/app-term 'mult left right product)))
(defn subst-code [source-code substituted-code]
  (ast/pos-lit (ast/app-term 'subst-code source-code substituted-code)))
(defn finax4 [alpha] (ast/pos-lit (ast/app-term 'finax4 alpha)))
(defn willard-map [alpha k d]
  (ast/pos-lit (ast/app-term 'willard-map alpha k d)))
(defn semprfk-alpha [alpha k theorem proof bound]
  (ast/pos-lit
    (ast/app-term 'semprfk-alpha alpha k theorem proof bound)))
(defn semprf-alpha [alpha theorem proof]
  (ast/pos-lit (ast/app-term 'semprf-alpha alpha theorem proof)))

(defn bounded-exists
  "Build the bounded existential syntax encoded by the SJAS formula grammar."
  [binding-nom bound body]
  (list 'bounded-exists
        (nominal/tie binding-nom {:bound bound :body body})))

(defn total-multiplication-willard-upsilon
  "Willard 2002 Equation (15)."
  [alpha k g h y z]
  (ast/and-form (subst-code g h)
                (semprfk-alpha alpha k h y z)))

(defn total-multiplication-willard-paradox
  "Willard 2002 Equation (12)."
  [y z alpha k]
  (let [d (nominal/nom (lvar 'willard-d))
        dt (ast/var-term d)]
    (ast/exists-form
      d
      (ast/and-form (willard-map alpha k dt)
                    (semprfk-alpha alpha k dt y z)))))

(defn total-multiplication-willard-v4-axiom
  "The proof-compressing V4 descent axiom from Willard 2002."
  []
  (let [alpha (nominal/nom (lvar 'willard-alpha))
        k (nominal/nom (lvar 'willard-k))
        g (nominal/nom (lvar 'willard-g))
        h (nominal/nom (lvar 'willard-h))
        y (nominal/nom (lvar 'willard-y))
        z (nominal/nom (lvar 'willard-z))
        hs (nominal/nom (lvar 'willard-h-star))
        ys (nominal/nom (lvar 'willard-y-star))
        zs (nominal/nom (lvar 'willard-z-star))
        at (ast/var-term alpha)
        kt (ast/var-term k)
        gt (ast/var-term g)
        ht (ast/var-term h)
        yt (ast/var-term y)
        zt (ast/var-term z)
        hst (ast/var-term hs)
        yst (ast/var-term ys)
        zst (ast/var-term zs)]
    (ast/forall-form
      alpha
      (ast/forall-form
        k
        (ast/forall-form
          g
          (ast/forall-form
            h
            (ast/forall-form
              y
              (ast/forall-form
                z
                (ast/implies-form
                  (total-multiplication-willard-upsilon at kt gt ht yt zt)
                  (bounded-exists
                    hs
                    ht
                    (bounded-exists
                      ys
                      yt
                      (bounded-exists
                        zs
                        zt
                        (total-multiplication-willard-upsilon
                          at kt gt hst yst zst)))))))))))))

(defn total-multiplication-willard-v5-axiom
  "Willard 2002 V5, specialized to the selected contradiction code."
  [contradiction-code-term]
  (let [y (nominal/nom (lvar 'willard-y))
        z (nominal/nom (lvar 'willard-z))
        alpha (nominal/nom (lvar 'willard-alpha))
        k (nominal/nom (lvar 'willard-k))
        proof (nominal/nom (lvar 'willard-proof))
        yt (ast/var-term y)
        zt (ast/var-term z)
        at (ast/var-term alpha)
        kt (ast/var-term k)
        pt (ast/var-term proof)
        antecedent
        (ast/and-form
          (finax4 at)
          (ast/and-form
            (leq at kt)
            (total-multiplication-willard-paradox yt zt at kt)))]
    (ast/forall-form
      y
      (ast/forall-form
        z
        (ast/forall-form
          alpha
          (ast/forall-form
            k
            (ast/implies-form
              antecedent
              (bounded-exists
                proof
                zt
                (ast/and-form
                  (lt pt zt)
                  (semprf-alpha at contradiction-code-term pt))))))))))

(defn total-multiplication-willard-route-axioms
  "Return the exact V4/V5 fragment required by the Type-M refutation rule."
  [contradiction-code-term]
  [(total-multiplication-willard-v4-axiom)
   (total-multiplication-willard-v5-axiom contradiction-code-term)])

(defn total-multiplication-complete-axioms
  "Return the reflected equational basis for total functional multiplication."
  []
  (let [x (nominal/nom (lvar 'x))
        y (nominal/nom (lvar 'y))
        z (nominal/nom (lvar 'z))
        xt (ast/var-term x)
        yt (ast/var-term y)
        zt (ast/var-term z)]
    [(ast/forall-form x
       (ast/eq-lit (mul-term xt zero) zero))
     (ast/forall-form x
       (ast/eq-lit (mul-term xt one) xt))
     (ast/forall-form x
       (ast/forall-form y
         (ast/eq-lit (mul-term xt (add-term yt one))
                     (add-term (mul-term xt yt) xt))))
     (ast/forall-form x
       (ast/forall-form y
         (ast/eq-lit (mul-term xt yt) (mul-term yt xt))))
     (ast/forall-form x
       (ast/forall-form y
         (ast/forall-form z
           (ast/eq-lit (mul-term (mul-term xt yt) zt)
                       (mul-term xt (mul-term yt zt))))))
     (ast/forall-form x
       (ast/forall-form y
         (ast/forall-form z
           (ast/eq-lit (mul-term xt (add-term yt zt))
                       (add-term (mul-term xt yt)
                                 (mul-term xt zt))))))]))

(defn boundary-arithmetic-basis-axioms
  "Return the finite arithmetic basis shared by Xtab and Tab-2."
  []
  (let [x (nominal/nom (lvar 'x))
        y (nominal/nom (lvar 'y))
        z (nominal/nom (lvar 'z))
        w (nominal/nom (lvar 'w))
        xt (ast/var-term x)
        yt (ast/var-term y)
        zt (ast/var-term z)
        wt (ast/var-term w)]
    [(ast/forall-form x
       (ast/eq-lit (add-term xt zero) xt))
     (ast/forall-form x
       (ast/eq-lit (add-term xt one) (add-term one xt)))
     (ast/forall-form x
       (mult xt zero zero))
     (ast/forall-form x
       (mult xt one xt))
     (ast/forall-form x
       (ast/forall-form y
         (ast/forall-form z
           (ast/forall-form w
             (ast/implies-form
               (ast/and-form (mult xt yt zt)
                             (mult xt yt wt))
               (ast/eq-lit zt wt))))))]))

(defn xtab-lem-witness-axioms
  "Return the reflected excluded-middle witness used by the Xtab profile."
  []
  (let [x (nominal/nom (lvar 'x))
        atom (ast/app-term 'xtab-lem-demo (ast/var-term x))]
    [(ast/forall-form x
       (ast/or-form (ast/pos-lit atom)
                    (ast/neg-lit atom)))]))

(defn tab2-rank2-witness-formula
  "Return the compact Rank-2 theorem reused by the Tab-2 certificate."
  []
  (let [x (sjas-code/code-nom 1)
        y (sjas-code/code-nom 2)]
    (ast/forall-form x
      (ast/exists-form y
        (ast/true-form)))))

(defn- level1-selfcons-formula
  "Build the Level-1 SelfCons schema with an explicit substitution argument."
  [system-code substitution-code]
  (let [x (nominal/nom (lvar 'selfcons-x))
        y (nominal/nom (lvar 'selfcons-y))
        p (nominal/nom (lvar 'selfcons-p))
        q (nominal/nom (lvar 'selfcons-q))]
    (ast/forall-form
      x
      (ast/forall-form
        y
        (ast/forall-form
          p
          (ast/forall-form
            q
            (ast/or-form
              (ast/neg-lit
                (ast/app-term 'pi-star-1-code (ast/var-term x)))
              (ast/or-form
                (ast/neg-lit
                  (ast/app-term 'neg-pair
                                (ast/var-term x)
                                (ast/var-term y)))
                (ast/or-form
                  (ast/neg-lit
                    (ast/app-term 'dsjas-subst-prf
                                  system-code
                                  substitution-code
                                  (ast/var-term x)
                                  (ast/var-term p)))
                  (ast/neg-lit
                    (ast/app-term 'dsjas-subst-prf
                                  system-code
                                  substitution-code
                                  (ast/var-term y)
                                  (ast/var-term q))))))))))))

(defn- canonical-term
  "Canonicalize bindable terms for stable formula-code generation."
  [term env]
  (case (ast/tag-of term)
    var (list 'var (get env (second term) (second term)))
    par (list 'par (get env (second term) (second term)))
    app (list* 'app (second term) (map #(canonical-term % env) (nnext term)))
    term))

(declare canonical-formula)

(defn- bind-name
  [env binding-nom]
  (let [label (symbol (str "v" (count env)))]
    [(assoc env binding-nom label) label]))

(defn- canonical-quantifier
  [tag tied env]
  (let [[env* label] (bind-name env (:binding-nom tied))]
    (list tag label (canonical-formula (:body tied) env*))))

(defn- canonical-bounded
  [tag tied env]
  (let [[env* label] (bind-name env (:binding-nom tied))
        {:keys [bound body]} (:body tied)]
    (list tag
          label
          (canonical-term bound env)
          (canonical-formula body env*))))

(defn canonical-formula
  "Return the stable binder-indexed form consumed by the SJAS code encoder."
  ([formula]
   (canonical-formula formula {}))
  ([formula env]
   (case (ast/tag-of formula)
     true '(true)
     false '(false)
     pos (list 'pos (canonical-term (second formula) env))
     neg (list 'neg (canonical-term (second formula) env))
     eq (list 'eq
              (canonical-term (second formula) env)
              (canonical-term (nth formula 2) env))
     neq (list 'neq
               (canonical-term (second formula) env)
               (canonical-term (nth formula 2) env))
     and (list 'and
               (canonical-formula (second formula) env)
               (canonical-formula (nth formula 2) env))
     or (list 'or
              (canonical-formula (second formula) env)
              (canonical-formula (nth formula 2) env))
     not (list 'not (canonical-formula (second formula) env))
     implies (list 'implies
                   (canonical-formula (second formula) env)
                   (canonical-formula (nth formula 2) env))
     forall (canonical-quantifier 'forall (second formula) env)
     once-forall (canonical-quantifier 'once-forall (second formula) env)
     exists (canonical-quantifier 'exists (second formula) env)
     bounded-forall
     (canonical-bounded 'bounded-forall (second formula) env)
     bounded-exists
     (canonical-bounded 'bounded-exists (second formula) env)
     formula)))

(def ^:private reserved-coding-context
  "Coding context for formulas whose nonlogical symbols are all reserved."
  (sjas-code/context sjas-code/reserved-coding-symbols))

(def ^:private level1-base-coding-symbols
  "Reserved symbols declared by Level-1-family systems before user additions."
  (vec (take-while #(not= 'dsjas-tab2-proof %)
                   sjas-code/reserved-coding-symbols)))

(def ^:private total-multiplication-coding-symbols
  "Level-1 symbols plus the Type-M vocabulary, excluding Tab-2's local slot."
  (vec (remove #{'dsjas-tab2-proof}
               sjas-code/reserved-coding-symbols)))

(defn formula-code-bytes
  "Encode a boundary formula with an exact declared symbol collection."
  [formula declared-symbols]
  (sjas-code/canonical-formula-code-bytes
    (sjas-code/context declared-symbols)
    (canonical-formula formula)))

(defn total-multiplication-formula-code-bytes
  "Encode a formula in the Type-M profile's finite language."
  [formula]
  (formula-code-bytes formula total-multiplication-coding-symbols))

(defn xtab-formula-code-bytes
  "Encode a formula in the Xtab profile's finite language."
  [formula]
  (formula-code-bytes formula
                      (conj level1-base-coding-symbols 'xtab-lem-demo)))

(defn reserved-formula-code-bytes
  "Encode a boundary formula that contains only reserved coding symbols."
  [formula]
  (sjas-code/canonical-formula-code-bytes
    reserved-coding-context
    (canonical-formula formula)))

(defn level1-selfcons-skeleton-code-bytes
  "Return the exact open `Gamma_1(g)` formula bytes for `system-code`."
  [system-code]
  (let [g (nominal/nom (lvar 'selfcons-g))
        skeleton (level1-selfcons-formula system-code (ast/var-term g))]
    (sjas-code/canonical-formula-code-bytes
      reserved-coding-context
      (canonical-formula skeleton {g 'v0}))))

(defn- skip-term-index
  "Return the first byte index after one canonical encoded term."
  [bytes index]
  (let [tag (nth bytes index)]
    (case tag
      21 (+ index 2)
      22 (+ index 2)
      23 (let [arity (dec (nth bytes (+ index 2)))]
           (loop [remaining arity
                  cursor (+ index 3)]
             (if (zero? remaining)
               cursor
               (recur (dec remaining)
                      (skip-term-index bytes cursor)))))
      24 (let [low (nth bytes (inc index))
               high (nth bytes (+ index 2))]
           (+ index 3 low (* sjas-code/byte-base high)))
      25 (let [low (nth bytes (inc index))
               high (nth bytes (+ index 2))]
           (+ index 3 low (* sjas-code/byte-base high)))
      (throw (ex-info "Unsupported term tag in encoded boundary source"
                      {:tag tag :index index})))))

(declare skip-formula-index)

(defn- skip-two-formulas-index
  [bytes index]
  (let [after-left (skip-formula-index bytes index)]
    (skip-formula-index bytes after-left)))

(defn- skip-formula-index
  "Return the first byte index after one canonical encoded formula."
  [bytes index]
  (let [tag (nth bytes index)]
    (case tag
      1 (inc index)
      2 (inc index)
      3 (skip-term-index bytes (inc index))
      4 (skip-term-index bytes (inc index))
      5 (let [after-left (skip-term-index bytes (inc index))]
          (skip-term-index bytes after-left))
      6 (let [after-left (skip-term-index bytes (inc index))]
          (skip-term-index bytes after-left))
      7 (skip-two-formulas-index bytes (inc index))
      8 (skip-two-formulas-index bytes (inc index))
      9 (skip-formula-index bytes (inc index))
      10 (skip-two-formulas-index bytes (inc index))
      11 (skip-formula-index bytes (+ index 2))
      12 (skip-formula-index bytes (+ index 2))
      13 (skip-formula-index bytes (+ index 2))
      14 (let [after-bound (skip-term-index bytes (+ index 2))]
           (skip-formula-index bytes after-bound))
      15 (let [after-bound (skip-term-index bytes (+ index 2))]
           (skip-formula-index bytes after-bound))
      (throw (ex-info "Unsupported formula tag in encoded boundary source"
                      {:tag tag :index index})))))

(defn system-beta-formula-byte-vectors
  "Parse the exact beta formula byte strings from an encoded SJAS system."
  [system-bytes]
  (let [bytes (vec system-bytes)]
    (when-not (and (<= 3 (count bytes))
                   (= 31 (nth bytes 0)))
      (throw (ex-info "Expected an encoded SJAS system source"
                      {:byte-count (count bytes)})))
    (let [beta-total (dec (nth bytes 2))]
      (loop [remaining beta-total
             cursor 3
             formulas []]
        (if (zero? remaining)
          formulas
          (let [after (skip-formula-index bytes cursor)]
            (recur (dec remaining)
                   after
                   (conj formulas (subvec bytes cursor after)))))))))

(defn- lower-bounded-formula
  "Lower SJAS bounded quantifiers to their proof-checker connectives."
  [formula]
  (case (ast/tag-of formula)
    bounded-forall
    (let [tied (second formula)
          binding (:binding-nom tied)
          {:keys [bound body]} (:body tied)]
      (ast/forall-form
        binding
        (ast/implies-form
          (leq (ast/var-term binding) bound)
          (lower-bounded-formula body))))

    bounded-exists
    (let [tied (second formula)
          binding (:binding-nom tied)
          {:keys [bound body]} (:body tied)]
      (ast/exists-form
        binding
        (ast/and-form
          (leq (ast/var-term binding) bound)
          (lower-bounded-formula body))))

    and (ast/and-form (lower-bounded-formula (second formula))
                      (lower-bounded-formula (nth formula 2)))
    or (ast/or-form (lower-bounded-formula (second formula))
                    (lower-bounded-formula (nth formula 2)))
    not (ast/not-form (lower-bounded-formula (second formula)))
    implies (ast/implies-form (lower-bounded-formula (second formula))
                              (lower-bounded-formula (nth formula 2)))
    forall (let [tied (second formula)]
             (ast/forall-form (:binding-nom tied)
                              (lower-bounded-formula (:body tied))))
    exists (let [tied (second formula)]
             (ast/exists-form (:binding-nom tied)
                              (lower-bounded-formula (:body tied))))
    formula))

(defn proof-side-formula
  "Return the exact NNF/once-forall shape used in proof antecedents."
  [formula]
  (letfn [(once-forall [current]
            (case (ast/tag-of current)
              and (ast/and-form (once-forall (second current))
                                (once-forall (nth current 2)))
              or (ast/or-form (once-forall (second current))
                              (once-forall (nth current 2)))
              forall (let [tied (second current)]
                       (ast/once-forall-form
                         (:binding-nom tied)
                         (once-forall (:body tied))))
              exists (let [tied (second current)]
                       (ast/exists-form
                         (:binding-nom tied)
                         (once-forall (:body tied))))
              current))]
    (once-forall
      (normalize/to-nnf
        (lower-bounded-formula formula)))))
