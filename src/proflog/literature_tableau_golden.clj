(ns proflog.literature-tableau-golden
  "Literature tableau golden suite seeded by the reviewed `bradleypallen/tableaux`
   corpus at commit `fa5a736`.

   Each inventory entry records upstream provenance, Proflog disposition, and
   the independently observed Proflog result. Unsupported upstream tests remain
   represented with explicit reasons rather than being silently omitted."
  (:require [proflog.ast :as ast]
            [proflog.kernel :as kernel]
            [proflog.proof :as proof]))

(def upstream-commit
  "fa5a736090465d0ddf35362a6271d4298d668d42")

(def valid-dispositions
  #{:direct :analog :performance :source-confirm :unsupported})

(def valid-reconciliation-statuses
  #{:resolved :deferred :unsupported :source-disputed})

(def coverage-kinds
  #{:closure :open-branch :alpha-beta :contradiction :branch-growth})

(defn- atom-lit
  [sym polarity]
  (if (= polarity :pos)
    (ast/pos-lit (ast/app-term sym))
    (ast/neg-lit (ast/app-term sym))))

(defn- and-form*
  [& forms]
  (reduce ast/and-form forms))

(defn- or-form*
  [& forms]
  (reduce ast/or-form forms))

(defn- implies*
  [left right]
  (ast/implies-form left right))

;; Small reusable classical formulas aligned with the upstream corpus.
(def formulas
  {:atom-p (atom-lit 'p :pos)
   :neg-p (atom-lit 'p :neg)
   :atom-q (atom-lit 'q :pos)
   :neg-q (atom-lit 'q :neg)
   :contradiction-basic (and-form* (atom-lit 'p :pos) (atom-lit 'p :neg))
   :contradiction-complex
   (and-form* (atom-lit 'p :pos)
              (atom-lit 'q :pos)
              (atom-lit 'p :neg)
              (atom-lit 'q :neg))
   :excluded-middle (or-form* (atom-lit 'p :pos) (atom-lit 'p :neg))
   :transitivity-tautology
   (implies* (and-form* (atom-lit 'p :pos) (atom-lit 'q :pos))
             (atom-lit 'p :pos))
   :material-implication-tautology
   (implies* (atom-lit 'p :pos) (atom-lit 'q :pos))
   :satisfiable-conjunction (and-form* (atom-lit 'p :pos) (atom-lit 'q :pos))
   :satisfiable-disjunction (or-form* (atom-lit 'p :pos) (atom-lit 'q :pos))
   :satisfiable-implication (implies* (atom-lit 'p :pos) (atom-lit 'q :pos))
   :complex-nested
   (and-form* (or-form* (atom-lit 'p :pos) (atom-lit 'q :neg))
              (implies* (atom-lit 'r :pos) (atom-lit 's :pos)))
   :de-morgan-left
   (and-form* (ast/not-form (and-form* (atom-lit 'p :pos) (atom-lit 'q :pos)))
              (or-form* (atom-lit 'p :neg) (atom-lit 'q :neg)))
   :multiple-consistent (and-form* (atom-lit 'p :pos) (atom-lit 'q :pos))
   :multiple-inconsistent
   (and-form* (atom-lit 'p :pos) (atom-lit 'q :pos) (atom-lit 'p :neg))
   :alpha-beta (and-form* (or-form* (atom-lit 'p :pos) (atom-lit 'q :pos))
                          (atom-lit 'p :neg))
   :fitting-closure (and-form* (atom-lit 'p :pos) (atom-lit 'p :neg))
   :fitting-open (atom-lit 'p :pos)
   :smullyan-alpha-beta (and-form* (or-form* (atom-lit 'p :pos) (atom-lit 'q :pos))
                                  (atom-lit 'r :neg))
   :smullyan-completeness (and-form* (atom-lit 'p :pos) (atom-lit 'p :neg))
   :ferguson-classical-contradiction (and-form* (atom-lit 'p :pos) (atom-lit 'p :neg))
   :empty-branch true
   :single-formula (atom-lit 'p :pos)
   :deep-nesting
   (and-form* (or-form* (or-form* (atom-lit 'p :pos) (atom-lit 'q :neg))
                        (atom-lit 'r :pos))
              (atom-lit 's :neg))
   :large-disjunction
   (or-form* (atom-lit 'p0 :pos) (atom-lit 'p1 :pos) (atom-lit 'p2 :pos)
             (atom-lit 'p3 :pos) (atom-lit 'p4 :pos))
   :large-conjunction
   (and-form* (atom-lit 'p0 :pos) (atom-lit 'p1 :pos) (atom-lit 'p2 :pos)
              (atom-lit 'p3 :pos) (atom-lit 'p4 :pos))
   :performance-complex
   (and-form* (or-form* (atom-lit 'p :pos) (atom-lit 'q :neg))
              (or-form* (atom-lit 'r :pos) (atom-lit 's :neg))
              (atom-lit 't :neg))
   :termination-check (and-form* (atom-lit 'p :pos) (atom-lit 'p :neg))
   :formula-p (atom-lit 'p :pos)
   :formula-pq (and-form* (atom-lit 'p :pos) (atom-lit 'q :pos))
   :formula-p-or-q (or-form* (atom-lit 'p :pos) (atom-lit 'q :pos))
   :formula-impl (implies* (atom-lit 'p :pos) (atom-lit 'q :pos))
   :formula-em (or-form* (atom-lit 'p :pos) (atom-lit 'p :neg))
   :formula-pierce (or-form* (implies* (atom-lit 'p :pos) (atom-lit 'q :pos))
                             (implies* (atom-lit 'q :pos) (atom-lit 'p :pos)))
   :formula-contradiction (and-form* (atom-lit 'p :pos) (atom-lit 'p :neg))
   :formula-modus-ponens-fail
   (and-form* (implies* (atom-lit 'p :pos) (atom-lit 'q :pos))
              (atom-lit 'p :pos)
              (atom-lit 'q :neg))
   :formula-distribution
   (and-form* (or-form* (atom-lit 'p :pos) (atom-lit 'q :pos))
              (or-form* (atom-lit 'p :neg) (atom-lit 'r :pos)))
   :formula-peirce (implies* (implies* (implies* (atom-lit 'p :pos) (atom-lit 'q :pos))
                                       (atom-lit 'p :pos))
                             (atom-lit 'p :pos))})

(defn inventory-entry
  "Construct a normalized inventory record."
  [{:keys [id source-file source-test disposition coverage suite formula-key
           upstream-result proflog-expectation unsupported-reason source-note]
    :or {suite :fast}}]
  (cond-> {:id id
           :source-file source-file
           :source-test source-test
           :disposition disposition
           :suite suite}
    coverage (assoc :coverage coverage)
    formula-key (assoc :formula-key formula-key)
    upstream-result (assoc :upstream-result upstream-result)
    proflog-expectation (assoc :proflog-expectation proflog-expectation)
    unsupported-reason (assoc :unsupported-reason unsupported-reason)
    source-note (assoc :source-note source-note)))

(def inventory-entries
  "Machine-readable inventory for every active upstream `tableaux` item."
  (vec
    (concat
      ;; Classical propositional logic — direct semantic analogs.
      [(inventory-entry
         {:id :comprehensive/test-simple-atom
          :source-file "tests/test_comprehensive.py"
          :source-test "test_simple_atom"
          :disposition :direct
          :coverage :open-branch
          :formula-key :atom-p
          :upstream-result :open
          :proflog-expectation :open})
       (inventory-entry
         {:id :comprehensive/test-simple-negation
          :source-file "tests/test_comprehensive.py"
          :source-test "test_simple_negation"
          :disposition :direct
          :coverage :open-branch
          :formula-key :neg-p
          :upstream-result :open
          :proflog-expectation :open})
       (inventory-entry
         {:id :comprehensive/test-contradiction-basic
          :source-file "tests/test_comprehensive.py"
          :source-test "test_contradiction_basic"
          :disposition :direct
          :coverage :contradiction
          :formula-key :contradiction-basic
          :upstream-result :closes
          :proflog-expectation :closes})
       (inventory-entry
         {:id :comprehensive/test-contradiction-complex
          :source-file "tests/test_comprehensive.py"
          :source-test "test_contradiction_complex"
          :disposition :direct
          :coverage :contradiction
          :formula-key :contradiction-complex
          :upstream-result :closes
          :proflog-expectation :closes})
       (inventory-entry
         {:id :comprehensive/test-tautology-excluded-middle
          :source-file "tests/test_comprehensive.py"
          :source-test "test_tautology_excluded_middle"
          :disposition :direct
          :coverage :open-branch
          :formula-key :excluded-middle
          :upstream-result :open
          :proflog-expectation :open})
       (inventory-entry
         {:id :comprehensive/test-tautology-transitivity
          :source-file "tests/test_comprehensive.py"
          :source-test "test_tautology_transitivity"
          :disposition :direct
          :formula-key :transitivity-tautology
          :upstream-result :open
          :proflog-expectation :open})
       (inventory-entry
         {:id :comprehensive/test-tautology-material-implication
          :source-file "tests/test_comprehensive.py"
          :source-test "test_tautology_material_implication"
          :disposition :direct
          :formula-key :material-implication-tautology
          :upstream-result :open
          :proflog-expectation :open})
       (inventory-entry
         {:id :comprehensive/test-satisfiable-conjunction
          :source-file "tests/test_comprehensive.py"
          :source-test "test_satisfiable_conjunction"
          :disposition :direct
          :formula-key :satisfiable-conjunction
          :upstream-result :open
          :proflog-expectation :open})
       (inventory-entry
         {:id :comprehensive/test-satisfiable-disjunction
          :source-file "tests/test_comprehensive.py"
          :source-test "test_satisfiable_disjunction"
          :disposition :direct
          :formula-key :satisfiable-disjunction
          :upstream-result :open
          :proflog-expectation :open})
       (inventory-entry
         {:id :comprehensive/test-satisfiable-implication
          :source-file "tests/test_comprehensive.py"
          :source-test "test_satisfiable_implication"
          :disposition :direct
          :formula-key :satisfiable-implication
          :upstream-result :open
          :proflog-expectation :open})
       (inventory-entry
         {:id :comprehensive/test-complex-nested-formula
          :source-file "tests/test_comprehensive.py"
          :source-test "test_complex_nested_formula"
          :disposition :direct
          :formula-key :complex-nested
          :upstream-result :open
          :proflog-expectation :open})
       (inventory-entry
         {:id :comprehensive/test-de-morgan-laws
          :source-file "tests/test_comprehensive.py"
          :source-test "test_de_morgan_laws"
          :disposition :direct
          :formula-key :de-morgan-left
          :upstream-result :open
          :proflog-expectation :open})
       (inventory-entry
         {:id :comprehensive/test-multiple-formulas-consistent
          :source-file "tests/test_comprehensive.py"
          :source-test "test_multiple_formulas_consistent"
          :disposition :direct
          :formula-key :multiple-consistent
          :upstream-result :open
          :proflog-expectation :open})
       (inventory-entry
         {:id :comprehensive/test-multiple-formulas-inconsistent
          :source-file "tests/test_comprehensive.py"
          :source-test "test_multiple_formulas_inconsistent"
          :disposition :direct
          :coverage :closure
          :formula-key :multiple-inconsistent
          :upstream-result :closes
          :proflog-expectation :closes})]
      ;; Weak Kleene — unsupported in classical Proflog kernel.
      (map (fn [test-name]
             (inventory-entry
               {:id (keyword "comprehensive" test-name)
                :source-file "tests/test_comprehensive.py"
                :source-test test-name
                :disposition :unsupported
                :upstream-result :non-classical
                :unsupported-reason
                "Proflog greenfield kernel is classical; no weak Kleene semantics profile."}))
           ["test_wk3_simple_atom"
            "test_wk3_contradiction_satisfiable"
            "test_wk3_truth_values"
            "test_wk3_model_evaluation"])
      ;; Predicate-syntax shape tests — upstream API tests, not semantic tableaux.
      (map (fn [test-name]
             (inventory-entry
               {:id (keyword "comprehensive" test-name)
                :source-file "tests/test_comprehensive.py"
                :source-test test-name
                :disposition :unsupported
                :upstream-result :api-shape
                :unsupported-reason
                "Upstream predicate/term constructor test; no portable Proflog tableau semantics."}))
           ["test_term_creation"
            "test_predicate_creation"
            "test_predicate_with_variables"
            "test_atom_backward_compatibility"])
      ;; Mode-aware signed tableaux — unsupported.
      (map (fn [test-name]
             (inventory-entry
               {:id (keyword "comprehensive" test-name)
                :source-file "tests/test_comprehensive.py"
                :source-test test-name
                :disposition :unsupported
                :upstream-result :signed-tableau
                :unsupported-reason
                "Proflog has no signed (T/F) tableau mode or mixed-mode prevention layer."}))
           ["test_mode_detection"
            "test_mode_aware_api"
            "test_mixed_mode_prevention"])
      ;; Optimization shape tests from comprehensive file.
      [       (inventory-entry
         {:id :comprehensive/test-formula-prioritization
          :source-file "tests/test_comprehensive.py"
          :source-test "test_formula_prioritization"
          :disposition :performance
          :coverage :branch-growth
          :suite :extended
          :formula-key :alpha-beta
          :upstream-result :optimization
          :proflog-expectation :open})
       (inventory-entry
         {:id :comprehensive/test-subsumption-elimination
          :source-file "tests/test_comprehensive.py"
          :source-test "test_subsumption_elimination"
          :disposition :performance
          :suite :extended
          :formula-key :contradiction-basic
          :upstream-result :optimization
          :proflog-expectation :closes})
       (inventory-entry
         {:id :comprehensive/test-early-satisfiability-detection
          :source-file "tests/test_comprehensive.py"
          :source-test "test_early_satisfiability_detection"
          :disposition :performance
          :suite :extended
          :formula-key :atom-p
          :upstream-result :optimization
          :proflog-expectation :open})
       (inventory-entry
         {:id :comprehensive/test-performance-complex-formula
          :source-file "tests/test_comprehensive.py"
          :source-test "test_performance_complex_formula"
          :disposition :performance
          :coverage :branch-growth
          :suite :extended
          :formula-key :performance-complex
          :upstream-result :optimization
          :proflog-expectation :open})
       (inventory-entry
         {:id :comprehensive/test-model-extraction-correctness
          :source-file "tests/test_comprehensive.py"
          :source-test "test_model_extraction_correctness"
          :disposition :analog
          :coverage :open-branch
          :formula-key :atom-p
          :upstream-result :open
          :proflog-expectation :open})]
      ;; Edge cases.
      [(inventory-entry
         {:id :comprehensive/test-empty-formula-list
          :source-file "tests/test_comprehensive.py"
          :source-test "test_empty_formula_list"
          :disposition :analog
          :formula-key :empty-branch
          :upstream-result :open
          :proflog-expectation :open})
       (inventory-entry
         {:id :comprehensive/test-single-formula-in-list
          :source-file "tests/test_comprehensive.py"
          :source-test "test_single_formula_in_list"
          :disposition :direct
          :formula-key :single-formula
          :upstream-result :open
          :proflog-expectation :open})
       (inventory-entry
         {:id :comprehensive/test-very-deep-nesting
          :source-file "tests/test_comprehensive.py"
          :source-test "test_very_deep_nesting"
          :disposition :performance
          :coverage :branch-growth
          :suite :extended
          :formula-key :deep-nesting
          :upstream-result :open
          :proflog-expectation :open})
       (inventory-entry
         {:id :comprehensive/test-large-disjunction
          :source-file "tests/test_comprehensive.py"
          :source-test "test_large_disjunction"
          :disposition :performance
          :coverage :branch-growth
          :suite :extended
          :formula-key :large-disjunction
          :upstream-result :open
          :proflog-expectation :open})
       (inventory-entry
         {:id :comprehensive/test-large-conjunction
          :source-file "tests/test_comprehensive.py"
          :source-test "test_large_conjunction"
          :disposition :performance
          :coverage :branch-growth
          :suite :extended
          :formula-key :large-conjunction
          :upstream-result :open
          :proflog-expectation :open})]
      ;; Literature — Priest (unsupported non-classical).
      (map (fn [test-name]
             (inventory-entry
               {:id (keyword "literature" test-name)
                :source-file "tests/test_literature_examples.py"
                :source-test test-name
                :disposition :unsupported
                :upstream-result :non-classical
                :unsupported-reason
                "Priest weak Kleene / signed tableau examples require non-classical semantics."
                :source-note "Graham Priest, An Introduction to Non-Classical Logic."}))
           ["test_priest_weak_kleene_conjunction_table"
            "test_priest_excluded_middle_not_tautology"
            "test_priest_contradiction_satisfiable_wk3"
            "test_priest_signed_tableau_rules"])
      ;; Fitting — source-confirm classical analogs.
      [(inventory-entry
         {:id :literature/test-fitting-basic-expansion-example
          :source-file "tests/test_literature_examples.py"
          :source-test "test_fitting_basic_expansion_example"
          :disposition :source-confirm
          :coverage :alpha-beta
          :formula-key :alpha-beta
          :upstream-result :open
          :proflog-expectation :open
          :source-note "Melvin Fitting, First-Order Logic and Automated Theorem Proving."})
       (inventory-entry
         {:id :literature/test-fitting-closure-example
          :source-file "tests/test_literature_examples.py"
          :source-test "test_fitting_closure_example"
          :disposition :source-confirm
          :coverage :closure
          :formula-key :fitting-closure
          :upstream-result :closes
          :proflog-expectation :closes
          :source-note "Melvin Fitting, First-Order Logic and Automated Theorem Proving."})
       (inventory-entry
         {:id :literature/test-fitting-satisfiable-example
          :source-file "tests/test_literature_examples.py"
          :source-test "test_fitting_satisfiable_example"
          :disposition :source-confirm
          :coverage :open-branch
          :formula-key :fitting-open
          :upstream-result :open
          :proflog-expectation :open
          :source-note "Melvin Fitting, First-Order Logic and Automated Theorem Proving."})]
      ;; Smullyan — source-confirm.
      [(inventory-entry
         {:id :literature/test-smullyan-alpha-beta-classification
          :source-file "tests/test_literature_examples.py"
          :source-test "test_smullyan_alpha_beta_classification"
          :disposition :source-confirm
          :coverage :alpha-beta
          :formula-key :smullyan-alpha-beta
          :upstream-result :open
          :proflog-expectation :open
          :source-note "Raymond M. Smullyan, First-Order Logic."})
       (inventory-entry
         {:id :literature/test-smullyan-systematic-tableau-construction
          :source-file "tests/test_literature_examples.py"
          :source-test "test_smullyan_systematic_tableau_construction"
          :disposition :source-confirm
          :coverage :alpha-beta
          :formula-key :alpha-beta
          :upstream-result :open
          :proflog-expectation :open
          :source-note "Raymond M. Smullyan, First-Order Logic."})
       (inventory-entry
         {:id :literature/test-smullyan-completeness-example
          :source-file "tests/test_literature_examples.py"
          :source-test "test_smullyan_completeness_example"
          :disposition :source-confirm
          :coverage :contradiction
          :formula-key :smullyan-completeness
          :upstream-result :closes
          :proflog-expectation :closes
          :source-note "Raymond M. Smullyan, First-Order Logic."})]
      ;; Handbook — unsupported signed/three-valued.
      (map (fn [test-name]
             (inventory-entry
               {:id (keyword "literature" test-name)
                :source-file "tests/test_literature_examples.py"
                :source-test test-name
                :disposition :unsupported
                :upstream-result :signed-or-three-valued
                :unsupported-reason
                "Handbook signed or three-valued tableau rules are outside the classical Proflog kernel."
                :source-note "D'Agostino/Gabbay/Hahnle/Posegga, Handbook of Tableau Methods."}))
           ["test_handbook_signed_semantic_tableaux"
            "test_handbook_three_valued_tableaux"
            "test_handbook_optimization_techniques"])
      ;; Literature edge cases.
      [(inventory-entry
         {:id :literature/test-deep-nesting-priest-example
          :source-file "tests/test_literature_examples.py"
          :source-test "test_deep_nesting_priest_example"
          :disposition :unsupported
          :upstream-result :non-classical
          :unsupported-reason "Priest non-classical deep nesting example."})
       (inventory-entry
         {:id :literature/test-three-valued-non-classical-behavior
          :source-file "tests/test_literature_examples.py"
          :source-test "test_three_valued_non_classical_behavior"
          :disposition :unsupported
          :upstream-result :non-classical
          :unsupported-reason "Three-valued behavior is unsupported in classical Proflog."})
       (inventory-entry
         {:id :literature/test-fitting-branch-bound-example
          :source-file "tests/test_literature_examples.py"
          :source-test "test_fitting_branch_bound_example"
          :disposition :performance
          :coverage :branch-growth
          :suite :extended
          :formula-key :deep-nesting
          :upstream-result :open
          :proflog-expectation :open
          :source-note "Melvin Fitting branch-bound discussion."})]
      ;; Ferguson wKrQ — mostly unsupported; one classical analog retained.
      (map (fn [[test-name reason]]
             (inventory-entry
               {:id (keyword "literature" test-name)
                :source-file "tests/test_literature_examples.py"
                :source-test test-name
                :disposition :unsupported
                :upstream-result :wkqr
                :unsupported-reason reason
                :source-note "T. M. Ferguson, weak Kleene restricted quantification."}))
           [["test_ferguson_epistemic_disjunction_example"
             "Ferguson epistemic weak Kleene disjunction is unsupported."]
            ["test_ferguson_epistemic_contradiction_non_closure"
             "Non-closure under epistemic weak Kleene is unsupported."]
            ["test_ferguson_sign_duality_in_negation"
             "Signed duality rules are unsupported."]
            ["test_ferguson_restricted_quantifier_example"
             "Restricted quantifier epistemic logic is unsupported."]
            ["test_ferguson_universal_quantifier_uncertainty"
             "Universal quantifier uncertainty fragment is unsupported."]
            ["test_ferguson_mixed_epistemic_reasoning"
             "Mixed epistemic reasoning is unsupported."]
            ["test_ferguson_non_classical_tautology_behavior"
             "Non-classical tautology behavior is unsupported."]
            ["test_ferguson_comparison_with_classical_three_valued"
             "Three-valued comparison is unsupported."]
            ["test_ferguson_epistemic_closure_conditions"
             "Epistemic closure conditions are unsupported."]])
      [(inventory-entry
         {:id :literature/test-ferguson-classical-contradiction-still-works
          :source-file "tests/test_literature_examples.py"
          :source-test "test_ferguson_classical_contradiction_still_works"
          :disposition :analog
          :coverage :contradiction
          :formula-key :ferguson-classical-contradiction
          :upstream-result :closes
          :proflog-expectation :closes
          :source-note "Classical contradiction behavior within Ferguson suite."})]
      ;; Performance file.
      [       (inventory-entry
         {:id :performance/test-prioritization-benefit
          :source-file "tests/test_performance.py"
          :source-test "test_prioritization_benefit"
          :disposition :performance
          :coverage :branch-growth
          :suite :extended
          :formula-key :alpha-beta
          :upstream-result :optimization
          :proflog-expectation :open})
       (inventory-entry
         {:id :performance/test-subsumption-benefit
          :source-file "tests/test_performance.py"
          :source-test "test_subsumption_benefit"
          :disposition :performance
          :suite :extended
          :formula-key :contradiction-basic
          :upstream-result :optimization
          :proflog-expectation :closes})
       (inventory-entry
         {:id :performance/test-complex-formula-performance
          :source-file "tests/test_performance.py"
          :source-test "test_complex_formula_performance"
          :disposition :performance
          :coverage :branch-growth
          :suite :extended
          :formula-key :performance-complex
          :upstream-result :optimization
          :proflog-expectation :open})
       (inventory-entry
         {:id :performance/test-termination-correctness
          :source-file "tests/test_performance.py"
          :source-test "test_termination_correctness"
          :disposition :performance
          :suite :extended
          :formula-key :termination-check
          :upstream-result :closes
          :proflog-expectation :closes})]
      ;; Setup smoke — non-portable signed tableau build.
      [(inventory-entry
         {:id :setup/signed-tableau-smoke
          :source-file "tests/test_setup.py"
          :source-test "setup_smoke"
          :disposition :unsupported
          :upstream-result :signed-tableau
          :unsupported-reason
          "Upstream smoke builds signed T:p tableaux; Proflog uses unsigned classical literals."})]
      ;; CLI formula list — analog classical formulas.
      (map (fn [[slug formula-key expectation]]
             (inventory-entry
               {:id (keyword "formulas" slug)
                :source-file "tests/test_formulas.txt"
                :source-test slug
                :disposition :analog
                :formula-key formula-key
                :upstream-result expectation
                :proflog-expectation expectation}))
           [["p" :formula-p :open]
            ["p-and-q" :formula-pq :open]
            ["p-or-q" :formula-p-or-q :open]
            ["p-impl-q" :formula-impl :open]
            ["excluded-middle" :formula-em :open]
            ["pierce-law" :formula-pierce :open]
            ["contradiction" :formula-contradiction :closes]
            ["modus-ponens-fail" :formula-modus-ponens-fail :open]
            ["distribution" :formula-distribution :open]
            ["peirce" :formula-peirce :open]]))))

(def reconciliation-entries
  "Recorded disagreements or deferred items among upstream, source, and Proflog."
  [{:source-test "test_wk3_contradiction_satisfiable"
    :upstream-result :open
    :external-expectation :open-under-wk3
    :proflog-observation :unsupported
    :status :unsupported
    :resolution
    "Classical Proflog cannot reproduce weak Kleene satisfiable contradictions; entry stays unsupported."
    :evidence "inventory disposition :unsupported for comprehensive/test_wk3_contradiction_satisfiable"}
   {:source-test "test_priest_excluded_middle_not_tautology"
    :upstream-result :open-under-wk3
    :external-expectation :not-tautology-in-priest-logic
    :proflog-observation :open-classical-tautology-refutation-fails
    :status :source-disputed
    :resolution
    "Proflog classical kernel treats excluded middle as refutation-open; Priest expectation deferred to unsupported non-classical entries."
    :evidence "literature/test_priest_excluded_middle_not_tautology marked unsupported"}
   {:source-test "test_formula_prioritization"
    :upstream-result :optimization-closes-faster
    :external-expectation :alpha-before-beta-heuristic
    :proflog-observation :open
    :status :resolved
    :resolution
    "Proflog records the observed semantic result (:open) for the alpha-beta fixture; upstream optimization timing is tracked separately in ADR-0115."
    :evidence "comprehensive/test-formula-prioritization proflog-expectation :open"}
   {:source-test "test_setup.py smoke"
    :upstream-result :signed-build-success
    :external-expectation :signed-T-p-branch
    :proflog-observation :unsupported
    :status :unsupported
    :resolution
    "Signed tableau smoke is non-portable; classical analog covered by formula-p CLI entry."
    :evidence "setup/signed-tableau-smoke and formulas/p"}])

(defn entry-by-id
  [id]
  (some #(when (= id (:id %)) %) inventory-entries))

(defn runnable-entries
  "Inventory entries with a Proflog formula and semantic expectation."
  []
  (filter (fn [{:keys [disposition formula-key proflog-expectation]}]
            (and formula-key
                 proflog-expectation
                 (not= disposition :unsupported)))
          inventory-entries))

(defn formula-for-entry
  [{:keys [formula-key]}]
  (get formulas formula-key))

(defn proflog-observation
  "Independently observe whether Proflog closes `formula`."
  [formula]
  (if (seq (kernel/prove formula 1))
    :closes
    :open))

(defn prove-with-steps
  "Return closure observation and proof-step count for envelope tests."
  [formula]
  (let [proofs (kernel/prove formula 1)]
    {:observation (if (seq proofs) :closes :open)
     :step-count (if (seq proofs)
                   (count (proof/collect-steps (first proofs)))
                   0)}))

(defn inventory-stats
  []
  (let [by-disposition (group-by :disposition inventory-entries)]
    {:total (count inventory-entries)
     :by-disposition (into {} (map (fn [[k v]] [k (count v)]) by-disposition))
     :runnable (count (runnable-entries))
     :unsupported (count (:unsupported by-disposition))
     :reconciliations (count reconciliation-entries)}))
