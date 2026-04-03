;; ============================================================================
;; αleanTAP-EP Fast: Explicit Forward Execution Engine
;; ============================================================================
;;
;; This namespace implements a sibling execution engine for forward proof
;; search. It keeps the surface syntax of αleanTAP-EP, but replaces the
;; relation-driven control flow with explicit Clojure recursion and cached
;; branch state. The reference prover in `cljtap.alphaleantap-ep` remains the
;; semantic oracle; this engine targets the performance-critical forward mode.
;;
;; Scope of the first implementation:
;;   - forward proof search only
;;   - same formula/program syntax
;;   - explicit branch state + cached literal indexes
;;   - core.logic substitutions retained as the unification substrate
;;
;; ============================================================================

(ns cljtap.alphaleantap-ep-fast
  (:require [clojure.core.logic :as logic]
            [clojure.core.logic.nominal :as nominal]))

;; ============================================================================
;; Part 1: Syntax Helpers
;; ============================================================================

(defn- tagged-seq?
  [x tag]
  (and (sequential? x) (= (first x) tag)))

(defn- var-term?
  [x]
  (tagged-seq? x 'var))

(defn- par-term?
  [x]
  (tagged-seq? x 'par))

(defn- app-term?
  [x]
  (tagged-seq? x 'app))

(defn- pos-lit?
  [x]
  (tagged-seq? x 'pos))

(defn- neg-lit?
  [x]
  (tagged-seq? x 'neg))

(defn- eq-lit?
  [x]
  (tagged-seq? x 'eq))

(defn- neq-lit?
  [x]
  (tagged-seq? x 'neq))

(defn- and-fml?
  [x]
  (tagged-seq? x 'and))

(defn- or-fml?
  [x]
  (tagged-seq? x 'or))

(defn- forall-fml?
  [x]
  (tagged-seq? x 'forall))

(defn- once-forall-fml?
  [x]
  (tagged-seq? x 'once-forall))

(defn- exists-fml?
  [x]
  (tagged-seq? x 'exists))

(defn- app-head
  [term]
  (second term))

(defn- app-args
  [term]
  (vec (drop 2 term)))

(defn- make-app
  [head args]
  (into ['app head] args))

(defn- make-pos
  [term]
  ['pos term])

(defn- make-neg
  [term]
  ['neg term])

(defn- make-eq
  [lhs rhs]
  ['eq lhs rhs])

(defn- make-neq
  [lhs rhs]
  ['neq lhs rhs])

(defn- tie-binding
  [tie]
  (:binding-nom tie))

(defn- tie-body
  [tie]
  (:body tie))

;; ============================================================================
;; Part 2: Fresh Values and Unification
;; ============================================================================

(defn- empty-subst
  []
  (assoc logic/empty-s :oc true))

(defn- fresh-lvar
  []
  (logic/lvar (gensym "g__")))

(defn- fresh-par
  []
  ['par (nominal/nom (logic/lvar (gensym "p__")))])

(defn- walk*
  [subst term]
  (logic/walk* subst term))

(defn- unify
  [subst lhs rhs]
  (logic/unify subst lhs rhs))

;; ============================================================================
;; Part 3: Deterministic Term / Literal Operations
;; ============================================================================

(declare subst-term)

(defn- subst-term*
  [env terms]
  (mapv #(subst-term env %) terms))

(defn- subst-term
  [env term]
  (cond
    (var-term? term)
    (get env (second term) term)

    (par-term? term)
    term

    (app-term? term)
    (make-app (app-head term) (subst-term* env (app-args term)))

    :else
    term))

(defn- subst-lit
  [env lit]
  (cond
    (pos-lit? lit) (make-pos (subst-term env (second lit)))
    (neg-lit? lit) (make-neg (subst-term env (second lit)))
    (eq-lit? lit)  (make-eq (subst-term env (second lit))
                            (subst-term env (nth lit 2)))
    (neq-lit? lit) (make-neq (subst-term env (second lit))
                             (subst-term env (nth lit 2)))
    :else          lit))

(defn- normalize-term
  [subst term]
  (walk* subst term))

(defn- normalize-lit
  [subst lit]
  (cond
    (pos-lit? lit) (make-pos (normalize-term subst (second lit)))
    (neg-lit? lit) (make-neg (normalize-term subst (second lit)))
    (eq-lit? lit)  (make-eq (normalize-term subst (second lit))
                            (normalize-term subst (nth lit 2)))
    (neq-lit? lit) (make-neq (normalize-term subst (second lit))
                             (normalize-term subst (nth lit 2)))
    :else          lit))

(defn- contains-par?
  [term]
  (cond
    (par-term? term)  true
    (vector? term)    (boolean (some contains-par? (rest term)))
    (sequential? term) (boolean (some contains-par? term))
    :else             false))

(defn- l-ground-term?
  [term]
  (not (contains-par? term)))

(defn- l-ground-args?
  [args]
  (every? l-ground-term? args))

(defn- free-closure?
  [t1 t2]
  (and (app-term? t1)
       (app-term? t2)
       (not= (app-head t1) (app-head t2))))

(defn- arity-mismatch-closure?
  [t1 t2]
  (and (app-term? t1)
       (app-term? t2)
       (= (app-head t1) (app-head t2))
       (not= (count (app-args t1)) (count (app-args t2)))))

(defn- one-one-pairs
  [args1 args2]
  (vec
    (mapcat (fn [t u] [[t u] [u t]]) args1 args2)))

(defn- collect-eq-pairs
  [eq-lits]
  (vec
    (mapcat
      (fn [[t1 t2]]
        (let [pairs [[t1 t2] [t2 t1]]]
          (if (and (app-term? t1)
                   (app-term? t2)
                   (= (app-head t1) (app-head t2)))
            (into pairs (one-one-pairs (app-args t1) (app-args t2)))
            pairs)))
      eq-lits)))

(defn- decompose-eq-args
  [args1 args2]
  (let [pairs (map vector args1 args2)]
    (when (seq pairs)
      (reduce
        (fn [acc [lhs rhs]]
          (if acc
            ['and acc (make-eq lhs rhs)]
            (make-eq lhs rhs)))
        nil
        pairs))))

(defn negate-formula
  [fml]
  (cond
    (and-fml? fml)
    ['or (negate-formula (second fml))
     (negate-formula (nth fml 2))]

    (or-fml? fml)
    ['and (negate-formula (second fml))
     (negate-formula (nth fml 2))]

    (forall-fml? fml)
    (let [t (second fml)]
      ['exists (nominal/tie (tie-binding t)
                            (negate-formula (tie-body t)))])

    (exists-fml? fml)
    (let [t (second fml)]
      ['once-forall (nominal/tie (tie-binding t)
                                 (negate-formula (tie-body t)))])

    (once-forall-fml? fml)
    (let [t (second fml)]
      ['exists (nominal/tie (tie-binding t)
                            (negate-formula (tie-body t)))])

    (pos-lit? fml)
    (make-neg (second fml))

    (neg-lit? fml)
    (make-pos (second fml))

    (eq-lit? fml)
    (make-neq (second fml) (nth fml 2))

    (neq-lit? fml)
    (make-eq (second fml) (nth fml 2))

    :else
    (throw (ex-info "Unsupported formula for negation" {:formula fml}))))

;; ============================================================================
;; Part 4: Program Compilation
;; ============================================================================

(defn- check-program!
  [program]
  (let [rels (map first program)
        dups (into {} (filter #(> (val %) 1) (frequencies rels)))]
    (when (seq dups)
      (throw
        (IllegalArgumentException.
          (str "Invalid Proflog program (Fitting Def 2.1): duplicate clause(s) "
               "for relation(s) " (pr-str (keys dups))
               ". Each relation symbol may have at most one clause."))))))

(defn compile-program
  [program]
  (check-program! program)
  (into {}
        (map (fn [[rel params body]]
               [rel {:params (vec params)
                     :body   body}]))
        program))

;; ============================================================================
;; Part 5: Deque and Branch State
;; ============================================================================

(defn- dq-empty
  []
  {:front '() :back '()})

(defn- dq-push-front
  [dq x]
  (update dq :front #(cons x %)))

(defn- dq-push-back
  [dq x]
  (update dq :back #(cons x %)))

(defn- dq-pop-front
  [dq]
  (cond
    (seq (:front dq))
    [(first (:front dq))
     (assoc dq :front (rest (:front dq)))]

    (seq (:back dq))
    (let [front (reverse (:back dq))]
      [(first front)
       {:front (rest front) :back '()}])

    :else
    nil))

(defn- initial-state
  [gamma-budget]
  {:subst        (empty-subst)
   :env          {}
   :unexp        (dq-empty)
   :lits         []
   :cache        nil
   :gamma-budget gamma-budget})

(defn- state-with-subst
  [state subst]
  (if (identical? subst (:subst state))
    state
    (assoc state :subst subst :cache nil)))

(defn- state-push-front
  [state fml]
  (update state :unexp dq-push-front fml))

(defn- state-push-back
  [state fml]
  (update state :unexp dq-push-back fml))

(defn- state-add-lit
  [state lit]
  (-> state
      (update :lits conj lit)
      (assoc :cache nil)))

(defn- subsidiary-state
  [state env]
  {:subst        (:subst state)
   :env          env
   :unexp        (dq-empty)
   :lits         []
   :cache        nil
   :gamma-budget (:gamma-budget state)})

;; ============================================================================
;; Part 6: Cached Literal Indexes
;; ============================================================================

(defn- rel-head-key
  [term]
  (if (app-term? term)
    (app-head term)
    ::other))

(defn- build-cache
  [state]
  (let [subst (:subst state)
        cache0 {:pos-index {}
                :neg-index {}
                :eq-lits   []
                :neq-lits  []}
        cache1
        (reduce
          (fn [cache lit]
            (let [lit (normalize-lit subst lit)]
              (cond
                (pos-lit? lit)
                (update-in cache [:pos-index (rel-head-key (second lit))]
                           (fnil conj []) (second lit))

                (neg-lit? lit)
                (update-in cache [:neg-index (rel-head-key (second lit))]
                           (fnil conj []) (second lit))

                (eq-lit? lit)
                (update cache :eq-lits conj [(second lit) (nth lit 2)])

                (neq-lit? lit)
                (update cache :neq-lits conj [(second lit) (nth lit 2)])

                :else
                cache)))
          cache0
          (:lits state))]
    (assoc cache1 :eq-pairs (collect-eq-pairs (:eq-lits cache1)))))

(defn- ensure-cache
  [state]
  (if (:cache state)
    state
    (assoc state :cache (build-cache state))))

;; ============================================================================
;; Part 7: Rewriting Search
;; ============================================================================

(defn- remove-index
  [coll idx]
  (into []
        (concat (subvec coll 0 idx)
                (subvec coll (inc idx)))))

(declare rewrite-term-once)

(defn- rewrite-term-once
  [subst term lhs rhs]
  (let [term (normalize-term subst term)
        lhs  (normalize-term subst lhs)
        rhs  (normalize-term subst rhs)
        root-result
        (when-let [s1 (unify subst term lhs)]
          [[s1 (normalize-term s1 rhs)]])]
    (lazy-cat
      root-result
      (when (app-term? term)
        (let [args (app-args term)
              head (app-head term)]
          (mapcat
            (fn [idx arg]
              (for [[s1 arg-out] (rewrite-term-once subst arg lhs rhs)]
                [s1 (normalize-term s1
                                    (make-app head
                                              (assoc args idx arg-out)))]))
            (range (count args))
            args))))))

(defn- rewrite-literal-once
  [subst lit lhs rhs]
  (cond
    (pos-lit? lit)
    (for [[s1 tm-out] (rewrite-term-once subst (second lit) lhs rhs)]
      [s1 (make-pos tm-out)])

    (neg-lit? lit)
    (for [[s1 tm-out] (rewrite-term-once subst (second lit) lhs rhs)]
      [s1 (make-neg tm-out)])

    :else
    '()))

(defn- rewrite-by-any-eq
  [subst term eq-pairs]
  (mapcat
    (fn [[lhs rhs]]
      (rewrite-term-once subst term lhs rhs))
    eq-pairs))

(declare rewrite-args-maybe)

(defn- rewrite-args-maybe
  [subst args eq-pairs]
  (if (empty? args)
    (list [subst [] false])
    (let [arg  (first args)
          rest (vec (rest args))]
      (lazy-cat
        ;; Rewrite current arg once using any branch equality.
        (mapcat
          (fn [[s1 arg-out]]
            (for [[s2 rest-out changed?] (rewrite-args-maybe s1 rest eq-pairs)]
              [s2
               (into [(normalize-term s2 arg-out)] rest-out)
               true]))
          (rewrite-by-any-eq subst arg eq-pairs))
        ;; Keep current arg unchanged.
        (for [[s1 rest-out changed?] (rewrite-args-maybe subst rest eq-pairs)]
          [s1
           (into [(normalize-term s1 arg)] rest-out)
           changed?])))))

(defn- rewrite-term-with-eqs
  [subst term eq-pairs]
  (if-not (app-term? term)
    '()
    (let [term (normalize-term subst term)
          head (app-head term)
          args (app-args term)]
      (for [[s1 args-out changed?] (rewrite-args-maybe subst args eq-pairs)
            :when changed?]
        [s1 (make-app head args-out)]))))

(defn- candidate-unifiers
  [subst target-term candidates]
  (keep (fn [cand] (unify subst target-term cand)) candidates))

(declare eq-member-search)

(defn- eq-member-search
  [subst target-lit candidates eq-pairs depth]
  (let [target-term (second target-lit)
        direct      (candidate-unifiers subst target-term candidates)
        indexed     (vec eq-pairs)]
    (lazy-cat
      direct
      (mapcat
        (fn [idx [lhs rhs]]
          (let [remaining (remove-index indexed idx)
                rewritten (rewrite-literal-once subst target-lit lhs rhs)]
            (mapcat
              (fn [[s1 lit-out]]
                (if (> depth 0)
                  (eq-member-search s1 lit-out candidates remaining (dec depth))
                  (candidate-unifiers s1 (second lit-out) candidates)))
              rewritten)))
        (range (count indexed))
        indexed))))

(declare eq-neq-close-search)

(defn- eq-neq-close-search
  [subst t1 t2 eq-pairs depth]
  (let [pairs (vec eq-pairs)]
    (mapcat
      (fn [idx [lhs rhs]]
        (let [remaining (remove-index pairs idx)]
          (mapcat
            (fn [[s1 t1-out]]
              (if-let [s2 (unify s1 t1-out t2)]
                (list s2)
                (when (> depth 0)
                  (eq-neq-close-search s1 t1-out t2 remaining (dec depth)))))
            (rewrite-term-once subst t1 lhs rhs))))
      (range (count pairs))
      pairs)))

(declare para-free-close-search)

(defn- para-free-close-search
  [subst t1 t2 eq-pairs depth]
  (let [pairs (vec eq-pairs)]
    (mapcat
      (fn [idx [lhs rhs]]
        (let [remaining (remove-index pairs idx)]
          (lazy-cat
            ;; Rewrite the left side.
            (mapcat
              (fn [[s1 t1-out]]
                (let [lhs* (normalize-term s1 t1-out)
                      rhs* (normalize-term s1 t2)]
                  (if (free-closure? lhs* rhs*)
                    (list s1)
                    (when (> depth 0)
                      (para-free-close-search s1 lhs* rhs* remaining (dec depth))))))
              (rewrite-term-once subst t1 lhs rhs))
            ;; Rewrite the right side.
            (mapcat
              (fn [[s1 t2-out]]
                (let [lhs* (normalize-term s1 t1)
                      rhs* (normalize-term s1 t2-out)]
                  (if (free-closure? lhs* rhs*)
                    (list s1)
                    (when (> depth 0)
                      (para-free-close-search s1 lhs* rhs* remaining (dec depth))))))
              (rewrite-term-once subst t2 lhs rhs)))))
      (range (count pairs))
      pairs)))

;; ============================================================================
;; Part 8: Proof Search
;; ============================================================================

(declare prove* continue-with-next)

(defn- continue-with-next
  [ctx state proof-tag lit]
  (when-let [[next-fml unexp] (dq-pop-front (:unexp state))]
    (let [next-state (-> state
                         (assoc :unexp unexp)
                         (state-add-lit lit))]
      (map (fn [{:keys [subst proof]}]
             {:subst subst
              :proof [proof-tag proof]})
           (prove* ctx next-fml next-state)))))

(defn- closure-result
  [subst proof]
  {:subst subst :proof proof})

(defn- lookup-clause
  [ctx rel]
  (get (:program ctx) rel))

(defn- call-results
  [ctx state rel args body proof-tag]
  (let [params (:params (lookup-clause ctx rel))
        call-env (zipmap params args)
        substate (subsidiary-state state call-env)]
    (map (fn [{:keys [subst proof]}]
           (closure-result subst [proof-tag rel proof]))
         (prove* ctx body substate))))

(defn- pos-candidates
  [state term]
  (get-in state [:cache :pos-index (rel-head-key term)] []))

(defn- neg-candidates
  [state term]
  (get-in state [:cache :neg-index (rel-head-key term)] []))

(defn- prove-pos
  [ctx lit state]
  (let [state (ensure-cache state)
        subst (:subst state)
        tm    (normalize-term subst (second lit))
        head  (rel-head-key tm)
        negs  (get-in state [:cache :neg-index head] [])
        eqs   (get-in state [:cache :eq-pairs] [])]
    (lazy-cat
      ;; Complementary closure.
      (map #(closure-result % ['close])
           (candidate-unifiers subst tm negs))
      ;; Paramodulated complementary closure.
      (map #(closure-result % ['para-close])
           (eq-member-search subst (make-pos tm) negs eqs 3))
      ;; Plain procedure call.
      (when (app-term? tm)
        (when-let [clause (lookup-clause ctx (app-head tm))]
          (let [args (app-args tm)]
            (when (l-ground-args? args)
              (call-results ctx state (app-head tm) args (:body clause) 'proc-call)))))
      ;; Equality-rewritten procedure call.
      (when (app-term? tm)
        (when-let [clause (lookup-clause ctx (app-head tm))]
          (mapcat
            (fn [[s1 new-tm]]
              (call-results ctx
                            (state-with-subst state s1)
                            (app-head new-tm)
                            (app-args new-tm)
                            (:body clause)
                            'subst-call))
            (rewrite-term-with-eqs subst tm eqs))))
      ;; Save and continue.
      (continue-with-next ctx state 'savefml lit))))

(defn- prove-neg
  [ctx lit state]
  (let [state (ensure-cache state)
        subst (:subst state)
        tm    (normalize-term subst (second lit))
        head  (rel-head-key tm)
        poss  (get-in state [:cache :pos-index head] [])
        eqs   (get-in state [:cache :eq-pairs] [])]
    (lazy-cat
      ;; Complementary closure.
      (map #(closure-result % ['close])
           (candidate-unifiers subst tm poss))
      ;; Paramodulated complementary closure.
      (map #(closure-result % ['para-close])
           (eq-member-search subst (make-neg tm) poss eqs 3))
      ;; Plain negative procedure call.
      (when (app-term? tm)
        (when-let [clause (lookup-clause ctx (app-head tm))]
          (let [args (app-args tm)]
            (when (l-ground-args? args)
              (call-results ctx state (app-head tm) args
                            (negate-formula (:body clause))
                            'neg-proc-call)))))
      ;; Equality-rewritten negative procedure call.
      (when (app-term? tm)
        (when-let [clause (lookup-clause ctx (app-head tm))]
          (mapcat
            (fn [[s1 new-tm]]
              (call-results ctx
                            (state-with-subst state s1)
                            (app-head new-tm)
                            (app-args new-tm)
                            (negate-formula (:body clause))
                            'neg-subst-call))
            (rewrite-term-with-eqs subst tm eqs))))
      ;; Save and continue.
      (continue-with-next ctx state 'savefml lit))))

(defn- prove-neq
  [ctx lit state]
  (let [state (ensure-cache state)
        subst (:subst state)
        t1    (normalize-term subst (second lit))
        t2    (normalize-term subst (nth lit 2))
        eqs   (get-in state [:cache :eq-pairs] [])]
    (lazy-cat
      ;; Reflexivity closure.
      (when-let [s1 (unify subst t1 t2)]
        (list (closure-result s1 ['refl-close])))
      ;; Equality-driven closure.
      (map #(closure-result % ['eq-refl-close])
           (eq-neq-close-search subst t1 t2 eqs 3))
      ;; Save and continue.
      (continue-with-next ctx state 'savefml lit))))

(defn- eq-neq-complements
  [subst t1 t2 neq-lits]
  (mapcat
    (fn [[n1 n2]]
      (lazy-cat
        (when-let [s1 (unify subst t1 n1)]
          (when-let [s2 (unify s1 t2 n2)]
            (list s2)))
        (when-let [s1 (unify subst t1 n2)]
          (when-let [s2 (unify s1 t2 n1)]
            (list s2)))))
    neq-lits))

(defn- eq-conflicts
  [subst t1 t2 eq-lits]
  (mapcat
    (fn [[a b]]
      (lazy-cat
        (when-let [s1 (unify subst t1 a)]
          (let [lhs (normalize-term s1 t2)
                rhs (normalize-term s1 b)]
            (when (free-closure? lhs rhs)
              (list s1))))
        (when-let [s1 (unify subst t1 b)]
          (let [lhs (normalize-term s1 t2)
                rhs (normalize-term s1 a)]
            (when (free-closure? lhs rhs)
              (list s1))))
        (when-let [s1 (unify subst t2 a)]
          (let [lhs (normalize-term s1 t1)
                rhs (normalize-term s1 b)]
            (when (free-closure? lhs rhs)
              (list s1))))
        (when-let [s1 (unify subst t2 b)]
          (let [lhs (normalize-term s1 t1)
                rhs (normalize-term s1 a)]
            (when (free-closure? lhs rhs)
              (list s1))))))
    eq-lits))

(defn- add-current-eq
  [state t1 t2]
  (let [eq-lits  (conj (vec (get-in state [:cache :eq-lits])) [t1 t2])]
    {:eq-lits  eq-lits
     :eq-pairs (collect-eq-pairs eq-lits)}))

(defn- prove-eq
  [ctx lit state]
  (let [state   (ensure-cache state)
        subst   (:subst state)
        t1      (normalize-term subst (second lit))
        t2      (normalize-term subst (nth lit 2))
        neq-lits (get-in state [:cache :neq-lits] [])
        eq-lits  (get-in state [:cache :eq-lits] [])
        eqs+     (add-current-eq state t1 t2)]
    (lazy-cat
      ;; Free closure.
      (when (free-closure? t1 t2)
        (list (closure-result subst ['free-close])))
      ;; Arity mismatch.
      (when (arity-mismatch-closure? t1 t2)
        (list (closure-result subst ['arity-mismatch-close])))
      ;; Eq/Neq complementary closure.
      (map #(closure-result % ['eq-neq-close])
           (eq-neq-complements subst t1 t2 neq-lits))
      ;; Eq conflict closure.
      (map #(closure-result % ['eq-conflict-close])
           (eq-conflicts subst t1 t2 eq-lits))
      ;; One-one decomposition.
      (when (and (app-term? t1)
                 (app-term? t2)
                 (= (app-head t1) (app-head t2))
                 (seq (app-args t1)))
        (when-let [decomposed (decompose-eq-args (app-args t1) (app-args t2))]
          (map (fn [{:keys [subst proof]}]
                 (closure-result subst ['decompose proof]))
               (prove* ctx decomposed state))))
      ;; Paramodulated free closure.
      (map #(closure-result % ['para-free-close])
           (para-free-close-search subst t1 t2 (:eq-pairs eqs+) 3))
      ;; Eq-triggered positive procedure call.
      (mapcat
        (fn [[rel terms]]
          (mapcat
            (fn [tm]
              (mapcat
                (fn [[s1 new-tm]]
                  (when-let [clause (lookup-clause ctx rel)]
                    (call-results ctx
                                  (state-with-subst state s1)
                                  rel
                                  (app-args new-tm)
                                  (:body clause)
                                  'eq-triggered-call)))
                (rewrite-term-with-eqs subst tm (:eq-pairs eqs+))))
            terms))
        (get-in state [:cache :pos-index] {}))
      ;; Eq-triggered negative procedure call.
      (mapcat
        (fn [[rel terms]]
          (mapcat
            (fn [tm]
              (mapcat
                (fn [[s1 new-tm]]
                  (when-let [clause (lookup-clause ctx rel)]
                    (call-results ctx
                                  (state-with-subst state s1)
                                  rel
                                  (app-args new-tm)
                                  (negate-formula (:body clause))
                                  'eq-triggered-neg-call)))
                (rewrite-term-with-eqs subst tm (:eq-pairs eqs+))))
            terms))
        (get-in state [:cache :neg-index] {}))
      ;; Eq-triggered disequality closure.
      (mapcat
        (fn [[n1 n2]]
          (map #(closure-result % ['eq-triggered-neq-close])
               (eq-neq-close-search subst n1 n2 (:eq-pairs eqs+) 3)))
        neq-lits)
      ;; Save and continue.
      (continue-with-next ctx state 'savefml lit))))

(defn prove*
  [ctx fml state]
  (lazy-seq
    (cond
      (and-fml? fml)
      (let [e1 (second fml)
            e2 (nth fml 2)
            next-state (state-push-front state e2)]
        (map (fn [{:keys [subst proof]}]
               (closure-result subst ['conj proof]))
             (prove* ctx e1 next-state)))

      (or-fml? fml)
      (let [e1 (second fml)
            e2 (nth fml 2)
            base-state state]
        (mapcat
          (fn [{left-subst :subst left-proof :proof}]
            (let [right-state (state-with-subst base-state left-subst)]
              (map (fn [{right-subst :subst right-proof :proof}]
                     (closure-result right-subst
                                     ['split left-proof right-proof]))
                   (prove* ctx e2 right-state))))
          (prove* ctx e1 base-state)))

      (forall-fml? fml)
      (let [budget (:gamma-budget state)]
        (when (or (nil? budget) (pos? budget))
          (let [t     (second fml)
                x     (fresh-lvar)
                state (-> state
                          (assoc :env (assoc (:env state) (tie-binding t) x)
                                 :gamma-budget (when budget (dec budget)))
                          (state-push-back fml))]
            (map (fn [{:keys [subst proof]}]
                   (closure-result subst ['univ proof]))
                 (prove* ctx (tie-body t) state)))))

      (once-forall-fml? fml)
      (let [t     (second fml)
            x     (fresh-lvar)
            state (assoc state :env (assoc (:env state) (tie-binding t) x))]
        (map (fn [{:keys [subst proof]}]
               (closure-result subst ['once-univ proof]))
             (prove* ctx (tie-body t) state)))

      (exists-fml? fml)
      (let [t     (second fml)
            p     (fresh-par)
            state (assoc state :env (assoc (:env state) (tie-binding t) p))]
        (map (fn [{:keys [subst proof]}]
               (closure-result subst ['witness proof]))
             (prove* ctx (tie-body t) state)))

      :else
      (let [lit (subst-lit (:env state) fml)]
        (cond
          (pos-lit? lit) (prove-pos ctx lit state)
          (neg-lit? lit) (prove-neg ctx lit state)
          (neq-lit? lit) (prove-neq ctx lit state)
          (eq-lit? lit)  (prove-eq ctx lit state)
          :else          '())))))

;; ============================================================================
;; Part 9: Public Interface
;; ============================================================================

(defn prove-fast
  "Explicit forward tableau proof search.
   Returns up to `n` proof terms."
  ([formula]
   (prove-fast formula 1))
  ([formula n]
   (prove-fast '() formula n))
  ([program formula n]
   (prove-fast program formula n nil))
  ([program formula n gamma-budget]
   (let [ctx   {:program (compile-program program)}
         state (initial-state gamma-budget)]
     (vec
       (take n
             (map :proof
                  (prove* ctx formula state)))))))

(defn query-succeeds-fast
  "A query succeeds iff there is a closed tableau for its negation."
  ([program query]
   (query-succeeds-fast program query 1))
  ([program query n]
   (query-succeeds-fast program query n nil))
  ([program query n gamma-budget]
   (prove-fast program (negate-formula query) n gamma-budget)))

(defn query-fails-fast
  "A query fails iff there is a closed tableau for the query itself."
  ([program query]
   (query-fails-fast program query 1))
  ([program query n]
   (query-fails-fast program query n nil))
  ([program query n gamma-budget]
   (prove-fast program query n gamma-budget)))

(defn query-succeeds-id-fast
  "Iterative deepening on gamma-budget."
  ([program query]
   (query-succeeds-id-fast program query 64))
  ([program query max-budget]
   (loop [budget 1]
     (when (<= budget max-budget)
       (let [result (query-succeeds-fast program query 1 budget)]
         (if (seq result)
           result
           (recur (* 2 budget))))))))

(defn query-fails-id-fast
  "Iterative deepening on gamma-budget."
  ([program query]
   (query-fails-id-fast program query 64))
  ([program query max-budget]
   (loop [budget 1]
     (when (<= budget max-budget)
       (let [result (query-fails-fast program query 1 budget)]
         (if (seq result)
           result
           (recur (* 2 budget))))))))
