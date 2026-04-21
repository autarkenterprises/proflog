(ns proflog.answers
  "Answer export helpers for greenfield Proflog queries.

   The kernel remains a structural relation over explicit object-language
   variables. The generic path exports symbolic substitutions and residual
   formulas for named answer vars, including deferred procedure-call
   obligations when recursive search is left symbolic. A separate bounded
   ground-enumeration helper is kept as a non-generic materialization layer
   for operational use."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer [== run]]
            [clojure.core.logic.nominal :as nominal]
            [proflog.ast :as ast]
            [proflog.kernel :as kernel]
            [proflog.language :as language]
            [proflog.normalize :as normalize]
            [proflog.query :as query]
            [proflog.subst :as subst]))

(defn- declaration-order
  "Stable ordering for declared symbols during bounded enumeration."
  [entry]
  [(val entry) (str (key entry))])

(defn- tuples
  "Cartesian power of `xs` with width `n`."
  [xs n]
  (if (zero? n)
    (list '())
    (for [x xs
          tail (tuples xs (dec n))]
      (cons x tail))))

(defn ground-terms-up-to-depth
  "Enumerate all declared-language ground terms up to constructor depth `max-depth`.

   Depth zero contains only nullary function symbols (including constants).
   Higher depths are built from shallower terms in a stable declaration order."
  [lang max-depth]
  (when (neg? max-depth)
    (throw (ex-info "Ground-term depth must be non-negative"
                    {:max-depth max-depth})))
  (let [declared-functions (sort-by declaration-order (:functions lang))
        nullary-terms (mapv (fn [[sym _]]
                              (ast/app-term sym))
                            (filter (fn [[_ arity]]
                                      (zero? arity))
                                    declared-functions))
        positive-functions (filter (fn [[_ arity]]
                                     (pos? arity))
                                   declared-functions)]
    (loop [depth 0
           terms-up-to nullary-terms
           exact-prev nullary-terms]
      (if (= depth max-depth)
        terms-up-to
        (let [exact-prev-set (set exact-prev)
              exact-next (->> positive-functions
                              (mapcat (fn [[sym arity]]
                                        (for [args (tuples terms-up-to arity)
                                              :when (some exact-prev-set args)]
                                          (apply ast/app-term sym args))))
                              distinct
                              vec)]
          (recur (inc depth)
                 (into terms-up-to exact-next)
                 exact-next))))))

(declare free-vars-formula)

(defn- free-vars-term
  "Collect the free object-language variable noms mentioned in `term`."
  [term]
  (case (ast/tag-of term)
    var #{(second term)}
    par #{}
    app (reduce into #{} (map free-vars-term (nnext term)))
    #{}))

(defn- free-vars-formula
  "Collect the free object-language variable noms mentioned in `formula`."
  [formula]
  (case (ast/tag-of formula)
    true #{}
    false #{}
    pos (free-vars-term (second formula))
    neg (free-vars-term (second formula))
    eq (into (free-vars-term (second formula))
             (free-vars-term (nth formula 2)))
    neq (into (free-vars-term (second formula))
              (free-vars-term (nth formula 2)))
    and (into (free-vars-formula (second formula))
              (free-vars-formula (nth formula 2)))
    or (into (free-vars-formula (second formula))
             (free-vars-formula (nth formula 2)))
    not (free-vars-formula (second formula))
    implies (into (free-vars-formula (second formula))
                  (free-vars-formula (nth formula 2)))
    forall (let [tied (second formula)]
             (disj (free-vars-formula (:body tied))
                   (:binding-nom tied)))
    exists (let [tied (second formula)]
             (disj (free-vars-formula (:body tied))
                   (:binding-nom tied)))
    #{}))

(defn- validate-answer-vars
  "Check that `answer-vars` are distinct free noms of `query`."
  [query answer-vars]
  (let [answer-vars (vec answer-vars)
        query-free-vars (free-vars-formula query)]
    (doseq [binding-nom answer-vars]
      (when-not (nominal/nom? binding-nom)
        (throw (ex-info "Answer variables must be noms"
                        {:answer-vars answer-vars
                         :invalid binding-nom}))))
    (when-not (= (count answer-vars) (count (distinct answer-vars)))
      (throw (ex-info "Answer variables must be distinct"
                      {:answer-vars answer-vars})))
    (doseq [binding-nom answer-vars]
      (when-not (contains? query-free-vars binding-nom)
        (throw (ex-info "Answer variable is not free in the query"
                        {:answer-var binding-nom
                         :query query}))))
    answer-vars))

(defn- lookup-clause
  "Pure clause lookup in a compiled program."
  [program relation]
  (some (fn [clause]
          (when (= relation (:relation clause))
            clause))
        (:clause-list program)))

(defn- bind-args
  "Create a pure environment mapping clause params to actual call args."
  [params args]
  (mapv vector params args))

(declare unfold-call-obligations)

(defn- unfold-call-literal
  "Expand one procedure call literal up to `call-depth` levels."
  [program lit call-depth]
  (let [atom (second lit)
        relation (second atom)
        args (nnext atom)]
    (if (pos? call-depth)
      (if-let [{:keys [params body negated-body]} (lookup-clause program relation)]
        (unfold-call-obligations
          program
          (subst/subst-formula
            (case (ast/tag-of lit)
              pos body
              neg negated-body)
            (bind-args params args))
          (dec call-depth))
        lit)
      lit)))

(defn- unfold-call-obligations
  "Eagerly unfold procedure calls in `formula` before kernel answer export."
  [program formula call-depth]
  (case (ast/tag-of formula)
    pos (unfold-call-literal program formula call-depth)
    neg (unfold-call-literal program formula call-depth)
    and (ast/and-form (unfold-call-obligations program (second formula) call-depth)
                      (unfold-call-obligations program (nth formula 2) call-depth))
    or (ast/or-form (unfold-call-obligations program (second formula) call-depth)
                    (unfold-call-obligations program (nth formula 2) call-depth))
    forall (let [tied (second formula)]
             (ast/forall-form (:binding-nom tied)
                              (unfold-call-obligations program (:body tied) call-depth)))
    exists (let [tied (second formula)]
             (ast/exists-form (:binding-nom tied)
                              (unfold-call-obligations program (:body tied) call-depth)))
    formula))

(defn binding-term
  "Return the answer term bound to `binding-nom`, or nil when absent."
  [answer binding-nom]
  (some (fn [[candidate-nom term]]
          (when (= candidate-nom binding-nom)
            term))
        (:bindings answer)))

(defn- walk-term
  "Purely walk one term through the explicit substitution `sigma`."
  [term sigma]
  (let [tag (ast/tag-of term)]
    (case tag
      var (if-let [value (subst/lookup-binding sigma (second term))]
            (recur value sigma)
            term)
      par (if-let [value (subst/lookup-binding sigma (second term))]
            (recur value sigma)
            term)
      app (apply ast/app-term
                 (second term)
                 (map #(walk-term % sigma) (nnext term)))
      term)))

(defn- walk-formula
  "Purely walk the term leaves of a residual formula through `sigma`."
  [formula sigma]
  (case (ast/tag-of formula)
    pos (ast/pos-lit (walk-term (second formula) sigma))
    neg (ast/neg-lit (walk-term (second formula) sigma))
    eq (ast/eq-lit (walk-term (second formula) sigma)
                   (walk-term (nth formula 2) sigma))
    neq (ast/neq-lit (walk-term (second formula) sigma)
                     (walk-term (nth formula 2) sigma))
    formula))

(defn- rename-term
  "Rename exported object-language vars according to `renaming`."
  [term renaming]
  (let [tag (ast/tag-of term)]
    (case tag
      var (ast/var-term (get renaming (second term) (second term)))
      par term
      app (apply ast/app-term
                 (second term)
                 (map #(rename-term % renaming) (nnext term)))
      term)))

(defn- rename-formula
  "Rename object-language vars inside an exported residual formula."
  [formula renaming]
  (case (ast/tag-of formula)
    pos (ast/pos-lit (rename-term (second formula) renaming))
    neg (ast/neg-lit (rename-term (second formula) renaming))
    eq (ast/eq-lit (rename-term (second formula) renaming)
                   (rename-term (nth formula 2) renaming))
    neq (ast/neq-lit (rename-term (second formula) renaming)
                     (rename-term (nth formula 2) renaming))
    formula))

(defn- admissible-term?
  "True when `term` is exportable under the declared object language."
  [lang term]
  (try
    (language/validate-term lang term)
    true
    (catch Exception _
      false)))

(defn- admissible-formula?
  "True when `formula` stays inside the declared object language."
  [lang formula]
  (try
    (language/validate-formula lang formula)
    true
    (catch Exception _
      false)))

(defn- export-answer-record
  "Project one kernel proof state into an answer record or nil if inadmissible."
  [lang answer-vars reified-answer-vars sigma neqs residual-formulas proof]
  (let [renaming (zipmap reified-answer-vars answer-vars)
        bindings (mapv (fn [binding-nom reified-binding-nom]
                         [binding-nom
                          (rename-term
                            (walk-term (ast/var-term reified-binding-nom) sigma)
                            renaming)])
                       answer-vars
                       reified-answer-vars)
        residuals (vec
                    (concat
                      (map (fn [[left right]]
                             (ast/neq-lit
                               (rename-term (walk-term left sigma) renaming)
                               (rename-term (walk-term right sigma) renaming)))
                           neqs)
                      (map (fn [formula]
                             (rename-formula
                               (walk-formula formula sigma)
                               renaming))
                           residual-formulas)))]
    (when (and (every? (fn [[_ term]]
                         (admissible-term? lang term))
                       bindings)
               (every? (fn [formula]
                         (admissible-formula? lang formula))
                       residuals))
      {:bindings bindings
       :residuals residuals
       :proofs [proof]})))

(defn- merge-answer-records
  "Merge records with the same bindings and residuals, collecting proofs."
  [records]
  (vals
    (reduce (fn [merged {:keys [bindings residuals proofs] :as record}]
              (let [key [bindings residuals]]
                (if-let [existing (get merged key)]
                  (assoc merged key (update existing :proofs into proofs))
                  (assoc merged key record))))
            {}
            records)))

(defn formula-answers
  "Export symbolic answers for a closed tableau over `formula`.

   This is the generic answer path: requested answer vars may remain partially
   instantiated, and residual formulas are preserved in the answer records."
  ([lang formula answer-vars]
   (formula-answers lang formula answer-vars {}))
  ([lang formula answer-vars {:keys [fuel proof-limit]
                              :or {proof-limit 10}}]
   (let [checked-formula (language/validate-formula lang formula)
         checked-answer-vars (validate-answer-vars checked-formula answer-vars)]
     (->> (run proof-limit [answer-vars-out sigma-out neqs-out residuals-out proof]
            (== answer-vars-out checked-answer-vars)
            (kernel/prove-answero
              checked-formula
              '()
              '()
              '()
              checked-answer-vars
              sigma-out
              neqs-out
              residuals-out
              fuel
              proof))
          (map (fn [[answer-vars-out sigma-out neqs-out residuals-out proof]]
                 (export-answer-record
                   lang
                   checked-answer-vars
                   answer-vars-out
                   sigma-out
                   neqs-out
                   residuals-out
                   proof)))
          (keep identity)
          merge-answer-records
          vec))))

(defn query-answers
  "Export symbolic answers for `query` relative to `program`.

   This is the generic solution for reverse and partial-mode query answering.
   Returned records may contain non-ground bindings, residual disequalities,
   and residual procedure-call obligations, but they never export internal
   `par` terms. `:call-depth` controls how many call layers are eagerly
   unfolded before remaining calls are exported as residual obligations."
  ([program query answer-vars]
   (query-answers program query answer-vars {}))
  ([program query answer-vars {:keys [call-depth fuel proof-limit]
                               :or {proof-limit 10
                                    call-depth 1}}]
   (let [checked-query (language/validate-query (:language program) query)
         expanded-query (unfold-call-obligations
                          program
                          (normalize/negate-formula checked-query)
                          call-depth)
         checked-answer-vars (validate-answer-vars checked-query answer-vars)]
     (->> (run proof-limit [answer-vars-out sigma-out neqs-out residuals-out proof]
            (== answer-vars-out checked-answer-vars)
            (kernel/prove-program-answero
              expanded-query
              '()
              '()
              '()
              checked-answer-vars
              program
              sigma-out
              neqs-out
              residuals-out
              fuel
              0
              proof))
          (map (fn [[answer-vars-out sigma-out neqs-out residuals-out proof]]
                 (export-answer-record
                   (:language program)
                   checked-answer-vars
                   answer-vars-out
                   sigma-out
                   neqs-out
                   residuals-out
                   proof)))
          (keep identity)
          merge-answer-records
          vec))))

(defn query-ground-answers
  "Enumerate bounded ground answers for `query`.

   `answer-vars` must be a sequence of free noms occurring in `query`. The
   caller controls completeness via `:max-depth` and `:fuel`. This helper is
   explicitly non-generic: it materializes answers by bounded Herbrand
   enumeration above the semantic kernel. Use `query-answers` for the generic
   symbolic API. Results are returned in declaration/enumeration order as maps
   containing:

   - `:bindings` ordered `[nom term]` pairs for the requested answer vars
   - `:query` the instantiated ground query that succeeded
   - `:proofs` proof terms witnessing success for that ground instance"
  ([program query answer-vars]
   (query-ground-answers program query answer-vars {}))
  ([program query answer-vars {:keys [failure-timeout-ms fuel limit max-depth proof-limit]
                               :or {max-depth 3
                                    failure-timeout-ms 250
                                    proof-limit 1}}]
   (let [checked-query (language/validate-query (:language program) query)
         checked-answer-vars (validate-answer-vars checked-query answer-vars)
         ground-terms (ground-terms-up-to-depth (:language program) max-depth)]
     (loop [assignments (seq (tuples ground-terms (count checked-answer-vars)))
            answers []]
       (cond
         (nil? assignments)
         answers

         (and limit (>= (count answers) limit))
         answers

         :else
         (let [terms (first assignments)
               bindings (mapv vector checked-answer-vars terms)
               instantiated-query (subst/subst-formula checked-query bindings)
               success-proofs (kernel/prove-program
                                program
                                (normalize/negate-formula instantiated-query)
                                proof-limit
                                fuel)]
           (recur (next assignments)
                  (if (seq success-proofs)
                    (let [failure-proofs
                          ;; Keep the failure-side guard operationally bounded
                          ;; so answer export stays usable on recursive
                          ;; programs such as Nim.
                          (query/query-fails-within
                            program
                            instantiated-query
                            proof-limit
                            failure-timeout-ms)]
                      (if (empty? failure-proofs)
                        (conj answers {:bindings bindings
                                       :query instantiated-query
                                       :proofs success-proofs})
                        answers))
                    answers))))))))
