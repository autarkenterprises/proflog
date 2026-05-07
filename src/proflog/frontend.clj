(ns proflog.frontend
  "Thin source-facing Proflog frontend.

   This namespace accepts a Clojure-readable prefix surface and emits the
   existing backend forms from `proflog.ast` and `proflog.language`. It does not
   evaluate programs. `language` builds a reusable language declaration,
   `proflog` translates visible source clauses into compiled programs, and `q`
   translates visible closed queries into kernel-facing formulas.
   `answer-query` binds visible answer variables and returns the query formula
   plus the answer-variable vector expected by `proflog.answers`."
  (:require [proflog.ast :as ast]
            [proflog.language :as backend-language]))

(defn- malformed!
  [message data]
  (throw (ex-info message data)))

(defn- arity-entry
  [form]
  (when-not (and (seq? form)
                 (symbol? (first form))
                 (= 2 (count form))
                 (integer? (second form))
                 (<= 0 (second form)))
    (malformed! "Expected a declaration entry like (symbol arity)"
                {:form form}))
  [(first form) (second form)])

(defn- parse-language-section
  [section]
  (when-not (seq? section)
    (malformed! "Malformed language section" {:section section}))
  (let [head (first section)
        entries (rest section)]
    (case head
      constants {:constants (vec entries)}
      functions {:functions (into {} (map arity-entry entries))}
      relations {:relations (into {} (map arity-entry entries))}
      (malformed! "Unknown language section"
                  {:section section
                   :known-sections '(constants functions relations)}))))

(defn- parse-language-declaration
  [sections]
  (reduce (fn [declaration section]
            (merge-with (fn [left right]
                          (cond
                            (vector? left) (into left right)
                            (map? left) (merge left right)
                            :else right))
                        declaration
                        (parse-language-section section)))
          {}
          sections))

(defmacro language
  "Build a reusable frontend language declaration.

   Example:
   (language
     (constants zero)
     (functions (s 1))
     (relations (p 1)))"
  [& sections]
  `(backend-language/language
     ~(list 'quote (parse-language-declaration sections))))

(defn- parse-head
  [head]
  (when-not (and (seq? head)
                 (symbol? (first head))
                 (every? symbol? (rest head)))
    (malformed! "Expected a predicate head like (relation x y)"
                {:head head}))
  {:relation (first head)
   :params (vec (rest head))})

(defn- parse-clause-form
  [form]
  (when-not (and (seq? form)
                 (= 3 (count form)))
    (malformed! "Expected a frontend clause like (|- head body) or (:= head body)"
                {:form form}))
  (let [op (first form)
        {:keys [relation params]} (parse-head (second form))]
    (cond
      (= := op) {:kind :definition
                 :relation relation
                 :params params
                 :body (nth form 2)}
      (= '|- op) {:kind :relation
                  :relation relation
                  :params params
                  :body (nth form 2)}
      :else (malformed! "Unknown frontend clause operator"
                        {:operator op
                         :form form
                         :known-operators '(:= |-)}))))

(defn- register-nom!
  [noms sym]
  (swap! noms conj sym)
  sym)

(declare emit-formula)

(defn- emit-term
  [term env helpers noms]
  (cond
    (symbol? term)
    (if-let [bound (get env term)]
      bound
      `(ast/app-term '~term))

    (seq? term)
    (let [head (first term)]
      (when-not (symbol? head)
        (malformed! "Expected a function symbol in term position"
                    {:term term}))
      `(ast/app-term '~head ~@(map #(emit-term % env helpers noms)
                                   (rest term))))

    :else
    (malformed! "Unsupported frontend term" {:term term})))

(defn- emit-nary-formula
  [constructor empty-form args env helpers noms stack]
  (case (count args)
    0 empty-form
    1 (emit-formula (first args) env helpers noms stack)
    (reduce (fn [left right]
              `(~constructor ~left ~right))
            (map #(emit-formula % env helpers noms stack) args))))

(defn- emit-quantifier
  [constructor bindings body env helpers noms stack]
  (when-not (and (vector? bindings)
                 (seq bindings)
                 (every? symbol? bindings))
    (malformed! "Expected a nonempty vector of quantifier bindings"
                {:bindings bindings
                 :body body}))
  (let [pairs (mapv (fn [binding]
                      [binding (register-nom! noms (gensym (str (name binding) "__")))])
                    bindings)
        scoped-env (reduce (fn [acc [source generated]]
                             (assoc acc source `(ast/var-term ~generated)))
                           env
                           pairs)
        body-code (emit-formula body scoped-env helpers noms stack)]
    (reduce (fn [inner [_ generated]]
              `(~constructor ~generated ~inner))
            body-code
            (reverse pairs))))

(defn- emit-helper-call
  [helper form env helpers noms stack]
  (let [{:keys [relation params body]} helper
        args (rest form)]
    (when (contains? stack relation)
      (malformed! "Recursive frontend helper definitions are not supported"
                  {:helper relation
                   :call form
                   :stack stack}))
    (when-not (= (count params) (count args))
      (malformed! "Arity mismatch for frontend helper call"
                  {:helper relation
                   :expected-arity (count params)
                   :actual-arity (count args)
                   :call form}))
    (let [helper-env (zipmap params
                             (map #(emit-term % env helpers noms) args))]
      (emit-formula body helper-env helpers noms (conj stack relation)))))

(defn- emit-formula
  [formula env helpers noms stack]
  (cond
    (true? formula) `(ast/true-form)
    (false? formula) `(ast/false-form)

    (seq? formula)
    (let [op (first formula)
          args (rest formula)]
      (cond
        (= '= op)
        (do
          (when-not (= 2 (count args))
            (malformed! "Expected (= left right)" {:formula formula}))
          `(ast/eq-lit ~(emit-term (first args) env helpers noms)
                       ~(emit-term (second args) env helpers noms)))

        (= '!= op)
        (do
          (when-not (= 2 (count args))
            (malformed! "Expected (!= left right)" {:formula formula}))
          `(ast/neq-lit ~(emit-term (first args) env helpers noms)
                        ~(emit-term (second args) env helpers noms)))

        (= 'and op)
        (emit-nary-formula `ast/and-form `(ast/true-form) args env helpers noms stack)

        (= 'or op)
        (emit-nary-formula `ast/or-form `(ast/false-form) args env helpers noms stack)

        (= 'not op)
        (do
          (when-not (= 1 (count args))
            (malformed! "Expected (not formula)" {:formula formula}))
          `(ast/not-form ~(emit-formula (first args) env helpers noms stack)))

        (= 'implies op)
        (do
          (when-not (= 2 (count args))
            (malformed! "Expected (implies antecedent consequent)"
                        {:formula formula}))
          `(ast/implies-form ~(emit-formula (first args) env helpers noms stack)
                             ~(emit-formula (second args) env helpers noms stack)))

        (= 'forall op)
        (do
          (when-not (= 2 (count args))
            (malformed! "Expected (forall [x] body)" {:formula formula}))
          (emit-quantifier `ast/forall-form (first args) (second args)
                           env helpers noms stack))

        (= 'exists op)
        (do
          (when-not (= 2 (count args))
            (malformed! "Expected (exists [x] body)" {:formula formula}))
          (emit-quantifier `ast/exists-form (first args) (second args)
                           env helpers noms stack))

        (contains? helpers op)
        (emit-helper-call (get helpers op) formula env helpers noms stack)

        (symbol? op)
        `(ast/pos-lit
           (ast/app-term '~op ~@(map #(emit-term % env helpers noms) args)))

        :else
        (malformed! "Unsupported frontend formula" {:formula formula})))

    :else
    (malformed! "Unsupported frontend formula" {:formula formula})))

(defn- helper-map
  [parsed-forms]
  (reduce (fn [helpers {:keys [kind relation params] :as parsed}]
            (if (= :definition kind)
              (do
                (when (contains? helpers relation)
                  (malformed! "Duplicate frontend helper definition"
                              {:helper relation}))
                (assoc helpers relation parsed))
              helpers))
          {}
          parsed-forms))

(defn- emit-relation-clause
  [{:keys [relation params body]} helpers noms]
  (doseq [param params]
    (register-nom! noms param))
  (let [env (into {} (map (fn [param]
                            [param `(ast/var-term ~param)])
                          params))]
    `(ast/clause '~relation
                 [~@params]
                 ~(emit-formula body env helpers noms #{}))))

(defmacro proflog
  "Compile visible prefix Proflog source clauses against a reusable language.

   `(:= head body)` introduces an inline source-level helper.
   `(|- head body)` introduces a real Proflog relation clause."
  [frontend-language & source-forms]
  (let [parsed (mapv parse-clause-form source-forms)
        helpers (helper-map parsed)
        relation-forms (filterv #(= :relation (:kind %)) parsed)
        noms (atom [])
        clauses (mapv #(emit-relation-clause % helpers noms) relation-forms)
        unique-noms (vec (distinct @noms))]
    (when-not (seq relation-forms)
      (malformed! "A Proflog program must contain at least one relation clause"
                  {:source-forms source-forms}))
    `(ast/nom ~@unique-noms
       (backend-language/compile-program
         ~frontend-language
         [~@clauses]))))

(defmacro q
  "Translate one visible frontend query/formula into a backend formula."
  [formula]
  (let [noms (atom [])
        code (emit-formula formula {} {} noms #{})
        unique-noms (vec (distinct @noms))]
    `(ast/nom ~@unique-noms
       ~code)))

(defn- validate-answer-query-bindings
  [bindings]
  (when-not (and (vector? bindings)
                 (seq bindings)
                 (every? symbol? bindings))
    (malformed! "Expected answer-query bindings like [x y]"
                {:bindings bindings}))
  (when-not (= (count bindings) (count (distinct bindings)))
    (malformed! "Duplicate frontend answer-query bindings"
                {:bindings bindings}))
  bindings)

(defmacro answer-query
  "Bind visible answer variables for a frontend query.

   Returns a map compatible with the public answer APIs:
   {:query formula :answer-vars [noms...]}."
  [bindings formula]
  (let [bindings (validate-answer-query-bindings bindings)
        noms (atom (vec bindings))
        env (into {} (map (fn [binding]
                            [binding `(ast/var-term ~binding)])
                          bindings))
        code (emit-formula formula env {} noms #{})
        unique-noms (vec (distinct @noms))]
    `(ast/nom ~@unique-noms
       {:query ~code
        :answer-vars [~@bindings]})))
