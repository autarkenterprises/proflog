(ns proflog.diagnostics.proof-trace
  "Read-only diagnostic renderer for Proflog kernel proof objects.

   Consumes completed proof terms after search; never affects proof acceptance
   or query answers."
  (:require [clojure.string :as str]
            [proflog.proof :as proof]))

(def known-step-tags
  '#{close conj split savefml
      univ witness once-univ
      eq-step neq-store neq-close refl-close
      pos-call neg-call pos-call-guarded-alt neg-call-guarded-alt
      pos-call-alt neg-call-alt profiled guarded-call guarded-call-alt})

(def branch-rule-tags '#{conj split})
(def quantifier-tags '#{univ witness once-univ})
(def equality-tags '#{eq-step neq-store})
(def procedure-call-tags
  '#{pos-call neg-call pos-call-alt neg-call-alt
      pos-call-guarded-alt neg-call-guarded-alt guarded-call guarded-call-alt})

(def closure-tags
  '#{close neq-close refl-close})

(defn- step-label
  [tag]
  (name tag))

(defn- closure-reason
  [tags]
  (cond
    (some '#{close} tags) :contradictory-literals
    (some '#{neq-close} tags) :disequality-contradiction
    (some '#{refl-close} tags) :procedure-refutation
    :else :unknown))

(defn- walk-proof-steps
  "Flatten recognized proof tags into ordered diagnostic steps."
  [proof]
  (let [tags (filter known-step-tags (proof/collect-steps proof))]
    (map-indexed (fn [idx tag]
                   {:index (inc idx)
                    :tag tag
                    :label (step-label tag)
                    :kind (cond
                            (branch-rule-tags tag) :branch-rule
                            (= 'savefml tag) :literal-save
                            (closure-tags tag) :closure
                            (quantifier-tags tag) :quantifier
                            (equality-tags tag) :equality
                            (procedure-call-tags tag) :procedure-call
                            (= 'profiled tag) :profile
                            :else :other)})
                 tags)))

(defn proof-trace-edn
  "Return a structured diagnostic trace for `proof`, or a status map when the
  artifact cannot be rendered."
  [proof]
  (cond
    (nil? proof)
    {:status :insufficient-data
     :reason "Proof artifact is nil."}

    (not (coll? proof))
    {:status :unsupported
     :reason "Proof artifact is not a collection."}

    :else
    (let [steps (vec (walk-proof-steps proof))
          tags (map :tag steps)]
      (if (empty? steps)
        {:status :insufficient-data
         :reason "Proof artifact contains no recognized tableau step tags."}
        (cond-> {:status :ok
                 :steps steps
                 :step-count (count steps)}
          (some closure-tags tags)
          (assoc :closure {:reason (closure-reason tags)
                           :tags (vec (filter closure-tags tags))})
          (some '#{split} tags)
          (assoc :branching true)
          (some '#{conj} tags)
          (assoc :alpha-expansion true))))))

(defn format-proof-trace
  "Render a structured proof trace as stable human-readable text."
  [trace]
  (case (:status trace)
    :ok
    (str/join
      "\n"
      (concat
        [(str "status: ok")
         (str "steps: " (:step-count trace))]
        (when (:branching trace) ["branching: true"])
        (when (:alpha-expansion trace) ["alpha-expansion: true"])
        (when-let [{:keys [reason tags]} (:closure trace)]
          [(str "closure: " (name reason))
           (str "closure-tags: " (str/join "," (map name tags)))])
        (map (fn [{:keys [index label kind]}]
               (str index ". " label " [" (name kind) "]"))
             (:steps trace))))

    (str "status: " (name (:status trace))
         (when-let [reason (:reason trace)]
           (str "\nreason: " reason)))))

(defn render-proof-trace
  "Convenience entry point: EDN trace plus formatted text."
  [proof]
  (let [trace (proof-trace-edn proof)]
    {:trace trace
     :text (format-proof-trace trace)}))
