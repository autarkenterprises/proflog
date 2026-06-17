(ns proflog.scheduling-benchmarks
  "Proof-preserving scheduling benchmarks for representative Proflog searches.

   Semantic expectations are recorded independently of branch-growth envelopes so
   future scheduling optimizations cannot trade correctness for speed."
  (:require [proflog.ast :as ast]
            [proflog.kernel :as kernel]
            [proflog.literature-tableau-golden :as golden]
            [proflog.proof :as proof]))

(defn- lit
  [sym polarity]
  (if (= polarity :pos)
    (ast/pos-lit (ast/app-term sym))
    (ast/neg-lit (ast/app-term sym))))

(defn benchmark-record
  [{:keys [id kind suite formula formula-key semantic max-steps optimization-target]
    :or {suite :fast max-steps 64}}]
  (cond-> {:id id
           :kind kind
           :suite suite
           :semantic semantic
           :max-steps max-steps}
    formula (assoc :formula formula)
    formula-key (assoc :formula-key formula-key)
    optimization-target (assoc :optimization-target optimization-target)))

(def benchmarks
  [{:id :single-pending-goal-open
    :kind :single-pending-goal
    :formula-key :atom-p
    :semantic :open
    :max-steps 8
    :optimization-target :goal-selection}
   {:id :single-pending-goal-closed
    :kind :single-pending-goal
    :formula-key :contradiction-basic
    :semantic :closes
    :max-steps 16
    :optimization-target :early-closure}
   {:id :multi-goal-open
    :kind :multi-goal
    :formula (ast/and-form (lit 'p :pos) (lit 'q :pos))
    :semantic :open
    :max-steps 16}
   {:id :multi-goal-closed
    :kind :multi-goal
    :formula-key :multiple-inconsistent
    :semantic :closes
    :max-steps 32}
   {:id :deterministic-alpha-beta
    :kind :deterministic-expansion
    :formula-key :alpha-beta
    :semantic :open
    :max-steps 32
    :optimization-target :alpha-before-beta}
   {:id :nondeterministic-disjunction
    :kind :nondeterministic-expansion
    :formula-key :large-disjunction
    :semantic :open
    :suite :extended
    :max-steps 64
    :optimization-target :branch-ordering}
   {:id :branching-closed
    :kind :branching
    :formula-key :contradiction-complex
    :semantic :closes
    :max-steps 48}
   {:id :branching-open
    :kind :branching
    :formula-key :deep-nesting
    :semantic :open
    :suite :extended
    :max-steps 64}])

(def benchmark-catalog
  (mapv benchmark-record benchmarks))

(defn formula-for-benchmark
  [{:keys [formula formula-key]}]
  (or formula (golden/formula-for-entry {:formula-key formula-key})))

(defn observe-semantic
  [formula]
  (if (seq (kernel/prove formula 1))
    :closes
    :open))

(defn measure-benchmark
  "Return semantic observation and, for closed cases, proof-step count."
  [{:keys [formula formula-key semantic] :as benchmark}]
  (let [f (or formula (golden/formula-for-entry {:formula-key formula-key}))
        proofs (kernel/prove f 1)
        observation (if (seq proofs) :closes :open)
        branch-growth-applicable (= :closes semantic)
        step-count (when (and branch-growth-applicable (seq proofs))
                     (count (proof/collect-steps (first proofs))))]
    (assoc benchmark
           :formula f
           :observation observation
           :branch-growth-applicable branch-growth-applicable
           :step-count step-count)))

(defn semantic-preservation-ok?
  "True when the observed semantic result matches the benchmark baseline."
  [measured]
  (= (:semantic measured) (:observation measured)))

(defn branch-growth-ok?
  "True when proof-step count stays within the recorded envelope for closed cases."
  [measured]
  (if (:branch-growth-applicable measured)
    (and (some? (:step-count measured))
         (<= (:step-count measured) (:max-steps measured)))
    true))

(defn run-benchmark
  "Run semantic and branch-growth checks. Returns a result map."
  [benchmark]
  (let [measured (measure-benchmark benchmark)]
    {:benchmark (:id measured)
     :semantic-ok (semantic-preservation-ok? measured)
     :branch-growth-applicable (:branch-growth-applicable measured)
     :branch-growth-ok (branch-growth-ok? measured)
     :observation (:observation measured)
     :expected (:semantic measured)
     :step-count (:step-count measured)
     :max-steps (:max-steps measured)}))

(defn fast-benchmarks
  []
  (filter #(= :fast (:suite %)) benchmark-catalog))

(defn extended-benchmarks
  []
  (filter #(= :extended (:suite %)) benchmark-catalog))

(defn required-kinds
  []
  #{:single-pending-goal :multi-goal :deterministic-expansion
    :nondeterministic-expansion :branching})
