(ns proflog.sjas-synthesis-guard
  "ADR-0142 criterion 8: a dataflow-independence guard for boundary proof-tuple
   synthesis.

   This is the direct, reusable lesson of the retracted ADR-0141 'independent'
   synthesis: it host-encoded the exact expected proof bytes before the
   core.logic query, so fresh variable *names* hid a host-selected answer.
   Independence is a DATAFLOW property -- every tuple component must be an unbound
   logic variable at the moment the proof relation is entered, so that the search
   (not the host) determines it -- not a naming property.

   Honest scope: this guard enforces the dataflow precondition for a synthesis
   run. It does not by itself synthesize a Theorem 2.3 closing tuple: that target
   is not yet checker-complete (its condition (B) and the Theorem 2.2 steps rest
   on the documented cut-free / tower-bound boundary, see
   proflog.sjas-theorem23-closure). The guard is the part of criterion 8 that is
   meaningful and enforceable today."
  (:require [clojure.core.logic :as l]))

(defn fresh-lvar?
  "True iff `x` is an (unbound) core.logic logic variable."
  [x]
  (l/lvar? x))

(defn host-ground-component?
  "True iff a tuple component is host-supplied data rather than a fresh logic
   variable to be determined by search."
  [x]
  (not (fresh-lvar? x)))

(defn dataflow-independent?
  "True iff every component of `tuple` is a fresh logic variable, i.e. the
   synthesis search -- not the host -- will determine the proof tuple."
  [tuple]
  (boolean (and (seq tuple) (every? fresh-lvar? tuple))))

(defn assert-dataflow-independent!
  "Throw if any component of `tuple` is host-ground before the proof relation is
   entered (the ADR-0141 failure mode). Return `tuple` when independent."
  [tuple]
  (let [seeded (filter host-ground-component? tuple)]
    (when (seq seeded)
      (throw (ex-info "Synthesis tuple has host-ground components before proof entry"
                      {:host-ground-count (count seeded)
                       :tuple-size (count tuple)}))))
  tuple)
