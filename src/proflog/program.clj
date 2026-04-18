(ns proflog.program
  "Compiled-program helpers for the Procedure Call Rule."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer [== conde fresh lcons membero]]))

(defn lookup-clauseo
  "Find the compiled clause for `relation` in `program`.

   Returns the clause parameters, body, and precomputed NNF negation of the
   body. The compiled program keeps a list view specifically so this lookup can
   remain relational inside the kernel."
  [program relation params body negated-body]
  (fresh [language clauses clause-list]
    (== {:language language
         :clauses clauses
         :clause-list clause-list}
        program)
    (membero {:relation relation
              :params params
              :body body
              :negated-body negated-body}
             clause-list)))

(defn bind-argso
  "Create an environment mapping formal parameter noms to actual argument terms."
  [params args env]
  (conde
    [(== '() params) (== '() args) (== '() env)]
    [(fresh [param param-rest arg arg-rest env-rest]
       (== (lcons param param-rest) params)
       (== (lcons arg arg-rest) args)
       (== (lcons [param arg] env-rest) env)
       (bind-argso param-rest arg-rest env-rest))]))

(defn call-clauseo
  "Resolve an atomic procedure call against a compiled program."
  [program atom env body negated-body]
  (fresh [relation args params]
    (== (lcons 'app (lcons relation args)) atom)
    (lookup-clauseo program relation params body negated-body)
    (bind-argso params args env)))
