(ns proflog.fitting.purity-test
  "ADR-0149 Test Obligation 5 (purity). The tableau relation uses only the
  relational core of core.logic: `==`, `conde`, `fresh` and relations built
  from them. No nominal logic, `!=`, finite domains, committed choice
  (`conda`, `condu`, `onceo`) or `project` may appear, so that the same
  relation can prove and check, and its answers do not depend on goal
  order."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]))

(def ^:private relation-source "src/proflog/fitting/tableau.clj")

(defn- forms [file]
  (with-open [r (java.io.PushbackReader. (io/reader file))]
    (doall (take-while #(not= ::eof %) (repeatedly #(read r false ::eof))))))

(defn- symbols-in [forms]
  (set (filter symbol? (tree-seq coll? seq forms))))

(def ^:private forbidden
  '#{conda condu onceo project != copy-term pred fnc defnc featurec
     nom tie hash fresh-nom everyg partial-map lvaro nonlvaro})

(deftest the-relation-uses-only-the-pure-subset
  (let [syms (symbols-in (forms relation-source))]
    (doseq [s forbidden]
      (is (not (contains? syms s)) (str s " appears in " relation-source)))
    (testing "and no namespace-qualified escape to them"
      (is (empty? (filter #(and (namespace %) (contains? forbidden (symbol (name %)))) syms))))))

(deftest the-relation-requires-only-core-logic
  (let [[_ _ & clauses] (first (forms relation-source))
        requires (for [c clauses :when (and (seq? c) (= :require (first c)))
                       spec (rest c)]
                   (if (sequential? spec) (first spec) spec))]
    (is (= '[clojure.core.logic] (vec requires)))))

(deftest the-rules-mention-no-fuel
  ;; ADR-0149 revision 7: Fitting's rules have no step bound, so the
  ;; construction that transcribes them has none. A bound is a wrapper that
  ;; ties the same construction through finitely many stages.
  (let [construction (first (filter #(and (seq? %) (= 'construction (second %)))
                                    (forms relation-source)))]
    (is (some? construction) "tableau.clj defines the construction")
    (is (empty? (filter #(re-find #"(?i)fuel" (name %)) (symbols-in [construction]))))))
