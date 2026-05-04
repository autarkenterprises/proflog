(ns proflog.minikanren-constraints
  "Project-local miniKanren constraint overlay.

   Core.logic exposes the lower-level constraint hooks needed by the
   faster-miniKanren-style relations used in ADR-36, but not public
   `symbolo`, `numbero`, or `absento` vars. This namespace keeps those
   relations local to Proflog and builds them only from public core.logic
   constraint machinery."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer [!= predc treec]]))

(defn symbolo
  "Constraint relation requiring `x` to be a Clojure symbol.

   The check delays while `x` is still a logic variable and runs once the
   variable is sufficiently instantiated."
  [x]
  (predc x symbol? 'symbolo))

(defn numbero
  "Constraint relation requiring `x` to be a Clojure number.

   The check delays while `x` is still a logic variable and runs once the
   variable is sufficiently instantiated."
  [x]
  (predc x number? 'numbero))

(defn absento
  "Constraint relation requiring `target` to be absent from `term`.

   This is intentionally a thin overlay on core.logic `treec`: every discovered
   tree node is constrained with disequality against `target`, and open subterms
   keep delayed constraints that are checked when more structure appears."
  [target term]
  (treec term
         (fn [node] (!= target node))
         'absento))
