(ns proflog.core-logic-host-probe
  (:gen-class)
  (:require [proflog.core-logic-host :as host]))

(defn -main
  [& _args]
  (println (host/format-host-info (host/host-info))))

