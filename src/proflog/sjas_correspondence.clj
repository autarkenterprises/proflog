(ns proflog.sjas-correspondence
  "Executable audit helpers for the ADR-0073 SJAS correspondence program.

   These helpers do not prove the correspondence theorem. Their job is narrower:
   keep the set of encoded SJAS proof-certificate symbols visible, classify each
   symbol against the current Track 2a relevance matrix, and make tests fail
   when a new encoded constructor appears without an explicit correspondence
   obligation."
  (:require [proflog.proof :as proof]
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
     sjas-axiom})

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
     willard-sjas-arithmetic
     willard-sjas-axiom-member
     willard-sjas-theorem-code
     willard-sjas-proof-check
     willard-sjas-subst-code
     willard-sjas-subst-source-result
     willard-sjas-subst-exprf
     willard-sjas-subst-proof-check})

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
