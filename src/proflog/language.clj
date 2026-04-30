(ns proflog.language
  "Language declarations, validation, and surface-to-core compilation."
  (:require [clojure.set :as set]
            [clojure.core.logic :refer [lvar]]
            [clojure.core.logic.nominal :as nominal]
            [proflog.ast :as ast]
            [proflog.normalize :as normalize]
            [proflog.subst :as subst]))

(defn fresh-nom
  "Create a fresh nom for compile-time alpha-renaming.

   These noms are deliberately fresh with respect to all incoming clauses, so a
   compiled single-clause body cannot accidentally capture variables from one of
   the original surface clauses."
  [label]
  (nominal/nom (lvar label)))

(defn normalize-declaration-map
  "Turn constants into explicit zero-arity functions."
  [{:keys [constants functions relations] :as declaration}]
  (let [constants (vec (or constants []))
        functions (or functions {})
        relations (or relations {})
        constant-overlap (set/intersection (set constants) (set (keys functions)))
        relation-overlap (set/intersection (set (concat constants (keys functions)))
                                          (set (keys relations)))]
    (when (seq constant-overlap)
      (throw (ex-info "Constant symbols may not also be declared as functions"
                      {:overlap constant-overlap
                       :declaration declaration})))
    (when (seq relation-overlap)
      (throw (ex-info "Term symbols may not also be declared as relation symbols"
                      {:overlap relation-overlap
                       :declaration declaration})))
    {:constants (set constants)
     :functions (merge (into {} (map (fn [sym] [sym 0]) constants))
                       functions)
     :relations relations}))

(defn language
  "Construct a normalized language declaration."
  [declaration]
  (let [{:keys [functions relations] :as normalized}
        (normalize-declaration-map declaration)]
    (doseq [[sym arity] functions]
      (when-not (and (symbol? sym) (integer? arity) (<= 0 arity))
        (throw (ex-info "Invalid function declaration"
                        {:symbol sym :arity arity}))))
    (doseq [[sym arity] relations]
      (when-not (and (symbol? sym) (integer? arity) (<= 0 arity))
        (throw (ex-info "Invalid relation declaration"
                        {:symbol sym :arity arity}))))
    normalized))

(defn- declared-function-arity [lang sym]
  (get-in lang [:functions sym]))

(defn- declared-relation-arity [lang sym]
  (get-in lang [:relations sym]))

(declare validate-formula)

(defn validate-term
  "Validate a term against the declared object language.

   Surface terms are not allowed to mention `par`; those are internal proof-time
   constants and must never appear in user-authored programs or queries."
  [lang term]
  (let [tag (ast/tag-of term)]
    (case tag
      var term
      par (throw (ex-info "Internal parameter terms are not admissible in surface programs"
                          {:term term}))
      app (let [sym (second term)
                args (nnext term)
                expected-arity (declared-function-arity lang sym)
                actual-arity (count args)]
            (when (nil? expected-arity)
              (throw (ex-info (str "Undeclared function symbol: " sym)
                              {:term term :symbol sym})))
            (when (not= expected-arity actual-arity)
              (throw (ex-info (str "Arity mismatch for function symbol " sym)
                              {:term term
                               :symbol sym
                               :expected-arity expected-arity
                               :actual-arity actual-arity})))
            (doseq [arg args]
              (validate-term lang arg))
            term)
      (throw (ex-info "Malformed term" {:term term})))))

(defn validate-atom
  "Validate an atomic application against the declared relation signature."
  [lang atom]
  (let [tag (ast/tag-of atom)]
    (when-not (= 'app tag)
      (throw (ex-info "Malformed atom" {:atom atom})))
    (let [sym (second atom)
          args (nnext atom)
          expected-arity (declared-relation-arity lang sym)
          actual-arity (count args)]
      (when (nil? expected-arity)
        (throw (ex-info (str "Undeclared relation symbol: " sym)
                        {:atom atom :symbol sym})))
      (when (not= expected-arity actual-arity)
        (throw (ex-info (str "Arity mismatch for relation symbol " sym)
                        {:atom atom
                         :symbol sym
                         :expected-arity expected-arity
                         :actual-arity actual-arity})))
      (doseq [arg args]
        (validate-term lang arg))
      atom)))

(defn validate-formula
  "Validate a surface or core formula against the declared language."
  [lang formula]
  (let [tag (ast/tag-of formula)]
    (case tag
      true formula
      false formula
      pos (do (validate-atom lang (second formula)) formula)
      neg (do (validate-atom lang (second formula)) formula)
      eq (do (validate-term lang (second formula))
             (validate-term lang (nth formula 2))
             formula)
      neq (do (validate-term lang (second formula))
              (validate-term lang (nth formula 2))
              formula)
      and (do (validate-formula lang (second formula))
              (validate-formula lang (nth formula 2))
              formula)
      or (do (validate-formula lang (second formula))
             (validate-formula lang (nth formula 2))
             formula)
      not (do (validate-formula lang (second formula))
              formula)
      implies (do (validate-formula lang (second formula))
                  (validate-formula lang (nth formula 2))
                  formula)
      forall (do (validate-formula lang (:body (second formula)))
                 formula)
      once-forall (do (validate-formula lang (:body (second formula)))
                      formula)
      exists (do (validate-formula lang (:body (second formula)))
                 formula)
      (throw (ex-info "Malformed formula" {:formula formula})))))

(defn validate-query
  "Validate a surface query formula and return it unchanged on success."
  [lang query]
  (validate-formula lang query))

(defn validate-clause
  "Validate one surface clause against the declared language."
  [lang {:keys [relation params body] :as clause}]
  (let [expected-arity (declared-relation-arity lang relation)]
    (when (nil? expected-arity)
      (throw (ex-info (str "Undeclared relation symbol: " relation)
                      {:clause clause :symbol relation})))
    (when (not= expected-arity (count params))
      (throw (ex-info (str "Arity mismatch for relation symbol " relation)
                      {:clause clause
                       :symbol relation
                       :expected-arity expected-arity
                       :actual-arity (count params)})))
    (doseq [param params]
      (when-not (nominal/nom? param)
        (throw (ex-info "Clause parameters must be noms"
                        {:clause clause :parameter param}))))
    (validate-formula lang body)
    clause))

(defn- disjuncts
  "Flatten a formula's top-level disjunctions into core alternatives."
  [formula]
  (if (= 'or (ast/tag-of formula))
    (concat (disjuncts (second formula))
            (disjuncts (nth formula 2)))
    (list formula)))

(defn- conjuncts
  "Flatten a formula's top-level conjunctions."
  [formula]
  (if (= 'and (ast/tag-of formula))
    (concat (conjuncts (second formula))
            (conjuncts (nth formula 2)))
    (list formula)))

(defn- strip-leading-quantifiers
  "Return `[scope core]` for a formula after removing leading quantifiers.

   The scope is compile-time IR metadata. The original formula is still kept on
   the guarded alternative, so this helper does not change the executable
   clause body."
  [formula]
  (loop [scope []
         current formula]
    (case (ast/tag-of current)
      forall (let [tied (second current)]
               (recur (conj scope {:quantifier 'forall
                                   :binding-nom (:binding-nom tied)})
                      (:body tied)))
      once-forall (let [tied (second current)]
                    (recur (conj scope {:quantifier 'once-forall
                                        :binding-nom (:binding-nom tied)})
                           (:body tied)))
      exists (let [tied (second current)]
               (recur (conj scope {:quantifier 'exists
                                   :binding-nom (:binding-nom tied)})
                      (:body tied)))
      [scope current])))

(defn- guard-formula?
  [formula]
  (contains? #{'eq 'neq} (ast/tag-of formula)))

(defn- defined-call-formula?
  [defined-relations formula]
  (when (contains? #{'pos 'neg} (ast/tag-of formula))
    (let [atom (second formula)]
      (contains? defined-relations (second atom)))))

(defn- term-static-constructor-size
  "Count positive-arity constructor structure visible before proof search."
  [term]
  (case (ast/tag-of term)
    app (let [args (nnext term)]
          (+ (if (seq args) 1 0)
             (reduce + (map term-static-constructor-size args))))
    0))

(defn- formula-static-constructor-size
  [formula]
  (case (ast/tag-of formula)
    pos (let [atom (second formula)]
          (reduce + (map term-static-constructor-size (nnext atom))))
    neg (let [atom (second formula)]
          (reduce + (map term-static-constructor-size (nnext atom))))
    eq (+ (term-static-constructor-size (second formula))
          (term-static-constructor-size (nth formula 2)))
    neq (+ (term-static-constructor-size (second formula))
           (term-static-constructor-size (nth formula 2)))
    0))

(defn- demand-ordered-calls
  "Preserve source call order inside one guarded alternative.

   Visible constructor demand is useful as an alternate order, but replacing
   source order can move a consumer before the call that produces its symbolic
   input. Reverse/append output synthesis depends on producer-before-consumer
   source order."
  [calls]
  calls)

(defn- constructor-demand-ordered-calls
  "Build an alternate call order using only visible constructor structure."
  [calls]
  (->> calls
       (map-indexed vector)
       (sort-by (fn [[idx call]]
                  [(- (formula-static-constructor-size call)) idx]))
       (map second)))

(defn- distinct-lists
  "Preserve the first occurrence of each sequence after normalizing to lists."
  [xss]
  (:items
    (reduce (fn [{:keys [seen items] :as acc} xs]
              (let [xs (apply list xs)]
                (if (contains? seen xs)
                  acc
                  {:seen (conj seen xs)
                   :items (conj items xs)})))
            {:seen #{}
             :items []}
            xss)))

(defn- guarded-alternative-demand-score
  [guarded]
  [(- (count (:calls guarded)))
   (- (reduce + (map formula-static-constructor-size (:guards guarded))))
   (- (count (:guards guarded)))])

(defn- demand-ordered-guarded-alternatives
  "Prefer guarded alternatives that expose recursive demand before base cases."
  [guarded-alternatives]
  (->> guarded-alternatives
       (map-indexed vector)
       (sort-by (fn [[idx guarded]]
                  (conj (guarded-alternative-demand-score guarded) idx)))
       (map second)))

(defn- guarded-alternative
  "Build compile-time IR for one executable alternative.

   The IR is intentionally generic: it records equality/disequality guards,
   calls to relations defined by this program, and residual formulas without
   inspecting relation names or constructor names."
  [defined-relations formula]
  (let [[scope core] (strip-leading-quantifiers formula)
        parts (conjuncts core)
        grouped (reduce (fn [acc part]
                          (cond
                            (guard-formula? part)
                            (update acc :guards conj part)

                            (defined-call-formula? defined-relations part)
                            (update acc :calls conj part)

                            :else
                            (update acc :residuals conj part)))
                        {:guards []
                         :calls []
                         :residuals []}
                        parts)
        ordered-calls (demand-ordered-calls (:calls grouped))
        demand-calls (constructor-demand-ordered-calls (:calls grouped))
        negated-calls (map normalize/negate-formula ordered-calls)
        negated-demand-calls (map normalize/negate-formula demand-calls)
        negated-call-orders (distinct-lists [negated-calls
                                             negated-demand-calls])]
    {:formula formula
     :negated-formula (normalize/negate-formula formula)
     :scope (apply list scope)
     :core core
     :conjuncts (apply list parts)
     :negated-conjuncts (apply list (map normalize/negate-formula parts))
     :guards (apply list (:guards grouped))
     :negated-guards (apply list (map normalize/negate-formula (:guards grouped)))
     :calls (apply list ordered-calls)
     :demand-calls (apply list demand-calls)
     :negated-calls (apply list negated-calls)
     :negated-demand-calls (apply list negated-demand-calls)
     :negated-call-orders (apply list negated-call-orders)
     :residuals (apply list (:residuals grouped))
     :negated-residuals (apply list (map normalize/negate-formula (:residuals grouped)))
     :negated-ordered-conjuncts (apply list
                                        (map normalize/negate-formula
                                             (concat (:guards grouped)
                                                     ordered-calls
                                                     (:residuals grouped))))}))

(defn- clause-group->core-clause
  "Compile a group of same-relation surface clauses into one Fitting-style clause."
  [lang defined-relations relation clauses]
  (let [arity (declared-relation-arity lang relation)
        fresh-params (vec (repeatedly arity #(fresh-nom (gensym (str relation "-p")))))
        compiled-bodies
        (mapv (fn [{:keys [params body]}]
                (let [env (map (fn [old new]
                                 [old (ast/var-term new)])
                               params
                               fresh-params)]
                  (-> body
                      (subst/subst-formula env)
                      (normalize/to-nnf))))
              clauses)
        alternatives (vec (mapcat disjuncts compiled-bodies))
        compiled-body (reduce ast/or-form alternatives)
        guarded-alternatives (map #(guarded-alternative defined-relations %)
                                  alternatives)
        ordered-guarded-alternatives
        (demand-ordered-guarded-alternatives guarded-alternatives)]
    {:relation relation
     :params fresh-params
     :body compiled-body
     :negated-body (normalize/negate-formula compiled-body)
     :alternatives (apply list alternatives)
     :negated-alternatives (apply list (map normalize/negate-formula alternatives))
     :guarded-alternatives (apply list ordered-guarded-alternatives)}))

(defn- ordinary-clause-view
  [clause]
  (select-keys clause [:relation :params :body :negated-body]))

(defn- alternative-clause-view
  [clause]
  (select-keys clause [:relation
                       :params
                       :body
                       :negated-body
                       :alternatives
                       :negated-alternatives]))

(defn- guarded-clause-view
  [clause]
  (select-keys clause [:relation
                       :params
                       :body
                       :negated-body
                       :alternatives
                       :negated-alternatives
                       :guarded-alternatives]))

(defn compile-program
  "Validate and compile a surface program into the greenfield core form.

   Multiple surface clauses for one relation become a single compiled clause
   whose body is the disjunction of alpha-renamed, NNF-normalized bodies."
  [lang clauses]
  (doseq [clause clauses]
    (validate-clause lang clause))
  (let [groups (group-by :relation clauses)
        defined-relations (set (keys groups))]
    (let [compiled-clauses
          (into {}
                (map (fn [[relation same-relation-clauses]]
                       [relation (clause-group->core-clause
                                   lang
                                   defined-relations
                                   relation
                                   same-relation-clauses)]))
                groups)]
      {:language lang
       :clauses compiled-clauses
       ;; Keep a sequential view for the purely relational procedure-call rule.
       :clause-list (apply list (map ordinary-clause-view (vals compiled-clauses)))
       ;; Keep top-level alternatives separate so ordinary call lookup
       ;; preserves the historical compiled-clause shape and answer-mode search
       ;; order.
       :alternative-clause-list (apply list
                                       (map alternative-clause-view
                                            (vals compiled-clauses)))
       ;; Keep guarded alternative metadata in its own list so future
       ;; source-to-IR execution paths can opt in without perturbing ordinary
       ;; procedure-call lookup.
       :guarded-clause-list (apply list
                                   (map guarded-clause-view
                                        (vals compiled-clauses)))})))
