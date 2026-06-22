(ns proflog.sjas-boundary-completion-probe
  "Replayable ADR-0141 extended probes for SJAS boundary completion.

   The focused tests keep long proof-search checks off the default path. This
   namespace gives `test-runs/` commands a stable entrypoint for those probes so
   they can be resumed and compared without shell-quoting Clojure expressions."
  (:require [proflog.kernel.willard-sjas-profile :as sjas-profile]
            [proflog.willard-sjas :as sjas]
            [proflog.willard-sjas-code :as sjas-code]))

(defn tab2-proof-list-valid-report
  "Run the decoded Tab-2 proof-list validator on ADR-0141's Rank-2 fixture."
  []
  (let [system (sjas/tab2-complete-system {})
        fixture (sjas/tab2-rank2-reuse-fixture system)
        proof-list-bytes (apply list
                                (sjas-code/code-term-bytes
                                  (:proof-list-code fixture)))
        valid? (sjas-profile/tab2-proof-list-valid?
                 (:program system)
                 (:system-code system)
                 (:target-code fixture)
                 proof-list-bytes
                 420)]
    {:probe :adr0141-tab2-proof-list-valid
     :profile (:profile system)
     :target-byte-count (count (sjas-code/code-term-bytes
                                 (:target-code fixture)))
     :proof-list-byte-count (count proof-list-bytes)
     :result valid?}))

(defn -main
  "Run one ADR-0141 extended probe by name."
  [& args]
  (case (first args)
    "tab2-proof-list-valid"
    (do
      (prn (tab2-proof-list-valid-report))
      (shutdown-agents))

    (do
      (binding [*out* *err*]
        (println "Usage: lein run -m proflog.sjas-boundary-completion-probe tab2-proof-list-valid"))
      (shutdown-agents)
      (System/exit 2))))
