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
     first-order})

(def ^:private unresolved-layer-symbols
  "Profile wrappers and SJAS-specific staging witnesses whose role depends on
   the concrete profiled form. Generic sidecars are classified separately as
   excluded from SJAS proof-predicate certificates."
  '#{profiled
     willard-sjas-tableau0
     willard-sjas-level1
     willard-sjas-arithmetic
     willard-sjas-fact
     willard-sjas-axiom-member
     willard-sjas-theorem-code
     willard-sjas-proof-check
     willard-sjas-subst-code
     willard-sjas-subst-proof-check
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
    (classify-set excluded-answer-overlay-symbols
                  {:status :excluded
                   :aspect :answer-overlay
                   :obligation "Reject query-entry and residual-deferral proof constructors from SJAS proof-predicate certificates."})
    (classify-set excluded-layer-symbols
                  {:status :excluded
                   :aspect :generic-sidecar
                   :obligation "Reject generic optimized sidecar closure from SJAS proof-predicate certificates."})
    (classify-set unresolved-layer-symbols
                  {:status :unresolved
                   :aspect :optimized-or-profile-layer
                   :obligation "Prove wrapper/layer irrelevance, provide bounded expansion, or replace with object-level rules."})))

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

(defn classify-proof-symbol
  "Return the Track 2a classification for a proof symbol, or nil when the symbol
   is not part of the current SJAS proof-certificate alphabet."
  [sym]
  (get proof-symbol-classifications sym))

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
