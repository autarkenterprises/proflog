(ns proflog.literature-tableau-golden-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.literature-tableau-golden :as golden]))

(def required-source-tests
  "Every active upstream test name from the reviewed `tableaux` inventory."
  #{"test_simple_atom"
    "test_simple_negation"
    "test_contradiction_basic"
    "test_contradiction_complex"
    "test_tautology_excluded_middle"
    "test_tautology_transitivity"
    "test_tautology_material_implication"
    "test_satisfiable_conjunction"
    "test_satisfiable_disjunction"
    "test_satisfiable_implication"
    "test_complex_nested_formula"
    "test_de_morgan_laws"
    "test_multiple_formulas_consistent"
    "test_multiple_formulas_inconsistent"
    "test_wk3_simple_atom"
    "test_wk3_contradiction_satisfiable"
    "test_wk3_truth_values"
    "test_wk3_model_evaluation"
    "test_term_creation"
    "test_predicate_creation"
    "test_predicate_with_variables"
    "test_atom_backward_compatibility"
    "test_mode_detection"
    "test_mode_aware_api"
    "test_mixed_mode_prevention"
    "test_formula_prioritization"
    "test_subsumption_elimination"
    "test_early_satisfiability_detection"
    "test_performance_complex_formula"
    "test_model_extraction_correctness"
    "test_empty_formula_list"
    "test_single_formula_in_list"
    "test_very_deep_nesting"
    "test_large_disjunction"
    "test_large_conjunction"
    "test_priest_weak_kleene_conjunction_table"
    "test_priest_excluded_middle_not_tautology"
    "test_priest_contradiction_satisfiable_wk3"
    "test_priest_signed_tableau_rules"
    "test_fitting_basic_expansion_example"
    "test_fitting_closure_example"
    "test_fitting_satisfiable_example"
    "test_smullyan_alpha_beta_classification"
    "test_smullyan_systematic_tableau_construction"
    "test_smullyan_completeness_example"
    "test_handbook_signed_semantic_tableaux"
    "test_handbook_three_valued_tableaux"
    "test_handbook_optimization_techniques"
    "test_deep_nesting_priest_example"
    "test_three_valued_non_classical_behavior"
    "test_fitting_branch_bound_example"
    "test_ferguson_epistemic_disjunction_example"
    "test_ferguson_epistemic_contradiction_non_closure"
    "test_ferguson_classical_contradiction_still_works"
    "test_ferguson_sign_duality_in_negation"
    "test_ferguson_restricted_quantifier_example"
    "test_ferguson_universal_quantifier_uncertainty"
    "test_ferguson_mixed_epistemic_reasoning"
    "test_ferguson_non_classical_tautology_behavior"
    "test_ferguson_comparison_with_classical_three_valued"
    "test_ferguson_epistemic_closure_conditions"
    "test_prioritization_benefit"
    "test_subsumption_benefit"
    "test_complex_formula_performance"
    "test_termination_correctness"
    "setup_smoke"
    "p"
    "p-and-q"
    "p-or-q"
    "p-impl-q"
    "excluded-middle"
    "pierce-law"
    "contradiction"
    "modus-ponens-fail"
    "distribution"
    "peirce"})

(deftest inventory-covers-reviewed-upstream-corpus
  (testing "every active upstream item is represented with a final disposition"
    (let [catalog-tests (set (map :source-test golden/inventory-entries))
          stats (golden/inventory-stats)]
      (is (= 76 (:total stats)))
      (is (= required-source-tests catalog-tests))
      (doseq [entry golden/inventory-entries]
        (is (contains? golden/valid-dispositions (:disposition entry))
            (str "invalid disposition on " (:id entry)))
        (is (not= :pending (:disposition entry))
            (str "pending disposition on " (:id entry)))))))

(deftest minimum-coverage-kinds-are-present
  (testing "ADR-0112 requires closure, open, alpha/beta, contradiction, and branch-growth examples"
    (let [kinds (set (keep :coverage golden/inventory-entries))]
      (is (= golden/coverage-kinds kinds)))))

(deftest reconciliation-ledger-records-disagreements
  (testing "structured reconciliation entries exist for non-portable or disputed cases"
    (is (= 4 (count golden/reconciliation-entries)))
    (doseq [entry golden/reconciliation-entries]
      (is (contains? golden/valid-reconciliation-statuses (:status entry)))
      (is (string? (:resolution entry)))
      (is (string? (:evidence entry))))))

(deftest runnable-golden-cases-match-proflog-observations
  (testing "supported inventory entries are independently confirmed through Proflog"
    (doseq [{:keys [id proflog-expectation] :as entry}
            (golden/runnable-entries)
            :let [formula (golden/formula-for-entry entry)
                  observed (golden/proflog-observation formula)]]
      (is (= proflog-expectation observed)
          (str "golden mismatch on " id)))))

(deftest ^:slow literature-tableau-extended-branch-growth-envelopes
  (testing "performance-disposition entries stay within modest proof-step envelopes"
    (doseq [{:keys [id suite formula-key proflog-expectation] :as entry} golden/inventory-entries
            :when (and (= :extended suite)
                       formula-key
                       proflog-expectation)
            :let [{:keys [observation step-count]}
                  (golden/prove-with-steps (golden/formula-for-entry entry))]]
      (is (= proflog-expectation observation) (str "semantic mismatch on " id))
      (is (<= step-count 64)
          (str "branch-growth envelope exceeded on " id)))))

(deftest unsupported-entries-document-reasons
  (testing "unsupported upstream tests record explicit non-portability reasons"
    (doseq [{:keys [id unsupported-reason disposition]} golden/inventory-entries
            :when (= :unsupported disposition)]
      (is (string? unsupported-reason) (str "missing unsupported reason on " id))
      (is (pos? (count unsupported-reason)) (str "empty unsupported reason on " id)))))
