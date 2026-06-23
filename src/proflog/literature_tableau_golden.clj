(ns proflog.literature-tableau-golden
  "Literature tableau golden suite seeded by the reviewed
   `bradleypallen/tableaux` corpus at commit `fa5a736`.

   The catalog is intentionally explicit.  Every upstream test keeps a row, and
   every Proflog-runnable row has a translation record that states which source
   assertions are retained, which assertions are dropped because they depend on
   non-portable APIs, and which Proflog formula is executed."
  (:require [clojure.string :as str]
            [proflog.ast :as ast]
            [proflog.kernel :as kernel]
            [proflog.normalize :as normalize]
            [proflog.proof :as proof]))

(def upstream-commit
  "fa5a736090465d0ddf35362a6271d4298d668d42")

(def valid-dispositions
  #{:direct :analog :performance :unsupported})

(def valid-reconciliation-statuses
  #{:resolved :deferred :unsupported :source-disputed})

(defn- lit
  [sym]
  (ast/pos-lit (ast/app-term sym)))

(defn- not*
  [formula]
  (ast/not-form formula))

(defn- and*
  [& formulas]
  (reduce ast/and-form formulas))

(defn- or*
  [& formulas]
  (reduce ast/or-form formulas))

(defn- implies*
  [antecedent consequent]
  (ast/implies-form antecedent consequent))

(defn- chain-implies
  [syms]
  (reduce implies* (map lit syms)))

(def p (lit 'p))
(def q (lit 'q))
(def r (lit 'r))
(def s (lit 's))

(def formulas
  "Formula keys used by the translation records.

   Values remain surface formulas where the source used `not` or `implies`;
   proof execution normalizes them before invoking the kernel."
  {:atom-p p
   :neg-p (not* p)
   :contradiction-basic (and* p (not* p))
   :contradiction-complex (and* (implies* p q) p (not* q))
   :excluded-middle (or* p (not* p))
   :transitivity-tautology
   (implies* (and* (implies* p q) (implies* q r))
             (implies* p r))
   :material-implication-forward
   (implies* (implies* p q) (or* (not* p) q))
   :material-implication-reverse
   (implies* (or* (not* p) q) (implies* p q))
   :satisfiable-conjunction (and* p q)
   :satisfiable-disjunction (or* p q)
   :satisfiable-implication (implies* p q)
   :satisfiable-implication-negation (not* (implies* p q))
   :complex-nested-formula
   (and* (implies* (and* p q) r)
         (implies* r s)
         (and* p q))
   :de-morgan-and-forward
   (implies* (not* (and* p q))
             (or* (not* p) (not* q)))
   :de-morgan-and-reverse
   (implies* (or* (not* p) (not* q))
             (not* (and* p q)))
   :de-morgan-or-forward
   (implies* (not* (or* p q))
             (and* (not* p) (not* q)))
   :de-morgan-or-reverse
   (implies* (and* (not* p) (not* q))
             (not* (or* p q)))
   :multiple-formulas-consistent
   (and* (implies* p q) (implies* q r) p)
   :multiple-formulas-inconsistent
   (and* p (implies* p q) (not* q))
   :performance-formula-prioritization
   (and* (or* p q) r)
   :subsumption-elimination
   (or* (and* p q) p)
   :model-extraction
   (or* (and* p q) (and* (not* p) r))
   :empty-branch (ast/true-form)
   :single-formula p
   :very-deep-nesting
   (nth (iterate not* p) 10)
   :large-disjunction
   (apply or* (map #(lit (symbol (str "p" %))) (range 10)))
   :large-conjunction
   (apply and* (map #(lit (symbol (str "p" %))) (range 10)))
   :performance-nested-implications
   (chain-implies ['p0 'p1 'p2 'p3 'p4])
   :fitting-basic-expansion
   (not* (and* p q))
   :fitting-closure
   (and* p (not* p) q)
   :fitting-satisfiable
   (and* (or* p q) (or* (not* p) r))
   :smullyan-alpha-beta
   (not* (and* p q))
   :smullyan-systematic-tautology
   (implies* (implies* p q)
             (implies* (implies* q r)
                       (implies* p r)))
   :smullyan-completeness
   (or* (and* p q) (and* (not* p) (not* q)))
   :handbook-signed-conjunction
   (and* p q)
   :handbook-optimization
   (and* (or* p q) (or* (not* p) r) (or* (not* q) s))
   :priest-deep-tautology
   (implies* p (implies* q (implies* p (implies* q p))))
   :fitting-branch-bound
   (and* (or* (lit 'p0) (lit 'p1))
         (or* (lit 'p0) (lit 'p2))
         (or* (lit 'p1) (lit 'p3))
         (or* (lit 'p2) (lit 'p3)))
   :ferguson-classical-contradiction
   (and* p (not* p))
   :formula-p p
   :formula-pq (and* p q)
   :formula-p-or-q (or* p q)
   :formula-impl (implies* p q)
   :formula-em (or* p (not* p))
   :formula-pierce (or* (implies* p q)
                        (implies* q p))
   :formula-contradiction (and* p (not* p))
   :formula-modus-ponens-contradiction
   (and* (implies* p q) p (not* q))
   :formula-distribution
   (and* (or* p q) (or* (not* p) r))
   :formula-peirce
   (implies* (implies* (implies* p q) p)
             p)})

(defn formula-for-key
  [formula-key]
  (get formulas formula-key))

(defn refutation-formula-for-key
  [formula-key]
  (not* (formula-for-key formula-key)))

(defn- normalize-formula
  [formula]
  (normalize/to-nnf formula))

(defn proflog-observation
  "Independently observe whether Proflog closes `formula`."
  [formula]
  (if (seq (kernel/prove (normalize-formula formula) 1))
    :closes
    :open))

(defn prove-with-steps
  "Return closure observation and recognized proof-step count for `formula`."
  [formula]
  (let [proofs (kernel/prove (normalize-formula formula) 1)]
    {:result (if (seq proofs) :closes :open)
     :observation (if (seq proofs) :closes :open)
     :steps (if (seq proofs)
              (count (proof/collect-steps (first proofs)))
              0)
     :step-count (if (seq proofs)
                   (count (proof/collect-steps (first proofs)))
                   0)}))

(defn- row
  [{:keys [id source-file source-test coverage source-location
           source-expectation source-note unsupported-reason suite]
    :or {suite :fast}}]
  (cond-> {:id id
           :source-file source-file
           :source-test source-test
           :coverage coverage
           :disposition coverage
           :source-location source-location
           :source-expectation source-expectation
           :suite suite}
    source-note (assoc :source-note source-note)
    unsupported-reason (assoc :unsupported-reason unsupported-reason)))

(defn- translation
  [{:keys [source-test source-expectation proflog-mode proflog-expectation
           formula-key formula-keys refutation-formula-key
           refutation-formula-keys retained-assertions dropped-assertions
           source-location source-note]}]
  (cond-> {:source-test source-test
           :source-expectation source-expectation
           :proflog-mode proflog-mode
           :proflog-expectation proflog-expectation
           :retained-assertions (vec retained-assertions)
           :dropped-assertions (vec dropped-assertions)}
    formula-key (assoc :formula-key formula-key)
    formula-keys (assoc :formula-keys (vec formula-keys))
    refutation-formula-key (assoc :refutation-formula-key refutation-formula-key)
    refutation-formula-keys (assoc :refutation-formula-keys (vec refutation-formula-keys))
    source-location (assoc :source-location source-location)
    source-note (assoc :source-note source-note)))

(defn- direct
  [id source-test location expectation formula-key retained]
  (let [source-file (first (str/split location #":"))]
    {:row (row {:id id
              :source-file source-file
              :source-test source-test
              :coverage :direct
              :source-location location
              :source-expectation expectation})
     :translation (translation
                    {:source-test source-test
                     :source-expectation expectation
                     :source-location location
                     :proflog-mode :satisfiability
                     :proflog-expectation expectation
                     :formula-key formula-key
                     :retained-assertions retained
                     :dropped-assertions []})}))

(def catalog-records
  [(direct :comprehensive/test-simple-atom
           "test_simple_atom"
           "tests/test_comprehensive.py:25"
           :open
           :atom-p
           ["formula is satisfiable"])
   (direct :comprehensive/test-simple-negation
           "test_simple_negation"
           "tests/test_comprehensive.py:36"
           :open
           :neg-p
           ["formula is satisfiable"])
   (direct :comprehensive/test-contradiction-basic
           "test_contradiction_basic"
           "tests/test_comprehensive.py:55"
           :closes
           :contradiction-basic
           ["formula is unsatisfiable"])
   (direct :comprehensive/test-contradiction-complex
           "test_contradiction_complex"
           "tests/test_comprehensive.py:68"
           :closes
           :contradiction-complex
           ["formula is unsatisfiable"])
   {:row (row {:id :comprehensive/test-tautology-excluded-middle
               :source-file "tests/test_comprehensive.py"
               :source-test "test_tautology_excluded_middle"
               :coverage :direct
               :source-location "tests/test_comprehensive.py:80"
               :source-expectation :closes})
    :translation (translation
                   {:source-test "test_tautology_excluded_middle"
                    :source-expectation :closes
                    :source-location "tests/test_comprehensive.py:80"
                    :proflog-mode :refutation
                    :proflog-expectation :closes
                    :refutation-formula-key :excluded-middle
                    :retained-assertions ["formula is satisfiable"
                                          "negation of formula is unsatisfiable"]
                    :dropped-assertions []})}
   {:row (row {:id :comprehensive/test-tautology-transitivity
               :source-file "tests/test_comprehensive.py"
               :source-test "test_tautology_transitivity"
               :coverage :direct
               :source-location "tests/test_comprehensive.py:92"
               :source-expectation :closes})
    :translation (translation
                   {:source-test "test_tautology_transitivity"
                    :source-expectation :closes
                    :source-location "tests/test_comprehensive.py:92"
                    :proflog-mode :refutation
                    :proflog-expectation :closes
                    :refutation-formula-key :transitivity-tautology
                    :retained-assertions ["negation of transitivity tautology is unsatisfiable"]
                    :dropped-assertions []})}
   {:row (row {:id :comprehensive/test-tautology-material-implication
               :source-file "tests/test_comprehensive.py"
               :source-test "test_tautology_material_implication"
               :coverage :direct
               :source-location "tests/test_comprehensive.py:106"
               :source-expectation :closes})
    :translation (translation
                   {:source-test "test_tautology_material_implication"
                    :source-expectation :closes
                    :source-location "tests/test_comprehensive.py:106"
                    :proflog-mode :all-refutations
                    :proflog-expectation :closes
                    :refutation-formula-keys [:material-implication-forward
                                              :material-implication-reverse]
                    :retained-assertions ["both material-implication equivalence directions refute"]
                    :dropped-assertions []})}
   (direct :comprehensive/test-satisfiable-conjunction
           "test_satisfiable_conjunction"
           "tests/test_comprehensive.py:123"
           :open
           :satisfiable-conjunction
           ["formula is satisfiable"])
   (direct :comprehensive/test-satisfiable-disjunction
           "test_satisfiable_disjunction"
           "tests/test_comprehensive.py:134"
           :open
           :satisfiable-disjunction
           ["formula is satisfiable"])
   {:row (row {:id :comprehensive/test-satisfiable-implication
               :source-file "tests/test_comprehensive.py"
               :source-test "test_satisfiable_implication"
               :coverage :direct
               :source-location "tests/test_comprehensive.py:145"
               :source-expectation :open})
    :translation (translation
                   {:source-test "test_satisfiable_implication"
                    :source-expectation :open
                    :source-location "tests/test_comprehensive.py:145"
                    :proflog-mode :all-satisfiable
                    :proflog-expectation :open
                    :formula-keys [:satisfiable-implication
                                   :satisfiable-implication-negation]
                    :retained-assertions ["formula is satisfiable"
                                          "negation of formula is also satisfiable"]
                    :dropped-assertions []})}
   {:row (row {:id :comprehensive/test-complex-nested-formula
               :source-file "tests/test_comprehensive.py"
               :source-test "test_complex_nested_formula"
               :coverage :analog
               :source-location "tests/test_comprehensive.py:158"
               :source-expectation :open})
    :translation (translation
                   {:source-test "test_complex_nested_formula"
                    :source-expectation :open
                    :source-location "tests/test_comprehensive.py:158"
                    :proflog-mode :satisfiability
                    :proflog-expectation :open
                    :formula-key :complex-nested-formula
                    :retained-assertions ["formula is satisfiable"]
                    :dropped-assertions ["all extracted models set s=true"]})}
   {:row (row {:id :comprehensive/test-de-morgan-laws
               :source-file "tests/test_comprehensive.py"
               :source-test "test_de_morgan_laws"
               :coverage :direct
               :source-location "tests/test_comprehensive.py:176"
               :source-expectation :closes})
    :translation (translation
                   {:source-test "test_de_morgan_laws"
                    :source-expectation :closes
                    :source-location "tests/test_comprehensive.py:176"
                    :proflog-mode :all-refutations
                    :proflog-expectation :closes
                    :refutation-formula-keys [:de-morgan-and-forward
                                              :de-morgan-and-reverse
                                              :de-morgan-or-forward
                                              :de-morgan-or-reverse]
                    :retained-assertions ["all four De Morgan implication negations are unsatisfiable"]
                    :dropped-assertions []})}
   {:row (row {:id :comprehensive/test-multiple-formulas-consistent
               :source-file "tests/test_comprehensive.py"
               :source-test "test_multiple_formulas_consistent"
               :coverage :analog
               :source-location "tests/test_comprehensive.py:198"
               :source-expectation :open})
    :translation (translation
                   {:source-test "test_multiple_formulas_consistent"
                    :source-expectation :open
                    :source-location "tests/test_comprehensive.py:198"
                    :proflog-mode :satisfiability
                    :proflog-expectation :open
                    :formula-key :multiple-formulas-consistent
                    :retained-assertions ["formula set is satisfiable"]
                    :dropped-assertions ["all extracted models set p,q,r=true"]})}
   (direct :comprehensive/test-multiple-formulas-inconsistent
           "test_multiple_formulas_inconsistent"
           "tests/test_comprehensive.py:215"
           :closes
           :multiple-formulas-inconsistent
           ["formula set is unsatisfiable"])
   {:row (row {:id :comprehensive/test-formula-prioritization
               :source-file "tests/test_comprehensive.py"
               :source-test "test_formula_prioritization"
               :coverage :performance
               :suite :extended
               :source-location "tests/test_comprehensive.py:507"
               :source-expectation :open})
    :translation (translation
                   {:source-test "test_formula_prioritization"
                    :source-expectation :open
                    :source-location "tests/test_comprehensive.py:507"
                    :proflog-mode :satisfiability
                    :proflog-expectation :open
                    :formula-key :performance-formula-prioritization
                    :retained-assertions ["formula is satisfiable"]
                    :dropped-assertions ["formula prioritization order"]})}
   {:row (row {:id :comprehensive/test-subsumption-elimination
               :source-file "tests/test_comprehensive.py"
               :source-test "test_subsumption_elimination"
               :coverage :performance
               :suite :extended
               :source-location "tests/test_comprehensive.py:531"
               :source-expectation :open})
    :translation (translation
                   {:source-test "test_subsumption_elimination"
                    :source-expectation :open
                    :source-location "tests/test_comprehensive.py:531"
                    :proflog-mode :satisfiability
                    :proflog-expectation :open
                    :formula-key :subsumption-elimination
                    :retained-assertions ["formula is satisfiable"]
                    :dropped-assertions ["subsumption branch-count comparison"]})}
   {:row (row {:id :comprehensive/test-early-satisfiability-detection
               :source-file "tests/test_comprehensive.py"
               :source-test "test_early_satisfiability_detection"
               :coverage :performance
               :suite :extended
               :source-location "tests/test_comprehensive.py:553"
               :source-expectation :open})
    :translation (translation
                   {:source-test "test_early_satisfiability_detection"
                    :source-expectation :open
                    :source-location "tests/test_comprehensive.py:553"
                    :proflog-mode :satisfiability
                    :proflog-expectation :open
                    :formula-key :atom-p
                    :retained-assertions ["single atom is satisfiable"]
                    :dropped-assertions ["early satisfiability optimization path"]})}
   {:row (row {:id :comprehensive/test-performance-complex-formula
               :source-file "tests/test_comprehensive.py"
               :source-test "test_performance_complex_formula"
               :coverage :performance
               :suite :extended
               :source-location "tests/test_comprehensive.py:562"
               :source-expectation :open})
    :translation (translation
                   {:source-test "test_performance_complex_formula"
                    :source-expectation :open
                    :source-location "tests/test_comprehensive.py:562"
                    :proflog-mode :satisfiability
                    :proflog-expectation :open
                    :formula-key :performance-nested-implications
                    :retained-assertions ["nested implication formula is satisfiable"]
                    :dropped-assertions ["wall-clock limit"]})}
   {:row (row {:id :comprehensive/test-model-extraction-correctness
               :source-file "tests/test_comprehensive.py"
               :source-test "test_model_extraction_correctness"
               :coverage :analog
               :source-location "tests/test_comprehensive.py:574"
               :source-expectation :open})
    :translation (translation
                   {:source-test "test_model_extraction_correctness"
                    :source-expectation :open
                    :source-location "tests/test_comprehensive.py:574"
                    :proflog-mode :satisfiability
                    :proflog-expectation :open
                    :formula-key :model-extraction
                    :retained-assertions ["formula is satisfiable"]
                    :dropped-assertions ["all extracted models satisfy source formula"]})}
   (direct :comprehensive/test-empty-formula-list
           "test_empty_formula_list"
           "tests/test_comprehensive.py:594"
           :open
           :empty-branch
           ["empty formula list is satisfiable"])
   (direct :comprehensive/test-single-formula-in-list
           "test_single_formula_in_list"
           "tests/test_comprehensive.py:603"
           :open
           :single-formula
           ["single formula list is satisfiable"])
   {:row (row {:id :comprehensive/test-very-deep-nesting
               :source-file "tests/test_comprehensive.py"
               :source-test "test_very_deep_nesting"
               :coverage :analog
               :source-location "tests/test_comprehensive.py:612"
               :source-expectation :open})
    :translation (translation
                   {:source-test "test_very_deep_nesting"
                    :source-expectation :open
                    :source-location "tests/test_comprehensive.py:612"
                    :proflog-mode :satisfiability
                    :proflog-expectation :open
                    :formula-key :very-deep-nesting
                    :retained-assertions ["deeply nested negation search terminates with a boolean result"]
                    :dropped-assertions ["exact upstream boolean return API"]})}
   (direct :comprehensive/test-large-disjunction
           "test_large_disjunction"
           "tests/test_comprehensive.py:625"
           :open
           :large-disjunction
           ["large disjunction is satisfiable"])
   (direct :comprehensive/test-large-conjunction
           "test_large_conjunction"
           "tests/test_comprehensive.py:638"
           :open
           :large-conjunction
           ["large conjunction is satisfiable"])])

(def literature-records
  [{:row (row {:id :literature/test-fitting-basic-expansion-example
               :source-file "tests/test_literature_examples.py"
               :source-test "test_fitting_basic_expansion_example"
               :coverage :analog
               :source-location "tests/test_literature_examples.py:170"
               :source-expectation :open
               :source-note "Melvin Fitting, First-Order Logic and Automated Theorem Proving."})
    :translation (translation
                   {:source-test "test_fitting_basic_expansion_example"
                    :source-expectation :open
                    :source-location "tests/test_literature_examples.py:170"
                    :proflog-mode :satisfiability
                    :proflog-expectation :open
                    :formula-key :fitting-basic-expansion
                    :retained-assertions ["formula is satisfiable"]
                    :dropped-assertions ["signed branch shape T:~(p&q), F:p or F:q"]})}
   {:row (row {:id :literature/test-fitting-closure-example
               :source-file "tests/test_literature_examples.py"
               :source-test "test_fitting_closure_example"
               :coverage :direct
               :source-location "tests/test_literature_examples.py:205"
               :source-expectation :closes
               :source-note "Melvin Fitting, First-Order Logic and Automated Theorem Proving."})
    :translation (translation
                   {:source-test "test_fitting_closure_example"
                    :source-expectation :closes
                    :source-location "tests/test_literature_examples.py:205"
                    :proflog-mode :satisfiability
                    :proflog-expectation :closes
                    :formula-key :fitting-closure
                    :retained-assertions ["formula closes from p and not p"]
                    :dropped-assertions []})}
   {:row (row {:id :literature/test-fitting-satisfiable-example
               :source-file "tests/test_literature_examples.py"
               :source-test "test_fitting_satisfiable_example"
               :coverage :analog
               :source-location "tests/test_literature_examples.py:235"
               :source-expectation :open
               :source-note "Melvin Fitting, First-Order Logic and Automated Theorem Proving."})
    :translation (translation
                   {:source-test "test_fitting_satisfiable_example"
                    :source-expectation :open
                    :source-location "tests/test_literature_examples.py:235"
                    :proflog-mode :satisfiability
                    :proflog-expectation :open
                    :formula-key :fitting-satisfiable
                    :retained-assertions ["formula is satisfiable"]
                    :dropped-assertions ["model extraction assertion"]})}
   {:row (row {:id :literature/test-smullyan-alpha-beta-classification
               :source-file "tests/test_literature_examples.py"
               :source-test "test_smullyan_alpha_beta_classification"
               :coverage :analog
               :source-location "tests/test_literature_examples.py:281"
               :source-expectation :open
               :source-note "Raymond M. Smullyan, First-Order Logic."})
    :translation (translation
                   {:source-test "test_smullyan_alpha_beta_classification"
                    :source-expectation :open
                    :source-location "tests/test_literature_examples.py:281"
                    :proflog-mode :satisfiability
                    :proflog-expectation :open
                    :formula-key :smullyan-alpha-beta
                    :retained-assertions ["alpha/beta example is satisfiable"]
                    :dropped-assertions ["signed alpha/beta rule classification details"]})}
   {:row (row {:id :literature/test-smullyan-systematic-tableau-construction
               :source-file "tests/test_literature_examples.py"
               :source-test "test_smullyan_systematic_tableau_construction"
               :coverage :direct
               :source-location "tests/test_literature_examples.py:326"
               :source-expectation :closes
               :source-note "Raymond M. Smullyan, First-Order Logic."})
    :translation (translation
                   {:source-test "test_smullyan_systematic_tableau_construction"
                    :source-expectation :closes
                    :source-location "tests/test_literature_examples.py:326"
                    :proflog-mode :refutation
                    :proflog-expectation :closes
                    :refutation-formula-key :smullyan-systematic-tautology
                    :retained-assertions ["negation of systematic tautology closes"]
                    :dropped-assertions []})}
   {:row (row {:id :literature/test-smullyan-completeness-example
               :source-file "tests/test_literature_examples.py"
               :source-test "test_smullyan_completeness_example"
               :coverage :analog
               :source-location "tests/test_literature_examples.py:368"
               :source-expectation :open
               :source-note "Raymond M. Smullyan, First-Order Logic."})
    :translation (translation
                   {:source-test "test_smullyan_completeness_example"
                    :source-expectation :open
                    :source-location "tests/test_literature_examples.py:368"
                    :proflog-mode :satisfiability
                    :proflog-expectation :open
                    :formula-key :smullyan-completeness
                    :retained-assertions ["formula is satisfiable"]
                    :dropped-assertions ["complete set of extracted models"]})}
   {:row (row {:id :literature/test-handbook-signed-semantic-tableaux
               :source-file "tests/test_literature_examples.py"
               :source-test "test_handbook_signed_semantic_tableaux"
               :coverage :analog
               :source-location "tests/test_literature_examples.py:401"
               :source-expectation :open
               :source-note "D'Agostino/Gabbay/Hahnle/Posegga, Handbook of Tableau Methods."})
    :translation (translation
                   {:source-test "test_handbook_signed_semantic_tableaux"
                    :source-expectation :open
                    :source-location "tests/test_literature_examples.py:401"
                    :proflog-mode :satisfiability
                    :proflog-expectation :open
                    :formula-key :handbook-signed-conjunction
                    :retained-assertions ["classical conjunction is satisfiable"]
                    :dropped-assertions ["signed T:p and T:q branch representation"]})}
   {:row (row {:id :literature/test-handbook-optimization-techniques
               :source-file "tests/test_literature_examples.py"
               :source-test "test_handbook_optimization_techniques"
               :coverage :analog
               :source-location "tests/test_literature_examples.py:463"
               :source-expectation :open
               :source-note "D'Agostino/Gabbay/Hahnle/Posegga, Handbook of Tableau Methods."})
    :translation (translation
                   {:source-test "test_handbook_optimization_techniques"
                    :source-expectation :open
                    :source-location "tests/test_literature_examples.py:463"
                    :proflog-mode :satisfiability
                    :proflog-expectation :open
                    :formula-key :handbook-optimization
                    :retained-assertions ["complex classical formula returns a semantic result"]
                    :dropped-assertions ["Handbook implementation-specific optimization technique"]})}
   {:row (row {:id :literature/test-deep-nesting-priest-example
               :source-file "tests/test_literature_examples.py"
               :source-test "test_deep_nesting_priest_example"
               :coverage :direct
               :source-location "tests/test_literature_examples.py:503"
               :source-expectation :closes
               :source-note "Graham Priest nested implication example, classical fragment."})
    :translation (translation
                   {:source-test "test_deep_nesting_priest_example"
                    :source-expectation :closes
                    :source-location "tests/test_literature_examples.py:503"
                    :proflog-mode :refutation
                    :proflog-expectation :closes
                    :refutation-formula-key :priest-deep-tautology
                    :retained-assertions ["negation of nested implication closes classically"]
                    :dropped-assertions []})}
   {:row (row {:id :literature/test-fitting-branch-bound-example
               :source-file "tests/test_literature_examples.py"
               :source-test "test_fitting_branch_bound_example"
               :coverage :performance
               :suite :extended
               :source-location "tests/test_literature_examples.py:548"
               :source-expectation :open
               :source-note "Melvin Fitting branch-bound discussion."})
    :translation (translation
                   {:source-test "test_fitting_branch_bound_example"
                    :source-expectation :open
                    :source-location "tests/test_literature_examples.py:548"
                    :proflog-mode :satisfiability
                    :proflog-expectation :open
                    :formula-key :fitting-branch-bound
                    :retained-assertions ["formula is satisfiable"]
                    :dropped-assertions ["upstream branch count is implementation-specific"]})}
   {:row (row {:id :literature/test-ferguson-classical-contradiction-still-works
               :source-file "tests/test_literature_examples.py"
               :source-test "test_ferguson_classical_contradiction_still_works"
               :coverage :direct
               :source-location "tests/test_literature_examples.py:626"
               :source-expectation :closes
               :source-note "T. M. Ferguson suite classical subcase."})
    :translation (translation
                   {:source-test "test_ferguson_classical_contradiction_still_works"
                    :source-expectation :closes
                    :source-location "tests/test_literature_examples.py:626"
                    :proflog-mode :satisfiability
                    :proflog-expectation :closes
                    :formula-key :ferguson-classical-contradiction
                    :retained-assertions ["classical contradiction closes"]
                    :dropped-assertions []})}])

(def performance-records
  [{:row (row {:id :performance/test-prioritization-benefit
               :source-file "tests/test_performance.py"
               :source-test "test_prioritization_benefit"
               :coverage :performance
               :suite :extended
               :source-location "tests/test_performance.py:33"
               :source-expectation :open})
    :translation (translation
                   {:source-test "test_prioritization_benefit"
                    :source-expectation :open
                    :source-location "tests/test_performance.py:33"
                    :proflog-mode :satisfiability
                    :proflog-expectation :open
                    :formula-key :performance-formula-prioritization
                    :retained-assertions ["prioritization fixture remains satisfiable"]
                    :dropped-assertions ["relative performance speedup"]})}
   {:row (row {:id :performance/test-subsumption-benefit
               :source-file "tests/test_performance.py"
               :source-test "test_subsumption_benefit"
               :coverage :analog
               :suite :extended
               :source-location "tests/test_performance.py:62"
               :source-expectation :open})
    :translation (translation
                   {:source-test "test_subsumption_benefit"
                    :source-expectation :open
                    :source-location "tests/test_performance.py:62"
                    :proflog-mode :satisfiability
                    :proflog-expectation :open
                    :formula-key :subsumption-elimination
                    :retained-assertions ["subsumption fixture remains satisfiable"]
                    :dropped-assertions ["relative subsumption speedup"]})}
   {:row (row {:id :performance/test-complex-formula-performance
               :source-file "tests/test_performance.py"
               :source-test "test_complex_formula_performance"
               :coverage :performance
               :suite :extended
               :source-location "tests/test_performance.py:92"
               :source-expectation :open})
    :translation (translation
                   {:source-test "test_complex_formula_performance"
                    :source-expectation :open
                    :source-location "tests/test_performance.py:92"
                    :proflog-mode :satisfiability
                    :proflog-expectation :open
                    :formula-key :performance-nested-implications
                    :retained-assertions ["complex performance fixture remains satisfiable"]
                    :dropped-assertions ["absolute runtime threshold"]})}
   (direct :performance/test-termination-correctness
           "test_termination_correctness"
           "tests/test_performance.py:119"
           :closes
           :contradiction-basic
           ["contradiction terminates and closes"])])

(def formula-records
  [{:row (row {:id :setup/signed-tableau-smoke
               :source-file "tests/test_setup.py"
               :source-test "setup_smoke"
               :coverage :analog
               :source-location "tests/test_setup.py:1"
               :source-expectation :open})
    :translation (translation
                   {:source-test "setup_smoke"
                    :source-expectation :open
                    :source-location "tests/test_setup.py:1"
                    :proflog-mode :satisfiability
                    :proflog-expectation :open
                    :formula-key :atom-p
                    :retained-assertions ["simple atom can be built and checked"]
                    :dropped-assertions ["signed T:p tableau object shape"]})}
   (direct :formulas/p
           "p"
           "tests/test_formulas.txt:1"
           :open
           :formula-p
           ["formula is satisfiable"])
   (direct :formulas/p-and-q
           "p-and-q"
           "tests/test_formulas.txt:2"
           :open
           :formula-pq
           ["formula is satisfiable"])
   (direct :formulas/p-or-q
           "p-or-q"
           "tests/test_formulas.txt:3"
           :open
           :formula-p-or-q
           ["formula is satisfiable"])
   (direct :formulas/p-impl-q
           "p-impl-q"
           "tests/test_formulas.txt:4"
           :open
           :formula-impl
           ["formula is satisfiable"])
   {:row (row {:id :formulas/excluded-middle
               :source-file "tests/test_formulas.txt"
               :source-test "excluded-middle"
               :coverage :direct
               :source-location "tests/test_formulas.txt:5"
               :source-expectation :closes})
    :translation (translation
                   {:source-test "excluded-middle"
                    :source-expectation :closes
                    :source-location "tests/test_formulas.txt:5"
                    :proflog-mode :refutation
                    :proflog-expectation :closes
                    :refutation-formula-key :formula-em
                    :retained-assertions ["negation of excluded middle closes"]
                    :dropped-assertions []})}
   (direct :formulas/pierce-law
           "pierce-law"
           "tests/test_formulas.txt:6"
           :open
           :formula-pierce
           ["formula is satisfiable"])
   (direct :formulas/contradiction
           "contradiction"
           "tests/test_formulas.txt:7"
           :closes
           :formula-contradiction
           ["formula is unsatisfiable"])
   (direct :formulas/modus-ponens-fail
           "modus-ponens-fail"
           "tests/test_formulas.txt:8"
           :closes
           :formula-modus-ponens-contradiction
           ["formula is unsatisfiable"])
   {:row (row {:id :formulas/distribution
               :source-file "tests/test_formulas.txt"
               :source-test "distribution"
               :coverage :analog
               :source-location "tests/test_formulas.txt:9"
               :source-expectation :open})
    :translation (translation
                   {:source-test "distribution"
                    :source-expectation :open
                    :source-location "tests/test_formulas.txt:9"
                    :proflog-mode :satisfiability
                    :proflog-expectation :open
                    :formula-key :formula-distribution
                    :retained-assertions ["formula is satisfiable"]
                    :dropped-assertions ["external parser formatting equivalence"]})}
   {:row (row {:id :formulas/peirce
               :source-file "tests/test_formulas.txt"
               :source-test "peirce"
               :coverage :direct
               :source-location "tests/test_formulas.txt:10"
               :source-expectation :closes})
    :translation (translation
                   {:source-test "peirce"
                    :source-expectation :closes
                    :source-location "tests/test_formulas.txt:10"
                    :proflog-mode :refutation
                    :proflog-expectation :closes
                    :refutation-formula-key :formula-peirce
                    :retained-assertions ["negation of Peirce's law closes"]
                    :dropped-assertions []})}])

(def unsupported-records
  (let [unsupported
        [["test_wk3_simple_atom" "tests/test_comprehensive.py" "tests/test_comprehensive.py:234"
          "Weak Kleene truth-value semantics are outside the classical Proflog kernel."]
         ["test_wk3_contradiction_satisfiable" "tests/test_comprehensive.py" "tests/test_comprehensive.py:246"
          "Weak Kleene satisfiable contradictions are outside the classical Proflog kernel."]
         ["test_wk3_truth_values" "tests/test_comprehensive.py" "tests/test_comprehensive.py:259"
          "Weak Kleene truth-value enumeration is outside the classical Proflog kernel."]
         ["test_wk3_model_evaluation" "tests/test_comprehensive.py" "tests/test_comprehensive.py:276"
          "Weak Kleene model evaluation is outside the classical Proflog kernel."]
         ["test_term_creation" "tests/test_comprehensive.py" "tests/test_comprehensive.py:301"
          "Upstream term-constructor API shape has no portable tableau assertion."]
         ["test_predicate_creation" "tests/test_comprehensive.py" "tests/test_comprehensive.py:315"
          "Upstream predicate-constructor API shape has no portable tableau assertion."]
         ["test_predicate_with_variables" "tests/test_comprehensive.py" "tests/test_comprehensive.py:330"
          "Upstream predicate variable API shape has no portable tableau assertion."]
         ["test_atom_backward_compatibility" "tests/test_comprehensive.py" "tests/test_comprehensive.py:347"
          "Upstream backward-compatible atom constructor is not a Proflog semantic obligation."]
         ["test_mode_detection" "tests/test_comprehensive.py" "tests/test_comprehensive.py:368"
          "Signed and weak-Kleene mode detection is not present in Proflog."]
         ["test_mode_aware_api" "tests/test_comprehensive.py" "tests/test_comprehensive.py:389"
          "Mode-aware upstream API is not present in Proflog."]
         ["test_mixed_mode_prevention" "tests/test_comprehensive.py" "tests/test_comprehensive.py:420"
          "Mixed-mode prevention is specific to the upstream signed/WK3 API."]
         ["test_priest_weak_kleene_conjunction_table" "tests/test_literature_examples.py" "tests/test_literature_examples.py:31"
          "Priest weak Kleene conjunction table requires non-classical semantics."]
         ["test_priest_excluded_middle_not_tautology" "tests/test_literature_examples.py" "tests/test_literature_examples.py:65"
          "Priest's excluded-middle result is non-classical and unsupported."]
         ["test_priest_contradiction_satisfiable_wk3" "tests/test_literature_examples.py" "tests/test_literature_examples.py:96"
          "Weak Kleene contradiction satisfiability is non-classical and unsupported."]
         ["test_priest_signed_tableau_rules" "tests/test_literature_examples.py" "tests/test_literature_examples.py:132"
          "Signed Priest tableau rules are not implemented by the classical kernel."]
         ["test_handbook_three_valued_tableaux" "tests/test_literature_examples.py" "tests/test_literature_examples.py:428"
          "Handbook three-valued tableau behavior is non-classical and unsupported."]
         ["test_three_valued_non_classical_behavior" "tests/test_literature_examples.py" "tests/test_literature_examples.py:526"
          "Three-valued self-implication behavior is non-classical and unsupported."]
         ["test_ferguson_epistemic_disjunction_example" "tests/test_literature_examples.py" "tests/test_literature_examples.py:595"
          "Ferguson epistemic weak-Kleene disjunction is unsupported."]
         ["test_ferguson_epistemic_contradiction_non_closure" "tests/test_literature_examples.py" "tests/test_literature_examples.py:612"
          "Ferguson non-closure for epistemic contradiction is unsupported."]
         ["test_ferguson_sign_duality_in_negation" "tests/test_literature_examples.py" "tests/test_literature_examples.py:651"
          "Ferguson signed negation duality rules are unsupported."]
         ["test_ferguson_restricted_quantifier_example" "tests/test_literature_examples.py" "tests/test_literature_examples.py:676"
          "Ferguson restricted quantifier rules are unsupported."]
         ["test_ferguson_universal_quantifier_uncertainty" "tests/test_literature_examples.py" "tests/test_literature_examples.py:713"
          "Ferguson quantifier uncertainty behavior is unsupported."]
         ["test_ferguson_mixed_epistemic_reasoning" "tests/test_literature_examples.py" "tests/test_literature_examples.py:747"
          "Ferguson mixed epistemic reasoning is unsupported."]
         ["test_ferguson_non_classical_tautology_behavior" "tests/test_literature_examples.py" "tests/test_literature_examples.py:782"
          "Ferguson non-classical tautology behavior is unsupported."]
         ["test_ferguson_comparison_with_classical_three_valued" "tests/test_literature_examples.py" "tests/test_literature_examples.py:804"
          "Ferguson comparison with classical three-valued logic is unsupported."]
         ["test_ferguson_epistemic_closure_conditions" "tests/test_literature_examples.py" "tests/test_literature_examples.py:821"
          "Ferguson epistemic closure conditions are unsupported."]]]
    (mapv (fn [[source-test source-file location reason]]
            {:row (row {:id (keyword (if (= source-file "tests/test_comprehensive.py")
                                       "comprehensive"
                                       "literature")
                                     source-test)
                        :source-file source-file
                        :source-test source-test
                        :coverage :unsupported
                        :source-location location
                        :source-expectation :unsupported
                        :unsupported-reason reason})})
          unsupported)))

(def all-records
  (vec (concat catalog-records
               literature-records
               performance-records
               formula-records
               unsupported-records)))

(def inventory-entries
  (mapv :row all-records))

(def translation-records
  (into {}
        (keep (fn [{:keys [translation]}]
                (when translation
                  [(:source-test translation) translation])))
        all-records))

(def reconciliation-entries
  (mapv (fn [{:keys [source-test unsupported-reason]}]
          {:source-test source-test
           :status :unsupported
           :reason unsupported-reason
           :resolution unsupported-reason})
        (filter #(= :unsupported (:coverage %)) inventory-entries)))

(def inventory-stats
  (into {}
        (map (fn [[coverage rows]] [coverage (count rows)]))
        (group-by :coverage inventory-entries)))

(defn entry-by-id
  [id-or-source-test]
  (some #(when (or (= id-or-source-test (:id %))
                   (= id-or-source-test (:source-test %)))
           %)
        inventory-entries))

(defn translation-for-source-test
  [source-test]
  (get translation-records source-test))

(defn runnable-entries
  []
  (filter #(not= :unsupported (:coverage %)) inventory-entries))

(defn formula-for-entry
  [{:keys [formula-key source-test]}]
  (or (some-> formula-key formula-for-key)
      (some-> source-test translation-for-source-test :formula-key formula-for-key)))

(defn- refutation-keys
  [record]
  (cond
    (:refutation-formula-key record)
    [(:refutation-formula-key record)]

    (:refutation-formula-keys record)
    (:refutation-formula-keys record)

    :else []))

(defn- formula-keys
  [record]
  (cond
    (:formula-key record)
    [(:formula-key record)]

    (:formula-keys record)
    (:formula-keys record)

    :else []))

(defn proflog-observation-for-translation
  [record]
  (let [open-checks (map (comp proflog-observation formula-for-key)
                         (formula-keys record))
        close-checks (map (comp proflog-observation refutation-formula-for-key)
                          (refutation-keys record))
        observations (concat open-checks close-checks)]
    (cond
      (empty? observations) nil
      (every? #{:closes} observations) :closes
      (every? #{:open} observations) :open
      (= :open (:proflog-expectation record)) (if (every? #{:open} observations) :open :mixed)
      :else (if (every? #{:closes} close-checks) :closes :mixed))))
