(ns proflog.fitting.namespace-closure-test
  "ADR-0149 Test Obligation 1. The subproject implements only Fitting's
  Proflog, so its source namespaces may require only Clojure itself,
  core.logic, and each other. Because every `proflog.fitting.*` source file is
  checked and Clojure and core.logic require nothing outside `clojure.*`, this
  per-file check bounds the transitive requires as well."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(def ^:private expected-namespaces
  "The namespaces the extraction is made of (ADR-0149, Decision §2;
  `proflog.fitting`, the entry point, since revision 7)."
  '#{proflog.fitting
     proflog.fitting.syntax
     proflog.fitting.tableau
     proflog.fitting.query
     proflog.fitting.programs})

(defn- source-files
  "Every source file of the subproject."
  []
  (->> (file-seq (io/file "src"))
       (filter #(str/ends-with? (.getName ^java.io.File %) ".clj"))))

(defn- ns-form
  "The first form of a source file, which must be its `ns` form."
  [file]
  (with-open [r (java.io.PushbackReader. (io/reader file))]
    (read r)))

(defn- required-libs
  "Library symbols named in the :require and :use clauses of an ns form."
  [[_ _ & clauses]]
  (for [clause clauses
        :when (and (seq? clause) (#{:require :use} (first clause)))
        spec (rest clause)]
    (if (sequential? spec) (first spec) spec)))

(defn- allowed-lib? [lib]
  (let [s (str lib)]
    (or (str/starts-with? s "clojure.")
        (= s "proflog.fitting")
        (str/starts-with? s "proflog.fitting."))))

(deftest the-extraction-has-its-namespaces
  (is (= expected-namespaces
         (set (map (comp second ns-form) (source-files))))))

(deftest every-require-is-clojure-core-logic-or-fitting
  (doseq [file (source-files)
          :let [form (ns-form file)]]
    (testing (str (second form))
      (doseq [lib (required-libs form)]
        (is (allowed-lib? lib)
            (str lib " is outside clojure.*, clojure.core.logic* and proflog.fitting.*"))))))
