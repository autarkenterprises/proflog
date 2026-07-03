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
  (:require [clojure.core.logic :as l]
            [clojure.core.logic.protocols :as logic-protocols]))

(defn fresh-lvar?
  "True iff `x` is syntactically a core.logic logic variable.

   This is sufficient for checking a host-built tuple before it enters a logic
   run. For checks inside a live run, use `state-fresh-lvar?`, which walks the
   current substitution first; an lvar object can already be bound."
  [x]
  (l/lvar? x))

(defn state-fresh-lvar?
  "True iff `x` is still an unbound logic variable in the live core.logic `state`.

   ADR-0147 audit correction: checking only the host object shape is not enough
   once a synthesis tuple is inside the relation. A tuple component that is an
   lvar object can already have been bound by earlier goals, which is exactly
   the dataflow distinction the synthesis guard is meant to enforce."
  [state x]
  (l/lvar? (logic-protocols/walk state x)))

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

(defn dataflow-independent-in-state?
  "True iff every component of `tuple` is still fresh in the live logic `state`.

   Use this at the proof-relation entry point. It rejects both host-ground data
   and lvars that were fresh when the tuple was constructed but have already
   been bound by earlier goals."
  [state tuple]
  (boolean (and (seq tuple) (every? #(state-fresh-lvar? state %) tuple))))

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

(defn assert-dataflow-independent-in-state!
  "Throw if any component of `tuple` is not fresh in the live logic `state`."
  [state tuple]
  (let [seeded (remove #(state-fresh-lvar? state %) tuple)]
    (when (or (empty? tuple) (seq seeded))
      (throw (ex-info "Synthesis tuple has non-fresh components at proof entry"
                      {:non-fresh-count (count seeded)
                       :tuple-size (count tuple)}))))
  tuple)
