(ns proflog.sjas-boundary-synthesis-probe
  "Bounded positive-SelfCons proof diagnostics from ADR-0139.

   This namespace sits at the proof-search diagnostic boundary. It asks the SJAS
   proof predicate to synthesize a proof code for an already-generated
   Workstream B SelfCons target, then routes that candidate through the cheap
   correspondence screen. ADR-0140 established that these positive Group-3
   proofs are never final Workstream B evidence; a report records only what the
   legacy proof-code search produced and why it does not qualify."
  (:require [clojure.pprint :as pp]
            [clojure.string :as str]
            [proflog.answers :as answers]
            [proflog.ast :as ast]
            [proflog.sjas-correspondence :as correspondence]
            [proflog.willard-sjas :as sjas]
            [proflog.willard-sjas-code :as sjas-code]))

(def ^:private default-query-opts
  "Default bounded search envelope for interactive ADR-0139 probes.

   Long-running synthesis probes should override these values and run through a
   detachable shell command that writes stdout, stderr, and timing output under
   `test-runs/`. The helper records the durable log path supplied by that
   caller, but it deliberately does not manage process supervision itself."
  {:fuel 320
   :proof-limit 1
   :defer-calls? false})

(defn- tab2-validation-system
  "Build the ordinary validation program used for Tab-2 target proof codes.

   ADR-0138 keeps the checked target system as `:willard-sjas-tab2-boundary`,
   while proof-code validation runs through the ordinary tableau profile with
   the Tab-2 relation present in the language. The synthesis probe follows the
   same split so synthesized candidates exercise the same validation surface."
  [system-opts]
  (sjas/system
    (assoc system-opts
           :profile :willard-sjas-tableau0
           :relations (merge (:relations system-opts)
                             sjas/tab2-boundary-relations))))

(def ^:private variant-specs
  {:total-multiplication
   {:target-report sjas/total-multiplication-full-target-report
    :query-system sjas/total-multiplication-reduced-witness-system
    :validation sjas/total-multiplication-constructed-certificate-validation
    :default-system-opts {:profile :willard-sjas-level1
                          :depth 3}
    :target-report-helper 'total-multiplication-full-target-report
    :validation-helper 'total-multiplication-constructed-certificate-validation}

   :xtab-or-lem-axiom
   {:target-report sjas/xtab-lem-full-target-report
    :query-system sjas/xtab-lem-reduced-witness-system
    :validation sjas/xtab-lem-constructed-certificate-validation
    :default-system-opts {:profile :willard-sjas-level1}
    :target-report-helper 'xtab-lem-full-target-report
    :validation-helper 'xtab-lem-constructed-certificate-validation}

   :tab-2-or-stronger
   {:target-report sjas/tab2-or-stronger-full-target-report
    :query-system tab2-validation-system
    :validation sjas/tab2-or-stronger-constructed-certificate-validation
    :default-system-opts {}
    :target-report-helper 'tab2-or-stronger-full-target-report
    :validation-helper 'tab2-or-stronger-constructed-certificate-validation}})

(defn boundary-proof-search-synthesis-variants
  "Return the Workstream B variants supported by the ADR-0139 synthesis probe."
  []
  (set (keys variant-specs)))

(defn- variant-spec
  [variant]
  (or (get variant-specs variant)
      (throw (ex-info "Unknown Workstream B synthesis variant"
                      {:variant variant
                       :supported (boundary-proof-search-synthesis-variants)}))))

(defn- merged-system-opts
  [spec opts]
  (merge (:default-system-opts spec)
         (:system-opts opts)))

(defn boundary-proof-search-synthesis-plan
  "Describe the legacy positive-SelfCons diagnostic without running search."
  ([variant]
   (boundary-proof-search-synthesis-plan variant {}))
  ([variant opts]
   (let [spec (variant-spec variant)
         system-opts (merged-system-opts spec opts)]
     {:variant variant
      :evidence-kind :proof-search-synthesis
      :probe-kind :positive-selfcons-proof-diagnostic
      :validation-kind :legacy-positive-selfcons-proof
      :final-evidence-eligible? false
      :counterexample-synthesis-status :not-implemented
      :target-report-helper (:target-report-helper spec)
      :validation-helper (:validation-helper spec)
      :system-opts system-opts
      :query-opts (merge default-query-opts
                         (select-keys opts [:fuel
                                            :proof-limit
                                            :defer-calls?]))
      :proof-search-synthesis-status :open
      :durable-log-required? true
      :remaining-obligations correspondence/boundary-final-evidence-obligations})))

(defn- first-binding
  [records binding-nom]
  (some #(answers/binding-term % binding-nom) records))

(defn- synthesis-result
  "Return either a supplied durable-probe result or a live proof-search result."
  [query-system target query-opts proof-code-var opts]
  (if-let [proof-code (:synthesized-proof-code opts)]
    {:status :provided
     :records [{:bindings [[proof-code-var proof-code]]
                :residuals '()}]
     :proof-code proof-code}
    (let [records (doall
                    (sjas/query-answers
                      query-system
                      (sjas/tableau-proof (:system-code target)
                                          (:group-three-code target)
                                          (ast/var-term proof-code-var))
                      [proof-code-var]
                      query-opts))]
      {:status (if (seq records) :found :not-found)
       :records records
       :proof-code (first-binding records proof-code-var)})))

(defn- proof-code-certificate-kind
  "Classify a public proof-code term without running target proof validation."
  [proof-code]
  (let [decoded-proof (try
                        (sjas-code/proof-formal-code-term->proof proof-code)
                        (catch Exception _
                          nil))]
    (cond
      (= 'sjas-axiom decoded-proof) :sjas-axiom
      decoded-proof :structural-tableau
      :else :unreadable-proof-code)))

(defn- evidence-candidate
  [variant target proof-code certificate-kind durable-log-path]
  {:variant variant
   :evidence-kind :proof-search-synthesis
   :system-code (:system-code target)
   :selfcons-code (:group-three-code target)
   :theorem-code (:group-three-code target)
   :target-formula (:selfcons-refutation-target target)
   :target-code (:target-code target)
   :proof-code proof-code
   :certificate-kind certificate-kind
   :durable-log-path durable-log-path})

(defn- result-classification
  [screen]
  (cond
    (contains? (:reasons screen) :ordinary-selfcons-citation)
    :ordinary-citation-rejected

    (= :rejected (:result screen))
    :screen-rejected

    :else
    :verification-required))

(defn boundary-proof-search-synthesis-report
  "Run a bounded positive-SelfCons proof-code diagnostic for one variant.

   The report asks `tableau-proof/3` to synthesize a proof-code term for the
   generated Group-3 SelfCons theorem of `variant`. The first synthesized proof
   code is classified, optionally validated with `:validate-proof? true`, and
   screened as a `:proof-search-synthesis` candidate. Ordinary `sjas-axiom`
   citation is reported as found but rejected; it closes no ADR-0119
   Workstream B obligations."
  ([variant]
   (boundary-proof-search-synthesis-report variant {}))
  ([variant opts]
   (let [spec (variant-spec variant)
         system-opts (merged-system-opts spec opts)
         query-opts (merge default-query-opts
                           (select-keys opts [:fuel
                                              :proof-limit
                                              :defer-calls?]))
         target ((:target-report spec) system-opts)
         query-system ((:query-system spec) system-opts)]
     (ast/nom proof-code-var
       (let [{:keys [status records proof-code]}
             (synthesis-result query-system
                               target
                               query-opts
                               proof-code-var
                               opts)]
         (if-not proof-code
           {:variant variant
            :evidence-kind :proof-search-synthesis
            :probe-kind :positive-selfcons-proof-diagnostic
            :final-evidence-eligible? false
            :counterexample-synthesis-status :not-implemented
            :target target
            :query-opts query-opts
            :synthesis-status status
            :result :nontrivial-not-found
            :proof-count 0
            :completed-obligations #{}
            :remaining-obligations
            correspondence/boundary-final-evidence-obligations}
           (let [validation (when (:validate-proof? opts)
                              ((:validation spec)
                               proof-code
                               (merge system-opts
                                      (select-keys query-opts [:fuel
                                                              :proof-limit]))))
                 certificate-kind (or (:certificate-kind validation)
                                      (proof-code-certificate-kind proof-code))
                 candidate (evidence-candidate
                             variant
                             target
                             proof-code
                             certificate-kind
                             (:durable-log-path opts))
                 screen (correspondence/screen-boundary-evidence target
                                                                  candidate)]
             {:variant variant
              :evidence-kind :proof-search-synthesis
              :probe-kind :positive-selfcons-proof-diagnostic
              :final-evidence-eligible? false
              :counterexample-synthesis-status :not-implemented
              :target target
              :query-opts query-opts
              :synthesis-status status
              :result (result-classification screen)
              :proof-count (count records)
              :candidate candidate
              :screen screen
              :validation validation
              :validation-status (if validation :checked :not-run)
              :completed-obligations #{}
              :remaining-obligations
              correspondence/boundary-final-evidence-obligations})))))))

(defn- parse-variant
  [text]
  (keyword (str/replace text #"^:" "")))

(defn -main
  "Print ADR-0139 synthesis reports for selected variants.

   The CLI intentionally uses the default bounded envelope. For durable runs,
   invoke this alias through the project's `test-runs/` logging pattern and
   pass the log path to programmatic callers that screen evidence candidates."
  [& variant-texts]
  (if (seq variant-texts)
    (pp/pprint
      (mapv boundary-proof-search-synthesis-report
            (map parse-variant variant-texts)))
    (pp/pprint
      {:usage "lein probe-proflog-sjas-boundary-synthesis <variant>..."
       :durable-log-required? true
       :variants (sort (boundary-proof-search-synthesis-variants))
       :plans (mapv boundary-proof-search-synthesis-plan
                    (sort (boundary-proof-search-synthesis-variants)))})))
