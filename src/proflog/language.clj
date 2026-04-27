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

(defn- clause-group->core-clause
  "Compile a group of same-relation surface clauses into one Fitting-style clause."
  [lang relation clauses]
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
        compiled-body (reduce ast/or-form compiled-bodies)]
    {:relation relation
     :params fresh-params
     :body compiled-body
     :negated-body (normalize/negate-formula compiled-body)}))

(defn compile-program
  "Validate and compile a surface program into the greenfield core form.

   Multiple surface clauses for one relation become a single compiled clause
   whose body is the disjunction of alpha-renamed, NNF-normalized bodies."
  [lang clauses]
  (doseq [clause clauses]
    (validate-clause lang clause))
  (let [groups (group-by :relation clauses)]
    (let [compiled-clauses
          (into {}
                (map (fn [[relation same-relation-clauses]]
                       [relation (clause-group->core-clause
                                   lang relation same-relation-clauses)]))
                groups)]
      {:language lang
       :clauses compiled-clauses
       ;; Keep a sequential view for the purely relational procedure-call rule.
       :clause-list (apply list (vals compiled-clauses))})))
