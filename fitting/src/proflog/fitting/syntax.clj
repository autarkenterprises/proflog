(ns proflog.fitting.syntax
  "Section 2 of M. Fitting, \"Tableaus for Logic Programming\" (1993):
  the language L, its terms and formulas, programs (Definition 2.1), and
  queries.

  This namespace is plain Clojure. It validates input and builds the finite
  tables that the tableau relation (`proflog.fitting.tableau`) consults. It
  decides nothing about provability.

  Notation, chosen to read as the paper does:

  | Paper                     | Here                                    |
  |---------------------------|-----------------------------------------|
  | constant 0, c             | `0`, `c` (declared in L)                |
  | f(t1,...,tn)              | `(f t1 ... tn)`                         |
  | variable x                | `x` (a symbol that is not a symbol of L)|
  | parameter (§4)            | `(par)`, `(par 1)`, `(par 1 1)`, ...    |
  | R(t1,...,tn), t = u       | `(R t1 ... tn)`, `(= t u)`              |
  | ¬X, X ∧ Y, X ∨ Y, X ⊃ Y   | `(not X)`, `(and X Y)`, `(or X Y)`, `(implies X Y)` |
  | (∀x)Φ, (∃x)Φ              | `(forall x Φ)`, `(exists x Φ)`          |
  | true, false               | `true`, `false`                         |
  | R(x1,...,xn) ← ϕ          | `(<- (R x1 ... xn) ϕ)`                  |

  Parameters are numbered in unary, `(par)` being the first, because the
  tableau relation counts them with `==` alone (ADR-0149, Decision §2).

  Arities are also unary lists, `(1 1)` for a two-place symbol, for the same
  reason."
  (:require [clojure.set :as set]
            [clojure.walk :as walk]))

;;; ---------------------------------------------------------------------
;;; Reserved words

(def reserved
  "Words of the notation itself. None may be a symbol of L or a variable."
  '#{and or not implies forall exists par <- =})

(def connectives '#{and or implies})
(def quantifiers '#{forall exists})

(defn- unary
  "The unary list for a natural number: 2 => (1 1)."
  [n]
  (apply list (repeat n 1)))

(defn- fail! [message data]
  (throw (ex-info message data)))

;;; ---------------------------------------------------------------------
;;; The language L (§2)

(defn- check-symbol! [kind s]
  (when-not (or (symbol? s) (number? s) (keyword? s))
    (fail! (str kind " must be a symbol, number or keyword") {:symbol s}))
  (when (contains? reserved s)
    (fail! (str s " is a reserved word and cannot be a " kind) {:symbol s})))

(defn- distinct-pairs
  "All ordered pairs [a b] of distinct members of xs."
  [xs]
  (for [a xs b xs :when (not= a b)] [a b]))

(defn language
  "A language L, given by its constant, function and relation symbols (§2).
  Equality is always among the relation symbols.

  `spec` is `{:constants [c ...] :functions {f n ...} :relations {R n ...}}`.

  The result holds the symbols as lists that core.logic relations can search:

  - `:constants`            (c ...)
  - `:functions`            ([f (1 ... 1)] ...), function symbols of arity >= 1
  - `:relations`            ([R (1 ... 1)] ...), including [= (1 1)]
  - `:distinct-constants`   ([c d] ...), every ordered pair with c ≠ d
  - `:distinct-functions`   ([f g] ...), every ordered pair with f ≠ g
  - `:symbols`              every symbol a ground literal of Lpar can
                            contain: those of L, and `not`, `par` and `1`
  - `:distinct-symbols`     ([a b] ...), every ordered pair of different
                            members of `:symbols`

  The constant and function tables are what the Free Closure Rule (§5)
  means by \"distinct\" symbols of L. The symbol tables let the tableau
  relation tell two ground literals apart without `!=`."
  [{:keys [constants functions relations]}]
  (let [constants (vec (distinct constants))
        symbols (distinct (concat constants (sort-by str (keys functions))
                                  ['=] (sort-by str (keys relations)) '[not par 1]))]
    (doseq [c constants] (check-symbol! "constant" c))
    (doseq [[f n] functions]
      (check-symbol! "function symbol" f)
      (when-not (and (integer? n) (pos? n))
        (fail! "a function symbol needs a positive arity; constants are declared as constants"
               {:symbol f :arity n})))
    (doseq [[r n] relations]
      (check-symbol! "relation symbol" r)
      (when-not (and (integer? n) (not (neg? n)))
        (fail! "a relation symbol needs a natural-number arity" {:symbol r :arity n})))
    (let [all (concat constants (keys functions) (keys relations))]
      (when-not (apply distinct? (cons ::sentinel all))
        (fail! "the symbols of L must be distinct" {:symbols all})))
    {:constants          (apply list constants)
     :functions          (apply list (for [[f n] (sort-by str functions)] [f (unary n)]))
     :relations          (apply list (cons ['= (unary 2)]
                                           (for [[r n] (sort-by str relations)] [r (unary n)])))
     :distinct-constants (apply list (distinct-pairs constants))
     :distinct-functions (apply list (distinct-pairs (sort-by str (keys functions))))
     :symbols            (apply list symbols)
     :distinct-symbols   (apply list (distinct-pairs symbols))}))

(defn- arity-of [table s]
  (some (fn [[sym arity]] (when (= sym s) (count arity))) table))

(defn constant? [L s] (boolean (some #{s} (:constants L))))
(defn function-arity [L f] (arity-of (:functions L) f))
(defn relation-arity [L r] (arity-of (:relations L) r))

(defn symbol-of-L?
  "Is s a constant, function or relation symbol of L?"
  [L s]
  (boolean (or (constant? L s) (function-arity L s) (relation-arity L s))))

(defn variable?
  "A variable is a symbol that is neither reserved nor a symbol of L."
  [L s]
  (and (symbol? s)
       (not (contains? reserved s))
       (not (symbol-of-L? L s))))

;;; ---------------------------------------------------------------------
;;; Terms and formulas

(defn parameter?
  "A parameter of Lpar (§4): (par), (par 1), (par 1 1), ..."
  [t]
  (and (seq? t) (= 'par (first t)) (every? #{1} (rest t))))

(defn term?
  "Is t a term of L? Free variables are allowed; parameters are not, since
  they belong to Lpar, not to L."
  [L t]
  (cond
    (constant? L t) true
    (variable? L t) true
    (and (seq? t) (seq t))
    (let [[f & args] t]
      (and (= (function-arity L f) (count args))
           (every? #(term? L %) args)))
    :else false))

(defn atom?
  "Is A an atomic formula R(t1,...,tn) of L (equality included)?"
  [L A]
  (and (seq? A)
       (seq A)
       (= (relation-arity L (first A)) (count (rest A)))
       (every? #(term? L %) (rest A))))

(defn formula?
  "Is X a formula of L, in the notation described in the namespace doc?"
  [L X]
  (cond
    (true? X) true
    (false? X) true
    (not (seq? X)) false
    :else
    (let [[op & args] X]
      (cond
        (= 'not op) (and (= 1 (count args)) (formula? L (first args)))
        (contains? connectives op) (and (= 2 (count args)) (every? #(formula? L %) args))
        (contains? quantifiers op) (and (= 2 (count args))
                                        (variable? L (first args))
                                        (formula? L (second args)))
        :else (atom? L X)))))

(defn- term-variables [L t]
  (cond
    (variable? L t) #{t}
    (seq? t) (apply set/union #{} (map #(term-variables L %) (rest t)))
    :else #{}))

(defn free-variables
  "The free variables of a formula X of L. With L, variables are the symbols
  that are not symbols of L. Without L, every symbol in a term position counts
  as a variable, constants included, so `(free-variables '(p a))` is `#{a}`.
  Use the one-argument form only when X has no symbolic constants."
  ([X] (free-variables nil X))
  ([L X]
   (let [term-vars (fn [t] (if L
                             (term-variables L t)
                             (set (filter #(and (symbol? %) (not (contains? reserved %)))
                                          (tree-seq seq? rest t)))))]
     (cond
       (or (true? X) (false? X)) #{}
       :else
       (let [[op & args] X]
         (cond
           (= 'not op) (free-variables L (first args))
           (contains? connectives op) (apply set/union (map #(free-variables L %) args))
           (contains? quantifiers op) (disj (free-variables L (second args)) (first args))
           :else (apply set/union #{} (map term-vars args))))))))

(defn- formula-leaves
  "Every variable name and constant written in a formula: the leaves that
  substitution may meet."
  [X]
  (cond
    (or (true? X) (false? X)) #{}
    (and (seq? X) (seq X))
    (let [[op & args] X]
      (cond
        (= 'not op) (formula-leaves (first args))
        (contains? connectives op) (apply set/union (map formula-leaves args))
        (contains? quantifiers op) (conj (formula-leaves (second args)) (first args))
        :else (set (remove seq? (mapcat #(tree-seq seq? rest %) args)))))
    :else #{}))

(defn sentence?
  "A sentence of L is a formula with no free variables (§2)."
  [L X]
  (and (formula? L X) (empty? (free-variables L X))))

(defn ground-term-of-L? [L t]
  (and (term? L t) (empty? (term-variables L t))))

(defn query?
  "\"A query is any ground literal of L\" (§2): a ground atom of L, or the
  negation of one."
  [L A]
  (let [atom (if (and (seq? A) (= 'not (first A)) (= 2 (count A))) (second A) A)]
    (and (atom? L atom) (every? #(ground-term-of-L? L %) (rest atom)))))

;;; ---------------------------------------------------------------------
;;; Programs (Definition 2.1)

(defn- distinct-names
  "The table of distinct (variable, leaf) pairs. Substitution of a closed
  term for a variable x (§2) only ever asks whether a leaf of a formula is x,
  and every name it can meet is a variable or a constant written in the
  program, in a root sentence, or in L. No rule creates a name, because every
  term a rule inserts is closed (ADR-0149, Decision §2)."
  [variables leaves]
  (apply list (for [x (sort-by str variables)
                    s (sort-by str leaves)
                    :when (not= x s)]
                [x s])))

(defn- with-names
  "Recompute the names table of P for the given extra formulas."
  [P extra-formulas]
  (let [L (:language P)
        formulas (concat (map #(nth % 2) (:clauses P))
                         (map #(apply list (first %) (second %)) (:clauses P))
                         extra-formulas)
        leaves (into (set (:constants L)) (mapcat formula-leaves formulas))
        variables (set (filter #(variable? L %) leaves))]
    (assoc P
           :sentences (vec extra-formulas)
           :distinct-names (distinct-names variables leaves))))

(defn- check-clause! [L clause]
  (when-not (and (seq? clause) (= '<- (first clause)) (= 3 (count clause)))
    (fail! "a clause is written (<- (R x1 ... xn) ϕ)" {:clause clause}))
  (let [[_ head body] clause]
    (when-not (and (seq? head) (seq head))
      (fail! "a clause head is an atomic formula R(x1,...,xn)" {:clause clause}))
    (let [[r & xs] head]
      (when (= '= r)
        (fail! "a program has no clause for equality" {:clause clause}))
      (when-not (= (relation-arity L r) (count xs))
        (fail! "the head must be a relation symbol of L with its arity" {:clause clause}))
      (when-not (every? #(variable? L %) xs)
        (fail! "the head's arguments must be variables" {:clause clause}))
      (when-not (apply distinct? (cons ::sentinel xs))
        (fail! "the head's variables must be distinct" {:clause clause}))
      (when-not (formula? L body)
        (fail! "the body must be a formula of L" {:clause clause}))
      (when-not (set/subset? (free-variables L body) (set xs))
        (fail! "the body's free variables must be among the head's variables"
               {:clause clause :free (free-variables L body)}))
      [r (apply list xs) body])))

(defn program
  "An L-program (Definition 2.1): a finite set of L-clauses
  `(<- (R x1 ... xn) ϕ)`, at most one for each relation symbol of L
  (except equality).

  Returns `{:language L, :clauses ([R (x1 ... xn) ϕ] ...), :distinct-names ...}`."
  [L clauses]
  (let [checked (mapv #(check-clause! L %) clauses)
        relations (map first checked)]
    (when-not (apply distinct? (cons ::sentinel relations))
      (fail! "a program has at most one clause for each relation symbol"
             {:relations relations}))
    (with-names {:language L :clauses (apply list checked)} [])))

(defn with-sentence
  "P, with its names table extended to cover the sentence X, so that a
  tableau may be started for X."
  [P X]
  (when-not (sentence? (:language P) X)
    (fail! "a tableau root must be a sentence of L" {:sentence X}))
  (if (some #{X} (:sentences P))
    P
    (with-names P (conj (:sentences P) X))))

;;; ---------------------------------------------------------------------
;;; Printing

(defn pretty
  "Print-ready form of a formula or certificate. The rules build formulas
  with vectors, which unify with, and are `=` to, the lists of the notation;
  this turns them back into lists. Certificate nodes, which are vectors that
  start with a keyword, stay vectors."
  [x]
  (walk/postwalk
   (fn [y]
     (cond
       (and (vector? y) (keyword? (first y))) y
       (vector? y) (apply list y)
       (and (seq? y) (not (list? y))) (apply list y)
       :else y))
   x))
