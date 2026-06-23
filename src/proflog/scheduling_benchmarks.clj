(ns proflog.scheduling-benchmarks
  "Proof-preserving scheduling benchmarks for representative Proflog searches.

   Open tableaux do not have a closed proof artifact, so their benchmark record
   uses a deterministic structural estimate of branch growth. Closed tableaux
   additionally record the recognized proof-step count from the kernel proof
   object."
  (:require [proflog.ast :as ast]
            [proflog.kernel :as kernel]
            [proflog.literature-tableau-golden :as golden]
            [proflog.normalize :as normalize]
            [proflog.proof :as proof]))

(defn- lit
  [sym]
  (ast/pos-lit (ast/app-term sym)))

(defn- and*
  [& formulas]
  (reduce ast/and-form formulas))

(defn- or*
  [& formulas]
  (reduce ast/or-form formulas))

(defn- not*
  [formula]
  (ast/not-form formula))

(defn- benchmark
  [{:keys [benchmark-id kind suite formula formula-key expected max-branches
           max-closed-proof-steps origin optimization-target]
    :or {suite :fast max-branches 64 max-closed-proof-steps 64}}]
  (let [f (or formula (golden/formula-for-key formula-key))]
    (cond-> {:benchmark-id benchmark-id
             :id benchmark-id
             :kind kind
             :suite suite
             :formula f
             :expected expected
             :semantic expected
             :max-branches max-branches
             :max-closed-proof-steps max-closed-proof-steps
             :origin origin}
      formula-key (assoc :formula-key formula-key)
      optimization-target (assoc :optimization-target optimization-target))))

(def benchmark-cases
  [(benchmark
     {:benchmark-id :single-pending-goal-open
      :kind :single-pending-goal
      :formula-key :atom-p
      :expected :open
      :max-branches 1
      :origin "single atom open branch"
      :optimization-target :goal-selection})
   (benchmark
     {:benchmark-id :single-pending-goal-closed
      :kind :single-pending-goal
      :formula-key :contradiction-basic
      :expected :closes
      :origin "single conjunction with complementary literals"
      :optimization-target :early-closure})
   (benchmark
     {:benchmark-id :multi-goal-open
      :kind :multi-goal
      :formula-key :satisfiable-conjunction
      :expected :open
      :origin "multi-literal open conjunction"})
   (benchmark
     {:benchmark-id :multi-goal-closed
      :kind :multi-goal
      :formula-key :multiple-formulas-inconsistent
      :expected :closes
      :origin "upstream multiple-formulas inconsistent case"})
   (benchmark
     {:benchmark-id :deterministic-alpha
      :kind :deterministic-expansion
      :formula-key :large-conjunction
      :expected :open
      :origin "large deterministic conjunction"})
   (benchmark
     {:benchmark-id :nondeterministic-disjunction
      :kind :nondeterministic-expansion
      :formula-key :large-disjunction
      :expected :open
      :suite :extended
      :max-branches 16
      :origin "large disjunction from upstream edge tests"
      :optimization-target :branch-ordering})
   (benchmark
     {:benchmark-id :branching-closed
      :kind :branching
      :formula (and* (or* (lit 'p) (lit 'q))
                     (not* (lit 'p))
                     (not* (lit 'q)))
      :expected :closes
      :origin "branching disjunction with both complementary branches"})
   (benchmark
     {:benchmark-id :branch-bound-fitting
      :kind :branching
      :formula-key :fitting-branch-bound
      :expected :open
      :suite :extended
      :max-branches 50
      :origin "Fitting branch-bound example from reviewed tableaux corpus"
      :optimization-target :branch-growth})])

(def benchmark-catalog benchmark-cases)

(defn formula-for-benchmark
  [{:keys [formula formula-key]}]
  (or formula (golden/formula-for-key formula-key)))

(defn- stats-leaf
  []
  {:formula-size 1
   :expansion-count 0
   :estimated-branches 1
   :max-depth 1
   :literal-count 1})

(defn- combine-and
  [left right]
  {:formula-size (+ 1 (:formula-size left) (:formula-size right))
   :expansion-count (+ 1 (:expansion-count left) (:expansion-count right))
   :estimated-branches (* (:estimated-branches left)
                          (:estimated-branches right))
   :max-depth (inc (max (:max-depth left) (:max-depth right)))
   :literal-count (+ (:literal-count left) (:literal-count right))})

(defn- combine-or
  [left right]
  {:formula-size (+ 1 (:formula-size left) (:formula-size right))
   :expansion-count (+ 1 (:expansion-count left) (:expansion-count right))
   :estimated-branches (+ (:estimated-branches left)
                          (:estimated-branches right))
   :max-depth (inc (max (:max-depth left) (:max-depth right)))
   :literal-count (+ (:literal-count left) (:literal-count right))})

(defn structural-stats
  "Return a profile-independent structural search estimate for a normalized formula."
  [formula]
  (case (ast/tag-of formula)
    true (assoc (stats-leaf) :literal-count 0)
    false (assoc (stats-leaf) :literal-count 0)
    pos (stats-leaf)
    neg (stats-leaf)
    eq (stats-leaf)
    neq (stats-leaf)
    and (combine-and (structural-stats (second formula))
                     (structural-stats (nth formula 2)))
    or (combine-or (structural-stats (second formula))
                   (structural-stats (nth formula 2)))
    forall (update (structural-stats (:body (second formula))) :max-depth inc)
    once-forall (update (structural-stats (:body (second formula))) :max-depth inc)
    exists (update (structural-stats (:body (second formula))) :max-depth inc)
    (throw (ex-info "Unsupported normalized formula for scheduling stats"
                    {:formula formula}))))

(defn observe-semantic
  [formula]
  (if (seq (kernel/prove (normalize/to-nnf formula) 1))
    :closes
    :open))

(defn closed-proof-step-count
  [formula]
  (when-let [proof (first (kernel/prove (normalize/to-nnf formula) 1))]
    (count (proof/collect-steps proof))))

(defn measure-formula
  "Measure one benchmark-like formula map.

   Required keys are `:formula` and `:expected`; `:benchmark-id`, `:origin`, and
   branch/step limits are preserved when supplied."
  [{:keys [formula expected max-branches max-closed-proof-steps]
    :or {max-branches 64 max-closed-proof-steps 64}
    :as benchmark}]
  (let [start (System/nanoTime)
        normalized (normalize/to-nnf formula)
        result (observe-semantic formula)
        stats (structural-stats normalized)
        closed-steps (when (= :closes result)
                       (closed-proof-step-count formula))
        elapsed-ms (/ (double (- (System/nanoTime) start)) 1000000.0)
        search-measure (or closed-steps (:estimated-branches stats))]
    (assoc benchmark
           :formula formula
           :normalized-formula normalized
           :result result
           :observation result
           :elapsed-ms elapsed-ms
           :closed-proof-step-count closed-steps
           :step-count (or closed-steps 0)
           :search-measure search-measure
           :semantic-ok (= expected result)
           :branch-growth-ok (and (<= (:estimated-branches stats) max-branches)
                                  (or (nil? closed-steps)
                                      (<= closed-steps max-closed-proof-steps)))
           :max-steps max-closed-proof-steps
           :max-branches max-branches
           :max-closed-proof-steps max-closed-proof-steps
           :formula-size (:formula-size stats)
           :expansion-count (:expansion-count stats)
           :estimated-branches (:estimated-branches stats)
           :max-depth (:max-depth stats)
           :literal-count (:literal-count stats))))

(defn measure-benchmark
  [benchmark]
  (measure-formula benchmark))

(defn semantic-preservation-ok?
  [measured]
  (= (:expected measured) (:result measured)))

(defn branch-growth-ok?
  [measured]
  (true? (:branch-growth-ok measured)))

(defn run-benchmark
  [benchmark]
  (measure-benchmark benchmark))

(defn run-benchmarks
  []
  (mapv measure-benchmark benchmark-cases))

(defn fast-benchmarks
  []
  (filterv #(= :fast (:suite %)) benchmark-cases))

(defn extended-benchmarks
  []
  (filterv #(= :extended (:suite %)) benchmark-cases))

(defn required-kinds
  []
  #{:single-pending-goal :multi-goal :deterministic-expansion
    :nondeterministic-expansion :branching})
