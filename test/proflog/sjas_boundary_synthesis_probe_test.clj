(ns proflog.sjas-boundary-synthesis-probe-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [proflog.sjas-boundary-synthesis-probe :as probe]
            [proflog.sjas-correspondence :as correspondence]
            [proflog.willard-sjas :as sjas]))

(deftest boundary-proof-search-synthesis-probe-exposes-all-workstream-b-variants
  (testing "ADR-0139 exposes every Workstream B negative variant as a synthesis target"
    (let [variants (probe/boundary-proof-search-synthesis-variants)]
      (is (= #{:total-multiplication
               :xtab-or-lem-axiom
               :tab-2-or-stronger}
             variants))
      (doseq [variant variants
              :let [plan (probe/boundary-proof-search-synthesis-plan variant)]]
        (is (= variant (:variant plan)))
        (is (= :proof-search-synthesis (:evidence-kind plan)))
        (is (= :open (:proof-search-synthesis-status plan)))
        (is (true? (:durable-log-required? plan)))
        (is (= correspondence/boundary-final-evidence-obligations
               (:remaining-obligations plan)))))))

(deftest boundary-proof-search-synthesis-report-screens-ordinary-selfcons-citation
  (testing "ADR-0139 screens a durable synthesis result and refuses ordinary Group-3 citation as final evidence"
    (let [durable-log-path "test-runs/adr-0139-total-multiplication-synthesis.log"
          report (probe/boundary-proof-search-synthesis-report
                   :total-multiplication
                   {:system-opts {:profile :willard-sjas-level1
                                  :depth 3}
                    :fuel 320
                    :proof-limit 1
                    :synthesized-proof-code (sjas/proof-certificate
                                               'sjas-axiom)
                    :durable-log-path durable-log-path})
          candidate (:candidate report)
          screen (:screen report)
          validation (:validation report)]
      (is (= :total-multiplication (:variant report)))
      (is (= :proof-search-synthesis (:evidence-kind report)))
      (is (= :provided (:synthesis-status report)))
      (is (= :ordinary-citation-rejected (:result report)))
      (is (= (sjas/proof-certificate 'sjas-axiom)
             (:proof-code candidate)))
      (is (= :sjas-axiom (:certificate-kind candidate)))
      (is (= durable-log-path (:durable-log-path candidate)))
      (is (= (get-in report [:target :system-code])
             (:system-code candidate)))
      (is (= (get-in report [:target :group-three-code])
             (:selfcons-code candidate)))
      (is (= (get-in report [:target :target-code])
             (:target-code candidate)))
      (is (= :rejected (:result screen)))
      (is (= #{:ordinary-selfcons-citation}
             (:reasons screen)))
      (is (nil? validation))
      (is (= :not-run (:validation-status report)))
      (is (= #{}
             (:completed-obligations report)))
      (is (= correspondence/boundary-final-evidence-obligations
             (:remaining-obligations report))))))

(deftest boundary-proof-search-synthesis-cli-defaults-to-plans
  (testing "no-argument CLI use prints plans instead of launching every live synthesis query"
    (let [output (with-out-str (probe/-main))]
      (is (str/includes? output
                         "lein probe-proflog-sjas-boundary-synthesis <variant>..."))
      (is (str/includes? output ":durable-log-required? true"))
      (is (str/includes? output ":plans"))
      (is (str/includes? output ":total-multiplication")))))
