;; Fitting's Proflog, as in M. Fitting, "Tableaus for Logic Programming"
;; (1993; LPTableaus.pdf at the repository root). See ADR-0149.
;;
;; This subproject is deliberately isolated from the root project: it uses
;; stock Clojure and stock core.logic from Maven, not the root project's
;; vendored core.logic overlay, and nothing outside `proflog.fitting.*` is on
;; its classpath. That isolation is what makes "only and exactly Fitting"
;; checkable (see test/proflog/fitting/namespace_closure_test.clj).
(defproject proflog-fitting "0.1.0-SNAPSHOT"
  :description "Fitting's Proflog (§2–§7) as one pure core.logic relation"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/core.logic "1.0.1"]]
  :source-paths ["src"]
  :test-paths ["test"]
  ;; `lein test` runs the default suite; `lein test :slow` runs the deeper
  ;; prove-mode cases; `lein test :all` runs everything.
  :test-selectors {:default (complement :slow)
                   :slow :slow
                   :all (constantly true)}
  ;; Prove-mode runs that time out are reported as :unresolved by the query
  ;; API; tests never need a large stack.
  :jvm-opts ["-Xmx2g"])
