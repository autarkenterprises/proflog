(ns proflog.sjas-correspondence
  "Executable audit helpers for the ADR-0073 SJAS correspondence program.

   These helpers do not participate in proof search. Their job is to keep the
   encoded SJAS proof-certificate symbols, Track 2a classifications, structural
   proof-tree audits, and direct-examination correspondence proof obligations
   executable enough that tests fail when the proof boundary changes without an
   explicit recorded argument."
  (:require [clojure.set :as set]
            [proflog.proof :as proof]
            [proflog.willard-sjas-code :as sjas-code]))

(def ^:private relevant-tableau-symbols
  "Proof constructors whose tree/closure structure is part of the current
   relevant-intensional hypothesis for semantic-tableau SJAS."
  '#{conj
     split
     univ
     once-univ
     witness
     close
     atom-close
     occurs-close
     free-close
     decompose
     args
     refl-close
     neq-rigid
     neq-store
     neq-close
     false-close
     arith-close
     savefml
     skip-true})

(def ^:private relevant-sjas-coding-symbols
  "Proof constructors that expose object-code, axiom-membership, or arithmetic
   decoding work. These are relevant because ADR-0073 must not allow the bridge
   to collapse inspectable Godel-code structure into an opaque host witness."
  '#{sjas-system-beta-axiom
     willard-sjas-code
     sjas-system-code-bytes
     sjas-system-reflected-axiom
     sjas-system-group-zero-axiom
     sjas-system-group-one-axiom
     sjas-system-code-header
     sjas-system-fixed-axiom
     sjas-system-group-three-axiom
     sjas-system-tableau0-group-three-axiom
     sjas-system-level1-group-three-axiom
     sjas-code-bytes
     sjas-ug-code-bytes
     sjas-ug-code-byte-cons
     sjas-ug-code-cons
     sjas-ug-code-end
     sjas-ug-code-canonical-byte
     sjas-ug-code-mul64-shift
     sjas-ug-code-mul64-zero
     sjas-bind-done
     sjas-bind-num
     sjas-equal
     sjas-eq-progress
     sjas-leq
     sjas-lt
     sjas-mult
     sjas-num-add-one
     sjas-num-dbl
     sjas-num-one
     sjas-num-zero
     sjas-read-add
     sjas-read-count
     sjas-read-dbl
     sjas-read-div
     sjas-read-log
     sjas-read-max
     sjas-read-one
     sjas-read-pred
     sjas-read-root
     sjas-read-sub
     sjas-read-var
     sjas-read-zero
     sjas-code-arg
     sjas-code-args-end
     sjas-neg-pair-structural
     wff
	     delta-star-0-code
	     pi-star-1-code
	     sigma-star-1-code
	     neg-pair
	     sjas-axiom
	     dsjas-tableau-proof-object
	     dsjas-subst-prf-object
     tab1-proof-list-object
     dsjas-tab1-proof-object})

(def ^:private relevant-equality-symbols
  "Equality, disequality, and equality-triggered proof constructors consumed by
   the SJAS object-level proof checker."
  '#{eq-step
     eq-triggered-call
     eq-triggered-neg-call
     eq-refl
     eq-bind
     par-bind})

(def ^:private relevant-procedure-symbols
  "Proof constructors introduced by Proflog procedure-call and guarded-call
   machinery that are now checked by the SJAS object-level proof checker from
   reflected system-code, explicit guarded partitions, and proof-code trees."
  '#{pos-call
     neg-call
     neg-call-alt
     neg-call-guarded-alt
     alt
     guarded-alt
     guarded-neg-alt
     guarded-neg-alt-saturated
     guarded-seq-step
     guarded-seq-last
     guarded-call-seq-step
     guarded-residual-seq-step
     guarded-residual-seq-last
     guarded-scope-exists
     guarded-scope-done
     guarded-seq-done
     guarded-call-seq-done
     guarded-residual-seq-done
     guard-saturation-done
     guard-eq})

(def ^:private relevant-sjas-profile-symbols
  "SJAS profile-layer markers that remain visible in checked proof evidence.

   These are not host registries. They name the object-level proof relation
   that produced or consumed the enclosed evidence: arithmetic/code closure,
   axiom membership, theorem-code reading, proof-predicate checking, or
   substitution checking. Outer Tableau-0/Level-1 profile annotations are
   erased by public certificate construction, but public proof evidence still
   carries them as explicit selected-profile markers."
  '#{profiled
     willard-sjas-tableau0
     willard-sjas-level1
     willard-sjas-tab1
     willard-sjas-arithmetic
     willard-sjas-axiom-member
     willard-sjas-theorem-code
     willard-sjas-proof-check
     willard-sjas-subst-code
     willard-sjas-subst-source-result
     willard-sjas-subst-exprf
     willard-sjas-subst-proof-check
     willard-sjas-tab1-proof-check})

(def ^:private excluded-answer-overlay-symbols
  "Answer-export proof constructors deliberately not admitted by SJAS theorem
   proof predicates. They describe query entry and residual deferral behavior,
   not semantic-tableau proof checking for encoded theorem/proof pairs."
  '#{guarded-call-seq-defer
     query-pos-call
     query-neg-call
     query-neg-call-guarded-alt})

(def ^:private excluded-layer-symbols
  "Generic optimized sidecar constructors deliberately not admitted by the SJAS
   proof predicate. They remain encodable so legacy/public proof evidence can be
   inspected, but Track 1 treats them as outside the SJAS proof-code fragment."
  '#{lem-close
     skolemized
     propositional
     first-order
     willard-sjas-fact
     sjas-generated-axiom-member})

(defn- classify-set
  "Attach the same classification record to each symbol in `symbols`."
  [symbols classification]
  (into {}
        (map (fn [sym] [sym classification]))
        symbols))

(def proof-symbol-classifications
  "Current Track 2a classification for symbols that can appear in encoded SJAS
   proof certificates.

   `:status` is deliberately coarse:
   - `:relevant` means Track 2b must preserve this proof-object feature.
   - `:excluded` means Track 1 rejects this feature from SJAS proof-predicate
     certificates instead of carrying it into correspondence.
   - `:unresolved` means the feature may be sound, but its relevance or allowed
     expansion has not yet been proven.

   More precise proof obligations live in the `:obligation` field so tests and
   diagnostics can point at the reason a bridge remains open."
  (merge
    (classify-set relevant-tableau-symbols
                  {:status :relevant
                   :aspect :tableau-tree-structure
                   :obligation "Preserve finite tableau tree structure, branch continuation, and closure."})
    (classify-set relevant-sjas-coding-symbols
                  {:status :relevant
                   :aspect :sjas-code-and-arithmetic-structure
                   :obligation "Preserve inspectable formula/system/proof code decoding and arithmetic witnesses."})
    (classify-set relevant-equality-symbols
                  {:status :relevant
                   :aspect :equality-extension
                   :obligation "Preserve equality/disequality proof-state updates and equality-triggered saved-call closure."})
    (classify-set relevant-procedure-symbols
                  {:status :relevant
                   :aspect :procedure-call-expansion
                   :obligation "Preserve reflected procedure-call and guarded-alternative proof-tree structure."})
    (classify-set relevant-sjas-profile-symbols
                  {:status :relevant
                   :aspect :sjas-profile-proof-evidence
                   :obligation "Preserve explicit SJAS profile, code, axiom-membership, theorem-code, and proof-predicate evidence markers."})
    (classify-set excluded-answer-overlay-symbols
                  {:status :excluded
                   :aspect :answer-overlay
                   :obligation "Reject query-entry and residual-deferral proof constructors from SJAS proof-predicate certificates."})
    (classify-set excluded-layer-symbols
                  {:status :excluded
                   :aspect :generic-sidecar
                   :obligation "Reject generic optimized sidecar and obsolete generated-host closure from SJAS proof-predicate certificates."})))

(def profile-form-classifications
  "Path-sensitive classifications for concrete `(profiled kind subproof)` forms.

   The bare symbol `profiled` is intentionally conservative in
   `proof-symbol-classifications`, because its relevance depends on the second
   field. This map records the current Track 2a refinement for that second
   field."
  {'willard-sjas-tableau0
   {:status :probably-irrelevant
    :aspect :sjas-profile-annotation
    :obligation "Prove wrapper erasure and prove encoded system/profile selection fixes this tag."}

   'willard-sjas-level1
   {:status :probably-irrelevant
    :aspect :sjas-profile-annotation
    :obligation "Prove wrapper erasure and include Level-1 substitution vocabulary in profile-selection invariants."}

   'willard-sjas-tab1
   {:status :probably-irrelevant
    :aspect :sjas-profile-annotation
    :obligation "Prove wrapper erasure and include Tab-1 proof-list vocabulary in profile-selection invariants."}

   'willard-sjas-arithmetic
   {:status :relevant
    :aspect :sjas-arithmetic-closure
    :obligation "Preserve the wrapped arithmetic/equality relation proof as object-language work."}

   'willard-sjas-code
   {:status :relevant
    :aspect :sjas-code-closure
    :obligation "Preserve the wrapped syntax/code-reading evidence and byte-structure proof."}

   'willard-sjas-proof-check
   {:status :relevant
    :aspect :sjas-tableau-proof-predicate
    :obligation "Account for the wrapped tableau-proof checking relation or replace it with object-level proof-tree checking."}

   'willard-sjas-subst-proof-check
   {:status :relevant
    :aspect :sjas-substitution-proof-predicate
    :obligation "Account for the wrapped subst-prf checking relation or replace it with object-level proof-tree checking."}

   'willard-sjas-tab1-proof-check
   {:status :relevant
    :aspect :sjas-tab1-proof-list-predicate
    :obligation "Account for the wrapped Tab-1 proof-list checking relation and its measured proof-list object."}

   'willard-sjas-axiom-member
   {:status :relevant
    :aspect :sjas-axiom-membership
    :obligation "Preserve decoded system-code axiom membership evidence."}

   'willard-sjas-subst-code
   {:status :relevant
    :aspect :sjas-substitution-code
    :obligation "Preserve structural substitution-code evidence."}

   'propositional
   {:status :excluded
    :aspect :generic-sidecar
    :obligation "Reject generic sidecar closure from SJAS proof-predicate validation paths."}

   'first-order
   {:status :excluded
    :aspect :generic-sidecar
    :obligation "Reject generic sidecar closure from SJAS proof-predicate validation paths."}})

(defn- proof-symbol-fragment-boundary
  "Return the first-fragment boundary record for one encoded proof symbol.

   ADR-0096 keeps this separate from Track 2a relevance classification. A
   symbol may be encoded and relevant for audit purposes while still being
   outside the first Track 2b correspondence fragment. The first fragment admits
   formula-bearing structural tableau nodes, which have no proof-symbol tags,
   plus the bare `sjas-axiom` citation certificate."
  [sym]
  (let [classification (get proof-symbol-classifications sym)]
    (cond
      (= 'sjas-axiom sym)
      {:fragment-status :sjas-axiom-citation
       :classification-status (:status classification)
       :classification-aspect (:aspect classification)
       :fragment-obligation
       "Admit only as the bare axiom-citation certificate, with axiom membership checked separately."}

      (= :excluded (:status classification))
      {:fragment-status :excluded
       :classification-status (:status classification)
       :classification-aspect (:aspect classification)
       :fragment-obligation
       "Keep encoded for inspection, but reject from SJAS proof-predicate certificates."}

      :else
      {:fragment-status :outside-first-fragment
       :classification-status (:status classification)
       :classification-aspect (:aspect classification)
       :fragment-obligation
       "Do not admit to the first formula-bearing tableau fragment until Track 2b proves primitive status, bounded macro expansion, wrapper erasure, or unreachability."})))

(def proof-symbol-fragment-boundaries
  "First Track 2b fragment boundary for each encoded proof symbol.

   `proof-symbol-classifications` answers whether a symbol is relevant,
   excluded, or unresolved for correspondence analysis. This map answers the
   different question of whether a decoded proof term containing that symbol is
   already inside the first proof-object fragment selected for the
   correspondence theorem."
  (into {}
        (map (fn [sym] [sym (proof-symbol-fragment-boundary sym)]))
        sjas-code/proof-symbols))

(defn classify-proof-symbol
  "Return the Track 2a classification for a proof symbol, or nil when the symbol
   is not part of the current SJAS proof-certificate alphabet."
  [sym]
  (get proof-symbol-classifications sym))

(defn classify-proof-symbol-fragment
  "Return the first-fragment boundary for one encoded proof symbol."
  [sym]
  (get proof-symbol-fragment-boundaries sym))

(defn classify-profile-form
  "Return the path-sensitive Track 2a classification for a concrete profiled
   proof form.

   Returns nil for non-profiled forms or for profile markers that have not yet
   been classified. Some SJAS proof-check wrappers carry relation-specific
   payloads after the marker, so profiled forms need at least one payload item
   rather than exactly one."
  [form]
  (when (and (sequential? form)
             (= 'profiled (first form))
             (<= 3 (count form)))
    (get profile-form-classifications (second form))))

(defn- profile-form?
  [form]
  (boolean (classify-profile-form form)))

(defn- proof-byte?
  [value]
  (and (integer? value)
       (<= 0 value)
       (< value sjas-code/byte-base)))

(defn- structural-error
  [path reason details]
  (merge {:path path
          :reason reason}
         details))

(defn- structural-proof-summary
  [node path depth]
  (letfn [(invalid [errors]
            {:valid? false
             :node-count 0
             :leaf-count 0
             :max-depth depth
             :formula-byte-count 0
             :child-counts []
             :formula-byte-payloads []
             :errors errors})
          (combine [formula-bytes children child-summaries local-errors]
            (let [all-errors (into (vec local-errors)
                                   (mapcat :errors child-summaries))]
              {:valid? (empty? all-errors)
               :node-count (inc (reduce + 0 (map :node-count child-summaries)))
               :leaf-count (if (seq children)
                             (reduce + 0 (map :leaf-count child-summaries))
                             1)
               :max-depth (reduce max depth (map :max-depth child-summaries))
               :formula-byte-count (+ (count formula-bytes)
                                      (reduce + 0
                                              (map :formula-byte-count
                                                   child-summaries)))
               :child-counts (into [(count children)]
                                   (mapcat :child-counts child-summaries))
               :formula-byte-payloads (into [(vec formula-bytes)]
                                            (mapcat :formula-byte-payloads
                                                    child-summaries))
               :errors all-errors}))
          (summarize-children [children]
            (map-indexed (fn [idx child]
                           (structural-proof-summary child
                                                     (conj path :children idx)
                                                     (inc depth)))
                         children))]
    (cond
      (not (sequential? node))
      (invalid [(structural-error path
                                  :non-list-node
                                  {:value node})])

      (empty? node)
      (invalid [(structural-error path :empty-node {})])

      (sequential? (first node))
      (let [formula-bytes (vec (first node))
            children (rest node)
            local-errors (cond-> []
                           (empty? formula-bytes)
                           (conj (structural-error (conj path :formula-bytes)
                                                   :empty-wide-byte-payload
                                                   {}))
                           true
                           (into (keep-indexed
                                   (fn [idx byte]
                                     (when-not (proof-byte? byte)
                                       (structural-error
                                         (conj path :formula-bytes idx)
                                         :invalid-byte
                                         {:value byte})))
                                   formula-bytes)))]
        (combine formula-bytes
                 children
                 (summarize-children children)
                 local-errors))

      (integer? (first node))
      (let [byte-count (first node)
            after-count (rest node)
            available-byte-count (count after-count)
            enough-bytes? (<= byte-count available-byte-count)
            formula-bytes (if enough-bytes?
                            (take byte-count after-count)
                            after-count)
            children (if enough-bytes?
                       (drop byte-count after-count)
                       '())
            local-errors (cond-> []
                           (not (and (pos? byte-count)
                                     (< byte-count sjas-code/byte-base)))
                           (conj (structural-error (conj path :byte-count)
                                                   :invalid-flat-byte-count
                                                   {:value byte-count}))
                           (not enough-bytes?)
                           (conj (structural-error path
                                                   :flat-byte-count-mismatch
                                                   {:expected byte-count
                                                    :actual available-byte-count}))
                           true
                           (into (keep-indexed
                                   (fn [idx byte]
                                     (when-not (proof-byte? byte)
                                       (structural-error
                                         (conj path :formula-bytes idx)
                                         :invalid-byte
                                         {:value byte})))
                                   formula-bytes)))]
        (combine formula-bytes
                 children
                 (summarize-children children)
                 local-errors))

      :else
      (invalid [(structural-error (conj path :head)
                                  :invalid-node-head
                                  {:value (first node)})]))))

(defn audit-structural-proof-tree
  "Summarize a formula-bearing structural tableau proof term.

   This is a Track 2 audit helper, not a semantic proof checker. It validates
   the proof object shape consumed by the SJAS structural checker and exposes
   finite tree and byte-size metrics needed by the correspondence proof work."
  [proof-term]
  (let [summary (structural-proof-summary proof-term [] 1)
        reasons (into #{} (map :reason (:errors summary)))]
    (assoc summary :error-reasons reasons)))

(defn audit-proof-term
  "Summarize the correspondence obligations present in a decoded proof term.

   The audit is intentionally syntactic. It walks the ordinary Proflog proof
   tree, keeps only symbols, and partitions them by the current classification
   map. Unknown and unencodable symbols are reported separately so callers can
   distinguish ordinary formula payload symbols, classified encoded
   constructors, and proof evidence that the current SJAS certificate alphabet
   cannot represent."
  [proof-term]
  (let [steps (set (proof/collect-steps proof-term))
        profile-forms (into #{}
                            (filter profile-form?)
                            (tree-seq coll? seq proof-term))
        known? #(contains? proof-symbol-classifications %)
        encodable? (set sjas-code/proof-symbols)
        by-status (fn [status]
                    (into #{}
                          (filter #(= status (:status (classify-proof-symbol %))))
                          steps))
        profile-by-status (fn [status]
                            (into #{}
                                  (filter #(= status (:status (classify-profile-form %))))
                                  profile-forms))]
    {:symbols steps
     :relevant-symbols (by-status :relevant)
     :excluded-symbols (by-status :excluded)
     :unresolved-symbols (by-status :unresolved)
     :profile-forms profile-forms
     :relevant-profile-forms (profile-by-status :relevant)
     :probably-irrelevant-profile-forms (profile-by-status :probably-irrelevant)
     :excluded-profile-forms (profile-by-status :excluded)
     :probably-excluded-profile-forms (profile-by-status :probably-excluded)
     :unencodable-symbols (into #{} (remove encodable?) steps)
     :unclassified-symbols (into #{} (remove known?) steps)}))

(defn audit-first-correspondence-fragment
  "Classify a decoded proof term against the first Track 2b fragment.

   The first fragment is intentionally narrower than the encoded proof alphabet:
   formula-bearing structural tableau nodes contain no proof-symbol tags, while
   the bare `sjas-axiom` symbol is the one admitted citation certificate. Any
   other symbol-bearing proof term remains outside this fragment until a later
   correspondence slice proves how to admit or erase the involved symbols."
  [proof-term]
  (let [audit (audit-proof-term proof-term)
        symbols (:symbols audit)
        structural-summary (when (and (not= 'sjas-axiom proof-term)
                                      (empty? symbols))
                             (audit-structural-proof-tree proof-term))
        fragment-status (cond
                          (= 'sjas-axiom proof-term) :sjas-axiom-citation
                          (and structural-summary
                               (:valid? structural-summary))
                          :formula-bearing-tableau
                          structural-summary :malformed-structural-tableau
                          :else :outside-first-fragment)
        blocking-symbols (if (= :outside-first-fragment fragment-status)
                           symbols
                           #{})
        admitted-symbols (if (= :sjas-axiom-citation fragment-status)
                           #{'sjas-axiom}
                           #{})]
    (assoc audit
           :fragment-status fragment-status
           :admitted-symbols admitted-symbols
           :blocking-symbols blocking-symbols
           :structural-proof-summary structural-summary
           :fragment-boundaries (select-keys proof-symbol-fragment-boundaries
                                             symbols))))

(def equality-disequality-constructor-symbols
  "Generic equality and disequality proof constructors (ADR-0098).

   The Track 2a `:equality-extension` tags plus the disequality-closure tags
   carried in the tableau-closure set. ADR-0098 records that the SJAS structural
   proof checker closes equality and disequality branches by formula-bearing
   recognition -- reflexive same-term closure, rigid-different progression, and
   disequality storage with later recheck -- rather than by consuming these
   tags. They are therefore unreachable in accepted first-fragment certificates:
   the equality calculus is absorbed into formula-bearing closure (already
   admitted by ADR-0096), not admitted as separate proof-rule tags."
  (into relevant-equality-symbols
        '#{refl-close neq-rigid neq-store neq-close}))

(defn audit-equality-reachability
  "Report which generic equality/disequality constructors occur in a decoded
   proof term (ADR-0098).

   An empty `:equality-symbols-present` is positive reachability evidence that
   the equality calculus is absorbed into formula-bearing structural closure for
   this certificate rather than admitted as separate equality/disequality tags."
  [proof-term]
  (let [steps (set (proof/collect-steps proof-term))
        present (into #{} (filter equality-disequality-constructor-symbols) steps)]
    {:equality-symbols-present present
     :equality-reachable? (boolean (seq present))}))

(def quantifier-instantiation-constructor-symbols
  "Quantifier-instantiation proof constructors (ADR-0099).

   The SJAS structural proof checker expands universal, once-universal, and
   existential nodes into formula-bearing children: a quantifier node introduces
   a `par-term` parameter (or a gamma witness) and continues with the
   instantiated body, whose code is carried explicitly in the child node. The
   instantiation is therefore size-accounted by the ADR-0097 structural tree
   audit, and these tags are unreachable in first-fragment certificates."
  '#{univ once-univ witness})

(def fragment-reachability-constructor-sets
  "Per Track 2a aspect, the generic proof constructors whose first-fragment
   reachability the Track 2a completion resolves (ADR-0098/0099).

   An accepted first-fragment certificate is formula-bearing and contains none
   of them: the equality/disequality calculus, the reflected procedure-call
   expansion, and the quantifier instantiation are each absorbed into
   formula-bearing structural closure rather than admitted as compact
   proof-rule tags."
  {:equality-extension equality-disequality-constructor-symbols
   :procedure-call-expansion relevant-procedure-symbols
   :quantifier-instantiation quantifier-instantiation-constructor-symbols})

(defn audit-fragment-reachability
  "Report, per Track 2a aspect, which generic proof constructors occur in a
   decoded proof term (ADR-0099).

   `:reachable-by-aspect` maps each high-risk aspect to the constructors
   present; an all-empty map is positive evidence that the apparatus is absorbed
   into formula-bearing closure for this certificate rather than admitted as
   compact proof-rule tags. `:reachable?` is true when any aspect is non-empty."
  [proof-term]
  (let [steps (set (proof/collect-steps proof-term))
        by-aspect (into {}
                        (map (fn [[aspect syms]]
                               [aspect (into #{} (filter syms) steps)]))
                        fragment-reachability-constructor-sets)]
    {:reachable-by-aspect by-aspect
     :reachable? (boolean (some seq (vals by-aspect)))}))

(def sjas-structural-checker-rule-inventory
  "Branch-level audit of `sjas-structural-proof-check-state-decodedo` (ADR-0103).

   This is static proof-audit data keyed to the checker line ranges reviewed in
   ADR-0101. It deliberately does not participate in proof search. Path A uses
   `:path-a-status` to define the narrowed literal-Willard fragment; Path B uses
   `:dsjas-rule-families` to name the candidate extended apparatus that would
   be needed for full correspondence."
  [{:id :literal-save-agenda-continuation
    :line-range [6179 6209]
    :path-a-status :bookkeeping-lemma
    :path-a-obligations #{:agenda-ancestor-preservation}
    :dsjas-rule-families #{:branch-bookkeeping}
    :dsjas-open-obligations #{}}
   {:id :complementary-literal-closure
    :line-range [6210 6217]
    :path-a-status :direct-willard
    :path-a-obligations #{}
    :dsjas-rule-families #{:base-tableau}
    :dsjas-open-obligations #{}}
   {:id :conjunction
    :line-range [6218 6236]
    :path-a-status :direct-willard
    :path-a-obligations #{}
    :dsjas-rule-families #{:base-tableau :branch-bookkeeping}
    :dsjas-open-obligations #{}}
   {:id :forall-once-forall-expansion
    :line-range [6237 6313]
    :path-a-status :quantifier-lemma
    :path-a-obligations #{:quantifier-freshness :gamma-parameter-admissibility}
    :dsjas-rule-families #{:base-tableau :quantifier}
    :dsjas-open-obligations #{}}
   {:id :exists-expansion
    :line-range [6314 6363]
    :path-a-status :quantifier-lemma
    :path-a-obligations #{:quantifier-freshness}
    :dsjas-rule-families #{:base-tableau :quantifier}
    :dsjas-open-obligations #{}}
   {:id :false-not-true-closure
    :line-range [6364 6373]
    :path-a-status :truth-lemma
    :path-a-obligations #{:truth-constant-semantics}
    :dsjas-rule-families #{:truth-normalization}
    :dsjas-open-obligations #{}}
   {:id :not-false-agenda-continuation
    :line-range [6374 6392]
    :path-a-status :truth-lemma
    :path-a-obligations #{:truth-constant-semantics :agenda-ancestor-preservation}
    :dsjas-rule-families #{:truth-normalization :branch-bookkeeping}
    :dsjas-open-obligations #{}}
   {:id :double-negation-and-atomic-duals
    :line-range [6393 6482]
    :path-a-status :direct-willard
    :path-a-obligations #{:nnf-irrelevance}
    :dsjas-rule-families #{:base-tableau :truth-normalization}
    :dsjas-open-obligations #{}}
   {:id :negation-and-implication
    :line-range [6483 6596]
    :path-a-status :direct-willard
    :path-a-obligations #{:agenda-ancestor-preservation :nnf-irrelevance}
    :dsjas-rule-families #{:base-tableau :branch-bookkeeping}
    :dsjas-open-obligations #{}}
   {:id :negated-and-bounded-quantifier-duals
    :line-range [6597 6706]
    :path-a-status :quantifier-lemma
    :path-a-obligations #{:quantifier-freshness :bounded-guard-correctness}
    :dsjas-rule-families #{:base-tableau :quantifier}
    :dsjas-open-obligations #{}}
   {:id :disequality-progress-and-storage
    :line-range [6707 6754]
    :path-a-status :excluded
    :path-a-obligations #{:path-a-excluded-equality-theory}
    :dsjas-rule-families #{:equality-theory}
    :dsjas-open-obligations #{:equality-theory-admissibility}}
   {:id :profile-structural-closes
    :line-range [6755 6783]
    :path-a-status :excluded
    :path-a-obligations #{:path-a-excluded-profile-closures}
    :dsjas-rule-families #{:axiom-membership
                           :recursive-proof
                           :substitution-proof
                           :arithmetic-profile}
    :dsjas-open-obligations #{:sjas-axiom-size-accounting
                              :recursive-proof-well-foundedness
                              :literature-admissibility}}
   {:id :positive-equality-closures
    :line-range [6784 6805]
    :path-a-status :excluded
    :path-a-obligations #{:path-a-excluded-equality-theory}
    :dsjas-rule-families #{:equality-theory}
    :dsjas-open-obligations #{:equality-theory-admissibility}}
   {:id :equality-triggered-reflected-calls
    :line-range [6806 6867]
    :path-a-status :excluded
    :path-a-obligations #{:path-a-excluded-equality-theory
                          :path-a-excluded-reflected-calls}
    :dsjas-rule-families #{:equality-theory :reflected-call}
    :dsjas-open-obligations #{:reflected-call-admissibility
                              :literature-admissibility}}
   {:id :equality-agenda-continuation
    :line-range [6868 6889]
    :path-a-status :excluded
    :path-a-obligations #{:path-a-excluded-equality-theory}
    :dsjas-rule-families #{:equality-theory :branch-bookkeeping}
    :dsjas-open-obligations #{:equality-theory-admissibility}}
   {:id :disjunction
    :line-range [6890 6927]
    :path-a-status :direct-willard
    :path-a-obligations #{}
    :dsjas-rule-families #{:base-tableau}
    :dsjas-open-obligations #{}}
   {:id :direct-reflected-calls
    :line-range [6928 6985]
    :path-a-status :excluded
    :path-a-obligations #{:path-a-excluded-reflected-calls}
    :dsjas-rule-families #{:reflected-call}
    :dsjas-open-obligations #{:reflected-call-admissibility
                              :literature-admissibility}}
   {:id :additional-quantifier-expansions
    :line-range [6986 7111]
    :path-a-status :quantifier-lemma
    :path-a-obligations #{:quantifier-freshness :bounded-guard-correctness}
    :dsjas-rule-families #{:base-tableau :quantifier}
    :dsjas-open-obligations #{}}
   {:id :true-agenda-continuation
    :line-range [7112 7130]
    :path-a-status :truth-lemma
    :path-a-obligations #{:truth-constant-semantics :agenda-ancestor-preservation}
    :dsjas-rule-families #{:truth-normalization :branch-bookkeeping}
    :dsjas-open-obligations #{}}])

(def ^:private path-a-lemma-statuses
  "Path A statuses that remain in the narrowed theorem but require lemmas."
  #{:bookkeeping-lemma :truth-lemma :quantifier-lemma})

(defn audit-path-a-narrow-rule-inventory
  "Summarize the ADR-0103 Path A branch inventory.

   Path A keeps direct Willard branches plus branches that are allowed only after
   explicit bookkeeping/truth/quantifier lemmas. Equality, arithmetic/profile,
   reflected-call, and recursive proof-predicate branches are classified as
   excluded from this narrowed theorem."
  []
  (let [rules sjas-structural-checker-rule-inventory
        ids-by-status (fn [status]
                        (into #{}
                              (comp (filter #(= status (:path-a-status %)))
                                    (map :id))
                              rules))
        known-status? #(contains? (conj path-a-lemma-statuses
                                        :direct-willard
                                        :excluded)
                                  (:path-a-status %))]
    {:rule-count (count rules)
     :direct-willard-rule-ids (ids-by-status :direct-willard)
     :lemma-rule-ids (into #{}
                           (comp (filter #(path-a-lemma-statuses
                                            (:path-a-status %)))
                                 (map :id))
                           rules)
     :excluded-rule-ids (ids-by-status :excluded)
     :unclassified-rule-ids (into #{}
                                  (comp (remove known-status?)
                                        (map :id))
                                  rules)
     :path-a-open-obligations (into #{}
                                    (mapcat :path-a-obligations)
                                    rules)}))

(def path-a-narrow-obligation-proofs
  "Direct-examination proof clauses discharging the narrowed Path A lemmas.

   These clauses are documentation-grade proof data, kept executable so tests
   can detect if the branch inventory grows without a corresponding proof
   obligation. They do not alter the kernel. Each clause names the checker
   mechanism and the semantic-tableau fact used by the direct-examination proof."
  {:agenda-ancestor-preservation
   {:status :proved
    :checker-mechanism #{:sjas-agenda-cons-coreo
                         :sjas-agenda-heado
                         :sjas-proof-guided-selecto}
    :argument
    "Agenda entries are only formulas already introduced on the current branch, with the environment snapshot saved at introduction. Selecting one continues the same branch, so the selected child is a descendant of the ancestor that introduced it."}

   :truth-constant-semantics
   {:status :proved
    :checker-mechanism #{:false-leaf
                         :not-true-leaf
                         :true-agenda-continuation
                         :not-false-agenda-continuation}
    :argument
    "`false` and `not true` are contradictory leaves. `true` and `not false` add no branch obligation and therefore preserve closure of the remaining agenda."}

   :nnf-irrelevance
   {:status :proved
    :checker-mechanism #{:double-negation
                         :atomic-dual
                         :de-morgan
                         :implication-normalization}
    :argument
    "The checker's negation, implication, and atomic/equality dual clauses are exactly the ordinary tableau negation-normalization steps; replacing a node by the normalized child preserves the closed tableau tree up to Willard's permitted prenex/NNF irrelevance."}

   :quantifier-freshness
   {:status :proved
    :checker-mechanism #{:nominal-fresh
                         :sjas-next-branch-nomo
                         :env-extension}
    :argument
    "`sjas-next-branch-nomo` chooses the canonical branch name by environment depth and each quantifier clause extends `env` before checking the child, so introduced variables/parameters are fresh relative to the branch prefix."}

   :gamma-parameter-admissibility
   {:status :proved
    :checker-mechanism #{:var-term-branch-parameter
                         :proof-vars
                         :env-threading}
    :argument
    "Universal and once-universal clauses instantiate with branch variable terms recorded in `env` and `proof-vars`; these are the permitted parameter terms over the already introduced branch context."}

   :bounded-guard-correctness
   {:status :proved
    :checker-mechanism #{:bounded-forall-leq-guard
                         :bounded-exists-leq-guard
                         :negated-bounded-duals}
    :argument
    "The bounded quantifier clauses build exactly the `leq` guard formulas used by Willard's bounded gamma and delta rules, with polarity flipped only by the corresponding negated-quantifier dual."}})

(def path-a-narrow-rule-proof-clauses
  "Per-branch Path A proof clauses for the admitted checker fragment."
  {:literal-save-agenda-continuation
   {:status :proved
    :willard-correspondence :branch-ancestor-bookkeeping
    :uses #{:agenda-ancestor-preservation}}
   :complementary-literal-closure
   {:status :proved
    :willard-correspondence :closed-branch-complementary-literals
    :uses #{}}
   :conjunction
   {:status :proved
    :willard-correspondence :alpha
    :uses #{:agenda-ancestor-preservation}}
   :forall-once-forall-expansion
   {:status :proved
    :willard-correspondence :gamma
    :uses #{:quantifier-freshness :gamma-parameter-admissibility}}
   :exists-expansion
   {:status :proved
    :willard-correspondence :delta
    :uses #{:quantifier-freshness}}
   :false-not-true-closure
   {:status :proved
    :willard-correspondence :truth-constant-closure
    :uses #{:truth-constant-semantics}}
   :not-false-agenda-continuation
   {:status :proved
    :willard-correspondence :truth-normalization-bookkeeping
    :uses #{:truth-constant-semantics :agenda-ancestor-preservation}}
   :double-negation-and-atomic-duals
   {:status :proved
    :willard-correspondence :negation-normalization
    :uses #{:nnf-irrelevance}}
   :negation-and-implication
   {:status :proved
    :willard-correspondence :negation-implication-alpha-beta
    :uses #{:nnf-irrelevance :agenda-ancestor-preservation}}
   :negated-and-bounded-quantifier-duals
   {:status :proved
    :willard-correspondence :quantifier-duals-and-bounded-rules
    :uses #{:quantifier-freshness :bounded-guard-correctness}}
   :disjunction
   {:status :proved
    :willard-correspondence :beta
    :uses #{}}
   :additional-quantifier-expansions
   {:status :proved
    :willard-correspondence :gamma-delta-bounded-gamma-bounded-delta
    :uses #{:quantifier-freshness
            :gamma-parameter-admissibility
            :bounded-guard-correctness}}
   :true-agenda-continuation
   {:status :proved
    :willard-correspondence :truth-normalization-bookkeeping
    :uses #{:truth-constant-semantics :agenda-ancestor-preservation}}})

(defn audit-path-a-narrow-correspondence-proof
  "Return the completed ADR-0103 Path A narrow-fragment proof audit.

   The theorem proved here is intentionally narrow: it excludes SJAS profile,
   equality-progress, reflected-call, recursive proof-predicate, substitution,
   and bare axiom-citation machinery. Within that narrowed domain, every
   admitted checker branch is either a direct Willard tableau rule or a branch
   whose named lemma is discharged in `path-a-narrow-obligation-proofs`."
  []
  (let [rules sjas-structural-checker-rule-inventory
        admitted-rules (remove #(= :excluded (:path-a-status %)) rules)
        admitted-rule-ids (into #{} (map :id) admitted-rules)
        excluded-rule-ids (into #{}
                                (comp (filter #(= :excluded (:path-a-status %)))
                                      (map :id))
                                rules)
        required-obligations (into #{}
                                   (mapcat :path-a-obligations)
                                   admitted-rules)
        discharged-obligations (set (keys path-a-narrow-obligation-proofs))
        missing-rule-proofs (set/difference admitted-rule-ids
                                            (set (keys path-a-narrow-rule-proof-clauses)))
        unproved-rule-ids (into #{}
                                (comp (filter #(not= :proved
                                                     (get-in path-a-narrow-rule-proof-clauses
                                                             [(:id %) :status])))
                                      (map :id))
                                admitted-rules)
        open-obligations (set/union
                           (set/difference required-obligations
                                           discharged-obligations)
                           (set/difference
                             (into #{}
                                   (mapcat :uses)
                                   (vals path-a-narrow-rule-proof-clauses))
                             discharged-obligations)
                           missing-rule-proofs
                           unproved-rule-ids)]
    {:verdict (if (empty? open-obligations) :proved :incomplete)
     :theorem
     "For non-axiom formula-bearing structural proof trees whose checker path uses only Path-A-admitted branches, Proflog acceptance is equivalent to Willard semantic-tableau proof acceptance, up to the listed normalization and agenda irrelevancies."
     :proved-rule-ids admitted-rule-ids
     :excluded-rule-ids excluded-rule-ids
     :discharged-obligations (select-keys path-a-narrow-obligation-proofs
                                          required-obligations)
     :rule-proof-clauses (select-keys path-a-narrow-rule-proof-clauses
                                      admitted-rule-ids)
     :open-obligations open-obligations
     :size-lower-bound :formula-bearing-proof-tree-bytes-carry-each-node-formula}))

(def ^:private dsjas-global-open-obligations
  "Path B blockers that are not tied to one line range.

   The branch inventory can name candidate rule families, but the full extended
   apparatus still needs a literature-admissibility argument and a chosen
   proof-object size accounting for bare `sjas-axiom` citations."
  #{:sjas-axiom-size-accounting
    :recursive-proof-well-foundedness
    :literature-admissibility})

(defn audit-dsjas-rule-inventory
  "Summarize the ADR-0103 Path B candidate `D_SJAS` branch inventory."
  []
  (let [rules sjas-structural-checker-rule-inventory]
    {:rule-count (count rules)
     :rule-families (into #{}
                          (mapcat :dsjas-rule-families)
                          rules)
     :open-obligations (into dsjas-global-open-obligations
                             (mapcat :dsjas-open-obligations)
                             rules)
     :unclassified-rule-ids (into #{}
                                  (comp (filter #(empty?
                                                   (:dsjas-rule-families %)))
                                        (map :id))
                                  rules)}))

(defn audit-path-b-correspondence-verdict
  "Return the completed ADR-0103 Path B verdict for the current implementation.

   Path B cannot be completed as a Track 2b proof that the current accepted
   domain is literal Willard `D`: the accepted domain includes extended
   equality/profile/reflected/proof-predicate branches and the fixed-size
   `sjas-axiom` citation counterexample from ADR-0102. The positive result is a
   Track 2c handoff: a future `D_SJAS` theorem must explicitly select the
   extended apparatus and repair axiom-citation size accounting."
  []
  (let [inventory (audit-dsjas-rule-inventory)
        literal-willard-families #{:base-tableau
                                   :branch-bookkeeping
                                   :truth-normalization
                                   :quantifier}
        extended-families (set/difference (:rule-families inventory)
                                          literal-willard-families)]
    {:literal-willard-track-2b-verdict :impossible-for-current-domain
     :dsjas-verdict :track-2c-required
     :blocking-reasons #{:sjas-axiom-citation-size-counterexample
                         :non-willard-extended-rule-families}
     :required-size-accounting-repair
     :formula-bearing-axiom-leaf-or-combined-object-required
     :extended-rule-families extended-families
     :candidate-rule-families (:rule-families inventory)
     :track-2c-open-obligations (:open-obligations inventory)
     :unclassified-rule-ids (:unclassified-rule-ids inventory)}))

(def dsjas-track2c-rule-specification
  "ADR-0104 selected rule-family specification for `D_SJAS`.

   This is the first executable Track 2c artifact. It names the selected
   apparatus independently of the implementation's current branch order. Each
   family is either a direct semantic-tableau family, a bounded bookkeeping
   normalization, or a selected object-language extension whose relation is
   already implemented over decoded codes and branch state."
  {:base-tableau
   {:status :selected
    :kind :semantic-tableau
    :rule "Alpha, beta, implication, negation, branch closure, and root/child tree validation."}
   :branch-bookkeeping
   {:status :selected
    :kind :bounded-normalization
    :rule "Agenda continuation and saved branch-environment snapshots preserve tableau ancestry."}
   :truth-normalization
   {:status :selected
    :kind :bounded-normalization
    :rule "`false`, `not true`, `true`, and `not false` are fixed truth-constant closures/skips."}
   :quantifier
   {:status :selected
    :kind :semantic-tableau
    :rule "Gamma, delta, bounded gamma, bounded delta, and negated quantifier duals with branch-local freshness."}
   :equality-theory
   {:status :selected
    :kind :selected-extension
    :rule "Branch-local equality substitution, disequality storage, contradiction, and constructor-theory closure."}
   :arithmetic-profile
   {:status :selected
    :kind :selected-extension
    :rule "U-Grounding arithmetic, byte reading, syntax-code predicates, and formula-class predicates as object relations."}
   :axiom-membership
   {:status :selected
    :kind :selected-extension
    :rule "Finite axiom membership and AxiomConj reconstruction from decoded system-code."}
   :reflected-call
   {:status :selected
    :kind :selected-extension
    :rule "Positive, negative, and guarded reflected calls expand only from reflected records decoded from system-code."}
   :recursive-proof
   {:status :selected
    :kind :selected-extension
    :rule "`tableau-proof/3` leaves decode theorem/proof/system code and validate the supplied proof payload."}
   :substitution-proof
   {:status :selected
    :kind :selected-extension
    :rule "`subst-prf/4` leaves validate substitution-code/source-result conditions and the supplied theorem proof payload."}})

(defn audit-dsjas-track2c-specification
  "Summarize the ADR-0104 selected `D_SJAS` apparatus."
  []
  (let [inventory (audit-dsjas-rule-inventory)
        selected-families (set (keys dsjas-track2c-rule-specification))]
    {:apparatus :D_SJAS
     :status :selected-apparatus
     :rule-families selected-families
     :rule-family-count (count selected-families)
     :rules dsjas-track2c-rule-specification
     :unclassified-rule-ids (:unclassified-rule-ids inventory)
     :inventory-families-without-spec (set/difference (:rule-families inventory)
                                                      selected-families)
     :spec-families-without-inventory (set/difference selected-families
                                                      (:rule-families inventory))}))

(def dsjas-proof-object-accounting
  "ADR-0104 proof-size accounting repair for `D_SJAS`.

   The ADR-0102 counterexample refutes measuring a bare `sjas-axiom` citation by
   `P` alone. Track 2c repairs this by measuring recursive proof-predicate
   leaves as composite, inspectable objects. Tableau proof leaves use `(S,F,P)`;
   substitution-proof leaves use `(S,G,F,P)`, where `G` is the substitution
   source/skeleton code. Structural proof trees keep the ordinary
   formula-bearing proof-code measure."
  {:selected-repair :combined-proof-object
   :proof-object-symbols {:tableau-proof 'dsjas-tableau-proof-object
                          :substitution-proof 'dsjas-subst-prf-object
                          :tab1-proof-list 'dsjas-tab1-proof-object}
   :tableau-citation-measured-components #{:system-code
                                            :theorem-code
                                            :proof-code}
   :substitution-citation-measured-components #{:system-code
                                                :substitution-code
                                                :theorem-code
                                                :proof-code}
   :tab1-citation-measured-components #{:system-code
                                         :theorem-code
                                         :proof-list-code}
   :structural-measured-components #{:proof-code}
   :tab1-entry-validation-status :entry-validation-implemented
   :citation-size-source
   {:system-code "Decoded finite axiom basis and profile/fixed-point payload."
    :substitution-code "Decoded substitution source or fixed-point skeleton payload."
    :theorem-code "Decoded cited theorem/axiom formula payload."
    :proof-code "Decoded `sjas-axiom` citation marker."
    :proof-list-code "Decoded Tab-1 theorem/proof-code pair list."}
   :adr-0102-counterexample-repaired? true})

(defn audit-dsjas-proof-object-accounting
  "Return the selected ADR-0104 proof-object accounting repair."
  []
  dsjas-proof-object-accounting)

(def tab1-proof-list-roadmap
  "ADR-0120 executable roadmap for the first Tab-1 implementation surface."
  {:apparatus :Tab-1
   :profile :willard-sjas-tab1
   :generic-tab-k-proof-list "H = [(t1,p1), ..., (tn,pn)]"
   :public-intermediate-classifiers {:pi-star-1 'pi-star-1?
                                     :sigma-star-1 'sigma-star-1?}
   :willard-terminology {:rank-1* :pi-star-1-or-sigma-star-1
                         :u1* :level-1-bounded-u-grounding-class}
   :host-conveniences {:pi-star-1-encodable? :host-basis-admission-only}
   :adr-0120-scope #{:proof-list-syntax
                     :public-proof-object-coding
                     :measured-tab1-object
                     :tab1-selfcons-relation-symbols
                     :terminology-reconciliation}
   :tab1-theorem-reuse-status :implemented
   :deferred-obligations #{}
   :boundary-failure-variants #{:Tab-2 :stronger-Tab-k}})

(defn audit-tab1-proof-list-roadmap
  "Return the ADR-0120 Tab-1 terminology and scope audit."
  []
  tab1-proof-list-roadmap)

(def self-extension-data-encoding-survey
  "ADR-0123 executable survey for the first SJAS self-extension demo.

   This is a documentation-layer audit, not proof-search code. It records why
   the next implementation slice starts with finite reflected pair beta axioms
   and defers recursive lists until the representation layer is present."
  {:workstream :self-extension
   :selected-demo :pairs-first
   :implementation-layer :reflected-beta
   :survey-criteria #{:finite-beta-axiomatization
                      :level-1-classifier-discipline
                      :system-identity-change
                      :group-three-selfcons-regeneration
                      :focused-sjas-tractability}
   :candidates
   {:fresh-pair-functions
    {:verdict :selected
     :data-symbols {'pair 2
                    'fst 1
                    'snd 1}
     :beta-laws ["forall x y. fst(pair(x,y)) = x"
                 "forall x y. snd(pair(x,y)) = y"]
     :argument "Finite projection laws give an immediately reflected data layer whose beta source changes system identity and regenerated SelfCons code."}
    :lists-from-pairs
    {:verdict :second-stage
     :argument "Lists are the intended next representation layer, but recursive list operations should be added after pair axioms are reflected and citeable."}
    :tagged-constants-only
    {:verdict :too-weak
     :argument "Fresh constants can demonstrate recoding, but they do not provide data-structure operations for self-interpretation."}}
   :deferred-self-extension-obligations #{:list-recursion
                                          :encoded-syntax-manipulation}})

(defn audit-self-extension-data-encoding-survey
  "Return the ADR-0123 data-encoding survey for Workstream C."
  []
  self-extension-data-encoding-survey)

(def boundary-failure-roadmap
  "ADR-0124 executable Workstream B contract for negative SJAS variants.

   This audit intentionally separates a public variant surface from a completed
   Goedel-boundary failure. The first surface is total multiplication; ADR-0125
   and ADR-0126 complete its reduced witness and generated target stages, but
   constructed certificates and proof-search synthesis evidence remain open."
  {:workstream :workstream-b
   :first-variant :total-multiplication
   :planned-variants #{:total-multiplication
                       :tab-2-or-stronger
                       :xtab-or-lem-axiom}
   :required-witness-stages [:reduced-reflected-beta-witness
                             :full-generated-selfcons-contradiction-target]
   :final-evidence-required #{:constructed-certificate
                              :proof-search-synthesis}
   :variant-statuses {:total-multiplication :full-target-implemented
                      :tab-2-or-stronger :not-started
                      :xtab-or-lem-axiom :not-started}
   :completed-witness-stages
   {:total-multiplication #{:reduced-reflected-beta-witness
                            :full-generated-selfcons-contradiction-target}}
   :reduced-witnesses
   {:total-multiplication
    {:kind :squaring-chain
     :default-depth 3
     :fragment "u_0 = 2; u_(i+1) = mul(u_i,u_i)"
     :status :implemented
     :remaining-stage :completed}}
   :full-targets
   {:total-multiplication
    {:kind :generated-selfcons-refutation
     :system-builder 'total-multiplication-reduced-witness-system
     :target-report 'total-multiplication-full-target-report
     :target-shape "AxiomConj(S_total-mul) /\\ not(SelfCons(S_total-mul))"
     :status :implemented
     :remaining-evidence #{:constructed-certificate
                           :proof-search-synthesis}}}
   :open-obligations
   {:total-multiplication #{:constructed-certificate
                            :proof-search-synthesis}
    :tab-2-or-stronger #{:variant-surface
                         :reduced-reflected-beta-witness
                         :full-generated-selfcons-contradiction-target
                         :constructed-certificate
                         :proof-search-synthesis}
    :xtab-or-lem-axiom #{:variant-surface
                         :reduced-reflected-beta-witness
                         :full-generated-selfcons-contradiction-target
                         :constructed-certificate
                         :proof-search-synthesis}}
   :workstream-complete? false})

(defn audit-boundary-failure-roadmap
  "Return the ADR-0124 Workstream B negative-variant audit."
  []
  boundary-failure-roadmap)

(def dsjas-combined-size-lower-bound
  "ADR-0104 lower-bound audit for the selected proof-object accounting.

   The result is stated under the same code-injectivity and byte-inspectability
   assumptions already used by the SJAS coding ADRs. Citation objects now count
   theorem, system, and substitution payloads where applicable, so the ADR-0102
   fixed-size `P` counterexample no longer hides the formula occurrences being
   measured. Structural objects keep the ADR-0097 formula-bearing proof-tree
   argument."
  {:status :proved-under-code-injectivity
   :covered-proof-object-kinds #{:tableau-axiom-citation
                                 :substitution-axiom-citation
                                 :formula-bearing-structural-tree}
   :uncovered-proof-object-kinds #{}
   :kind-arguments
   {:tableau-axiom-citation
    {:measure :combined-proof-object
     :object-symbol 'dsjas-tableau-proof-object
     :measured-components (:tableau-citation-measured-components
                            dsjas-proof-object-accounting)
     :j-source :theorem-code-payload
     :argument
     "Every function/application occurrence of the cited formula is in the measured theorem-code payload, while system-code bytes account for the finite axiom basis that made the citation legal."}
    :substitution-axiom-citation
    {:measure :combined-proof-object
     :object-symbol 'dsjas-subst-prf-object
     :measured-components (:substitution-citation-measured-components
                            dsjas-proof-object-accounting)
     :j-source :theorem-code-payload
     :argument
     "Every function/application occurrence of the cited theorem is in the measured theorem-code payload, while system-code and substitution-code bytes account for the finite axiom basis and fixed substitution source that make the substitution proof legal."}
    :formula-bearing-structural-tree
    {:measure :proof-code
     :measured-components #{:proof-code}
     :j-source :proof-code-formula-node-payloads
     :argument
     "Every node in the accepted structural proof tree carries formula bytes in the proof code, so application occurrences are not hidden outside the proof object."}}
   :assumptions #{:injective-public-code-reading
                  :trailing-zero-preservation
                  :fixed-symbol-codebook-or-decoded-signature}})

(defn audit-dsjas-combined-size-lower-bound
  "Return the ADR-0104 proof-size lower-bound audit for `D_SJAS` proof objects."
  []
  dsjas-combined-size-lower-bound)

(def dsjas-recursive-well-foundedness
  "ADR-0104 recursive proof/substitution well-foundedness audit.

   Public `tableau-proof/3` and `subst-prf/4` leaves, and the measured
   `dsjas-tableau-proof/3` and `dsjas-subst-prf/4` leaves used by SelfCons, are
   not justified by runtime fuel: fixed proof-tree validation deliberately
   preserves fuel. The selected `D_SJAS` semantics instead treats recursive proof
   checks as a least fixed point over finite, acyclic proof-call graphs. The
   proof is by induction on proof-call graph height, with ordinary structural
   induction on each decoded formula-bearing proof tree at a graph node."
  {:status :proved-for-finite-acyclic-proof-call-graphs
   :recursive-semantics :least-fixed-point-over-proof-call-graph
   :primary-measure :decoded-proof-code-payload
   :well-founded-measures #{:proof-call-graph-height
                            :decoded-proof-code-payload
                            :structural-proof-node-count}
   :secondary-measures #{:decoded-formula-size
                         :finite-system-code-size
                         :branch-state-size}
   :recursive-branches #{:tableau-proof-structural-core
                         :subst-prf-structural-core
                         :dsjas-tableau-proof-structural-core
                         :dsjas-subst-prf-structural-core}
   :unmeasured-recursive-branches #{}
   :runtime-fuel-role :not-a-proof-measure
   :cyclic-call-policy :no-finite-derivation
   :discharged-risks #{:same-proof-code-self-call
                       :mutual-proof-code-cycle
                       :non-subtree-proof-code-reference}
	   :implementation-evidence
	   {:tableau-proof-structural-core
	    {:line-range [7541 7568]
	     :proof-code-source :tableau-proof-third-argument
	     :decoder :decode-structural-proof-bytes-coreo
	     :recursive-entry :sjas-proof-check-programo}
	    :subst-prf-structural-core
	    {:line-range [7634 7706]
	     :proof-code-source :subst-prf-fourth-argument
	     :decoder :decode-structural-proof-bytes-coreo
	     :recursive-entry :sjas-proof-check-programo}
	    :dsjas-tableau-proof-structural-core
	    {:line-range [7487 7502]
	     :proof-code-source :decoded-dsjas-tableau-proof-object
	     :composite-object 'dsjas-tableau-proof-object
	     :decoder :decode-structural-proof-bytes-coreo
	     :recursive-entry :sjas-proof-check-programo}
	    :dsjas-subst-prf-structural-core
	    {:line-range [7504 7523]
	     :proof-code-source :decoded-dsjas-subst-prf-object
	     :composite-object 'dsjas-subst-prf-object
	     :decoder :decode-structural-proof-bytes-coreo
	     :recursive-entry :sjas-proof-check-programo}
	    :fuel-preservation
	    {:line-range [6124 6132]
     :role :runtime-certificate-validation-only}}
   :proof-obligations
   {:finite-node-decoding
    {:status :proved
     :argument
     "Each structural recursive branch reads a finite object proof-code byte stream and decodes it through the counted structural proof grammar before re-entering proof checking."}
    :structural-tree-descent
    {:status :proved
     :argument
     "Within one decoded proof payload, recursive checker calls consume child proof nodes of that finite formula-bearing tree; induction on structural proof-node count handles ordinary tableau descent."}
    :non-subtree-proof-calls
    {:status :proved-by-selected-semantics
     :argument
     "A proof-predicate leaf may target a separate proof-code payload, so subtree descent alone is insufficient. `D_SJAS` therefore ranks those calls by proof-call graph height."}
    :cycle-exclusion
    {:status :proved-by-least-fixed-point-semantics
     :argument
     "Same-proof-code self-calls and mutual proof-code cycles have no base case and therefore no finite least-fixed-point derivation; they are not accepted proof objects under the selected `D_SJAS` relation."}}
   :argument
   "For a finite acyclic graph of proof-predicate calls, prove acceptance by induction on graph height. Height-zero nodes close through ordinary structural tableau rules, axiom citations, arithmetic/profile closures, substitution-code validation, or other non-recursive selected leaves. A height-(n+1) recursive leaf first decodes the object proof-code payload, then checks a graph successor of strictly smaller height; the decoded proof tree at each node is finite and is handled by structural induction on its proof-node count. Cyclic proof-code references cannot be assigned such a finite height and therefore have no finite derivation in the least fixed point."})

(defn audit-dsjas-recursive-well-foundedness
  "Return the ADR-0104 recursive proof/substitution well-foundedness audit."
  []
  dsjas-recursive-well-foundedness)

(def dsjas-rule-family-admissibility
  "Per-family ADR-0104 literature-admissibility classification for `D_SJAS`.

   The classifications are intentionally about the selected variant apparatus,
   not about literal identity with Willard's ordinary semantic-tableau `D`."
  {:base-tableau
   {:status :admissible
    :classification :literal-tableau-core
    :argument "Alpha, beta, negation, implication, branch closure, and root/child validation are the ordinary semantic-tableau core."}
   :branch-bookkeeping
   {:status :admissible
    :classification :bounded-normalization
    :argument "Agenda and saved-environment bookkeeping only records which finite branch formula is continued; it does not add theorem power or hide proof bytes."}
   :truth-normalization
   {:status :admissible
    :classification :bounded-normalization
    :argument "`false`, `not true`, `true`, and `not false` are fixed truth-constant cases representable as local branch predicates."}
   :quantifier
   {:status :admissible
    :classification :literal-tableau-core
    :argument "Gamma, delta, bounded gamma/delta, and negated quantifier duals remain tableau rules with finite branch-local witnesses."}
   :equality-theory
   {:status :admissible
    :classification :selected-primitive
    :argument "Equality substitution, disequality storage, constructor contradiction, and stored-disequality violation are selected `D_SJAS` primitive branch predicates over finite branch state."}
   :arithmetic-profile
   {:status :admissible
    :classification :selected-primitive
    :argument "U-Grounding arithmetic, byte reading, syntax predicates, and formula-class predicates are selected object relations over decoded finite codes, not host truth oracles."}
   :axiom-membership
   {:status :admissible
    :classification :bounded-code-reconstruction
    :argument "Axiom membership and `AxiomConj` are reconstructed from decoded system-code sections instead of generated host registries."}
   :reflected-call
   {:status :admissible
    :classification :bounded-macro
    :argument "Reflected positive, negative, and guarded calls expand only through finite reflected clause records decoded from `system-code`."}
   :recursive-proof
   {:status :admissible
    :classification :least-fixed-point-selected-primitive
    :argument "Recursive `tableau-proof/3` leaves are admitted under the finite acyclic proof-call graph semantics proved in `dsjas-recursive-well-foundedness`."}
   :substitution-proof
   {:status :admissible
    :classification :least-fixed-point-selected-primitive
    :argument "`subst-prf/4` combines finite substitution-code validation with the same well-founded selected proof relation used by `tableau-proof/3`."}})

(def dsjas-literature-admissibility
  "ADR-0104 literature-admissibility audit for the selected `D_SJAS` apparatus.

   The theorem proved here is variant admissibility: Willard's `D`-parameterized
   self-verification framework may be instantiated with this explicitly selected
   `D_SJAS` apparatus because its rule families are bounded, inspectable,
   tableau-shaped primitives or macros with repaired proof-size accounting. This
   does not identify `D_SJAS` with literal ordinary-tableau `D`."
  {:status :proved-for-selected-dsjas-variant
   :apparatus-label :IS#_D_SJAS_beta
   :not-literal-willard-d? true
   :rule-family-admissibility dsjas-rule-family-admissibility
   :inadmissible-rule-families #{}
   :discharged-criteria #{:natural-tree-coding
                          :bounded-object-relations
                          :semantic-tableau-shape
                          :selected-apparatus-labeling
                          :combined-proof-size-discipline
                          :recursive-proof-well-foundedness
                          :d-parametric-proof-predicate
                          :system-code-reconstruction
                          :primitive-or-bounded-macro-rules}
   :open-criteria #{}
   :standing-assumptions #{:willard-d-parametricity
                           :external-beta-pi-star-1-truth
                           :injective-public-code-reading
                           :trailing-zero-preservation
                           :least-fixed-point-recursive-semantics}
   :criteria
   {:natural-tree-coding
   "Formula-bearing structural proof trees and composite tableau/substitution citation objects are inspectable byte-coded proof objects."
    :bounded-object-relations
    "Code reading, axiom membership, arithmetic/profile closure, reflected calls, substitution, and proof checks are represented as object relations over finite codes."
    :semantic-tableau-shape
    "The core proof state remains tableau-shaped: branch agenda, child proof nodes, literals, closures, equality state, and finite leaves."
    :selected-apparatus-labeling
    "`D_SJAS` is named as a modified selected apparatus, not identified with literal Willard `D`."
    :combined-proof-size-discipline
    "The proof-size theorem is repaired over composite `(S,F,P)` and `(S,G,F,P)` objects for recursive proof leaves and formula-bearing proof-code bytes for structural leaves."
    :recursive-proof-well-foundedness
    "Recursive `tableau-proof/3` and `subst-prf/4` leaves are interpreted by a least fixed point over finite acyclic proof-call graphs."
    :d-parametric-proof-predicate
    "The Willard source trail uses `Prf^D`, `ExPrf^D`, `Subst`, and `SubstPrf^D` for a selected deduction method `D`; Track 2c instantiates that parameter with `D_SJAS`."
    :system-code-reconstruction
    "`D_SJAS` reconstructs axiom, reflected-clause, profile, and fixed-point data from the presented system code."
    :primitive-or-bounded-macro-rules
    "Every non-literal-Willard rule family is either a named selected primitive over finite branch/code state or a bounded macro expansion from decoded finite system-code payloads."}
   :source-trail
   {:proof-coding-note "docs/log/2026-05-15-sjas-proof-coding-citations.md"
    :tableau-arithmeticization-spec "docs/SJAS_TABLEAU_ARITHMETIZATION_SPEC.md"
    :track-2c-program "docs/adr/ADR-0104-dsjas-track2c.md"
    :extended-apparatus-inventory "docs/log/2026-06-13-sjas-path-b-extended-dsjas.md"}})

(defn audit-dsjas-literature-admissibility
  "Return the ADR-0104 literature-admissibility status for `D_SJAS`."
  []
  dsjas-literature-admissibility)

(def ^:private dsjas-quantitative-rule-family-preservation
  "ADR-0108 bounded-satisfaction preservation clauses for the selected
   `D_SJAS` rule families.

   These clauses are the direct-examination bridge from ADR-0104's selected
   apparatus to Willard's Appendix D Normed(a,b) open-branch argument. They are
   deliberately phrased at the mathematical audit layer: no kernel relation is
   changed or shortcut."
  {:base-tableau
   {:status :proved
    :argument "The alpha, beta, implication, negation, closure, and root/child cases are exactly the semantic-tableau rules used by Willard's Appendix D argument."}
   :branch-bookkeeping
   {:status :proved
    :argument "Agenda and environment snapshots only preserve branch ancestry; erasing them leaves the same formula path and cannot close a Normed(a,b) positive branch."}
   :truth-normalization
   {:status :proved
    :argument "Truth-constant cases are bounded local rewrites of `false`, `true`, and their negations, so they preserve the same scoped standard-model valuation."}
   :quantifier
   {:status :proved
    :argument "Gamma, delta, bounded gamma/delta, and negated duals match the bounded-witness and U-grounded-term tableau steps in the Appendix D proof."}
   :equality-theory
   {:status :proved
    :argument "Equality substitution and disequality closure are branch-local first-order equality consequences over already decoded finite terms; they do not introduce faster-growing witnesses."}
   :arithmetic-profile
   {:status :proved
    :argument "U-Grounding arithmetic, byte reading, syntax, class, and negation-pair predicates are bounded Delta_0 checks over finite code payloads and close only standard-true/standard-false local atoms."}
   :axiom-membership
   {:status :proved
    :argument "Axiom citations are ordinary Z-axiom leaves once the selected measure counts the decoded system and theorem payloads; no formula occurrence is hidden outside Log_D_SJAS."}
   :reflected-call
   {:status :proved
    :argument "Reflected positive, negative, and guarded calls are finite macro expansions from decoded system-code clauses, so replacing the macro by its expansion preserves the same branch valuation."}
   :recursive-proof
   {:status :proved
    :argument "Recursive `tableau-proof/3` leaves are Delta_0 proof-predicate atoms interpreted by the finite acyclic least-fixed-point semantics proved in ADR-0104."}
   :substitution-proof
   {:status :proved
    :argument "`subst-prf/4` combines bounded substitution-code validation with the same finite acyclic proof-predicate semantics, preserving scoped truth of the represented proof atom."}})

(def dsjas-quantitative-ea-stability
  "ADR-0108 quantitative EA-stability theorem for the selected `D_SJAS`
   apparatus.

   The theorem is intentionally not stated over the bare proof-code object `P`.
   ADR-0102 refuted that statement with the fixed-size `sjas-axiom` citation.
   The positive theorem uses ADR-0104's selected proof-object length
   `Log_D_SJAS`: structural tableaux are measured by proof-code bytes, while
   recursive proof-predicate citation leaves are measured by the composite
   inspectable objects `(S,F,P)` and `(S,G,F,P)`.
   With that measure, the Appendix D A/E-stability contradiction proof carries
   over to `D_SJAS` by the rule-family preservation clauses above."
  {:status :proved-for-selected-combined-proof-measure
   :apparatus :D_SJAS
   :configuration :IS#_D_SJAS_beta
   :source-definitions
   {:a-stability "Definition 5.1: short Pi_1 proofs preserve Good(1/2 #theta)."
    :e-stability "Definition 5.3: short Sigma_1 proofs preserve Good(1/2 floor(Log(p)) - 1)."
    :ea-stability "Definition 5.5: both A-stability and E-stability."
    :appendix-d "Theorem D.4 proves the semantic-tableau case from the Normed(a,b) open-branch lemma and the conventional tableaux encoding requirement."}
   :measurement-verdicts {:proof-code-only :refuted
                          :selected-combined-proof-measure :proved}
   :quantitative-constants
   {:a-stability {:sigma 1
                  :tau 1
                  :lambda 1/2
                  :mu 0}
    :e-stability {:sigma 1
                  :tau 1
                  :lambda 1/2
                  :mu -1}}
	   :selected-length-measure
	   {:name :log-dsjas
	    :definition "The base-2 logarithmic length of the selected `D_SJAS` proof object."
	    :structural-measured-components (:structural-measured-components
	                                      dsjas-proof-object-accounting)
	    :tableau-citation-measured-components
	    (:tableau-citation-measured-components dsjas-proof-object-accounting)
	    :substitution-citation-measured-components
	    (:substitution-citation-measured-components dsjas-proof-object-accounting)
	    :structural-case "For formula-bearing structural proof trees, Log_D_SJAS is the ordinary proof-code length."
	    :citation-case "For measured proof-predicate citations, Log_D_SJAS is the combined length of decoded system-code, theorem-code, proof marker, and substitution-code when the predicate is `dsjas-subst-prf/4`."}
   :proof-code-only-counterexample
   {:status :refuted
    :counterexample :sjas-axiom-citation
    :proof-bits 18
    :formula-j 18
    :five-j-bits 90
    :unbounded-formula-payload? true
    :argument "The fixed `sjas-axiom` marker can cite arbitrarily large formulas when the formula and system payloads are not counted, so the Willard-style `Log(p)` inequality is false for proof-code-only `P`."}
   :theorem
   {:a-stability
    "For every r.e. Pi_1 view theta, if Upsilon is a Pi_1 theorem of theta union B via a `D_SJAS` proof object p with Log_D_SJAS(p) <= #(theta)+1, then Upsilon is Good(1/2 #(theta))."
    :e-stability
    "For every r.e. Pi_1 view theta, if Upsilon is a Sigma_1 theorem of theta union B via a `D_SJAS` proof object p with Log_D_SJAS(p) <= #(theta)+1, then Upsilon is Good(1/2 floor(Log_D_SJAS(p))-1)."
    :ea-stability
    "The selected `D_SJAS` configuration is quantitatively EA-stable because both preceding clauses hold with the recorded constants."}
   :proof-lemmas
   {:normed-open-branch-generalization
    {:status :proved-by-dsjas-rule-preservation
     :argument "Willard's Fact D.3 extends from ordinary semantic tableaux to `D_SJAS`: base tableau and quantifier rules are literal; bookkeeping and truth normalization erase to literal branches; selected extensions are bounded Delta_0 primitives or finite macros that preserve the scoped standard valuation; recursive proof/substitution atoms use the finite acyclic least-fixed-point semantics."}
    :size-to-u-height-bound
    {:status :proved-under-code-injectivity
     :source (:status dsjas-combined-size-lower-bound)
     :argument "ADR-0104 proves that every measured `D_SJAS` structural or citation object exposes the formula/function-symbol payload needed by the conventional tableaux encoding requirement. Formula-bearing structural trees give at least 24N+36J bits; combined citation objects give at least 18J bits, both above Willard's conservative 5J threshold."}
    :a-stability-contradiction
    {:status :proved
     :argument "Assume a short `D_SJAS` proof of a Pi_1 theorem Upsilon that fails Good(1/2 #theta). Add Reverse(Upsilon) to Z. The Normed(a,b) clause and Log_D_SJAS(p) <= #theta+1 place p below the generalized Fact D.3 threshold, yielding an open branch, contradicting that p is a closed proof."}
    :e-stability-contradiction
    {:status :proved
     :argument "Assume a short `D_SJAS` proof of a Sigma_1 theorem Upsilon that fails Good(1/2 floor(Log_D_SJAS(p))-1). Add Reverse(Upsilon) to Z. The same Normed(a,b) and size-to-U-height bounds yield a contradiction-free branch, contradicting closure."}
    :selfcons1-consequence
    {:status :proved
     :argument "With A-stability and E-stability established for the selected length measure, Willard's Theorem 5.9 applies to the Level-1 `SelfCons` statement for `IS#_{D_SJAS}(beta)`."}}
   :rule-family-preservation dsjas-quantitative-rule-family-preservation
   :standing-assumptions
   #{:injective-public-code-reading
     :trailing-zero-preservation
     :least-fixed-point-recursive-semantics
     :external-beta-pi-star-1-truth
     :standard-model-soundness-of-ugrounding-primitives}
   :open-obligations #{}})

(defn audit-dsjas-quantitative-ea-stability
  "Return the ADR-0108 quantitative EA-stability proof audit for `D_SJAS`."
  []
  dsjas-quantitative-ea-stability)
