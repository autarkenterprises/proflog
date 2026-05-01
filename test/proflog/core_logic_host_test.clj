(ns proflog.core-logic-host-test
  (:require [clojure.core.logic :as l]
            [clojure.test :refer [deftest is]]
            [proflog.core-logic-host :as host]))

(deftest core-logic-host-is-auditable
  (let [{:keys [source source-kind artifact-id version]} (host/host-info)]
    (is (string? source))
    (is (re-find #"clojure/core/logic\.clj" source))
    (is (contains? #{:maven-jar :local-source :classpath-source} source-kind))
    (is (or (nil? artifact-id)
            (= "core.logic" artifact-id)))
    (is (or (nil? version)
            (re-find #"^\d+\.\d+\.\d+" version)))))

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

(deftest core-logic-vector-unification-preserves-host-semantics
  (let [{:keys [available? calls result]}
        (counted-vector-unify
          #(doall
             (l/run* [q]
               (l/fresh [x y z]
                 (l/== [1 x [2 y] z] [1 :a [2 :b] :c])
                 (l/== q [x y z])))))]
    (is (= '([:a :b :c]) result))
    (when available?
      (is (pos? calls)))))

(deftest core-logic-vector-unification-keeps-list-shells-on-list-path
  (let [{:keys [available? calls result]}
        (counted-vector-unify
          #(doall
             (l/run* [q]
               (l/fresh [tail]
                 (l/== (l/llist :a tail) (list :a :b))
                 (l/== q tail)))))]
    (is (= '((:b)) result))
    (when available?
      (is (zero? calls)))))
