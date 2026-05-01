(ns proflog.core-logic-vector-probe
  "ADR-0032 probe for the vendored core.logic vector unification path."
  (:gen-class)
  (:require [clojure.core.logic :as l]
            [clojure.pprint :as pp]
            [proflog.core-logic-host :as host]
            [proflog.list-kernel-matrix-probe :as matrix]))

(defn- vector-unify-var
  []
  (ns-resolve 'clojure.core.logic 'unify-with-vector*))

(defn- vector-counter-var
  []
  (ns-resolve 'clojure.core.logic '*proflog-adr32-vector-unify-counter*))

(defn- counted-vector-unify
  [f]
  (if-let [counter-var (vector-counter-var)]
    (let [calls (atom 0)
          result (with-bindings {counter-var calls}
                   (f))]
      {:available? true
       :calls @calls
       :result result})
    {:available? (boolean (vector-unify-var))
     :calls 0
     :result (f)}))

(defn- host-vector-shell
  []
  (counted-vector-unify
    #(doall
       (l/run* [q]
         (l/fresh [x y z]
           (l/== [1 x [2 y] z] [1 :a [2 :b] :c])
           (l/== q [x y z]))))))

(defn- ordinary-list-shell
  []
  (counted-vector-unify
    #(doall
       (l/run* [q]
         (l/fresh [tail]
           (l/== (l/llist :a tail) (list :a :b))
           (l/== q tail))))))

(defn- matrix-case
  [case-id]
  (counted-vector-unify
    #(matrix/run-case (keyword case-id))))

(defn run-probe
  "Run a fast host vector shell and, when `case-id` is supplied, one matrix case."
  ([] (run-probe nil))
  ([case-id]
   (let [vector-shell (host-vector-shell)
         list-shell (ordinary-list-shell)
         selected-host (select-keys (host/host-info)
                                    [:source :source-kind :group-id
                                     :artifact-id :version :marker])]
     (cond-> {:probe :core-logic-vector-unification
              :host selected-host
              :vector-path-available? (:available? vector-shell)
              :host-vector-shell {:result (:result vector-shell)
                                  :expected '([:a :b :c])
                                  :vector-unify-calls (:calls vector-shell)}
              :ordinary-list-shell {:result (:result list-shell)
                                    :expected '((:b))
                                    :vector-unify-calls (:calls list-shell)}}
       case-id
       (assoc :matrix-case
              (let [result (matrix-case case-id)]
                {:case-id (keyword case-id)
                 :vector-unify-calls (:calls result)
                 :result (:result result)}))))))

(defn -main
  [& [case-id]]
  (pp/pprint (run-probe case-id))
  (shutdown-agents))
