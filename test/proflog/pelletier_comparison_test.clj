(ns proflog.pelletier-comparison-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.pelletier-test :as pelletier]))

(def adr-0024-baseline-too-slow-ids
  [24 25 26 27 28 29 30 31 32 34 36 37 38 41 43 44 45 46])

(def promoted-first-order-ids
  [25 30 31 36 41])

(def remaining-after-first-tranche-ids
  [24 26 27 28 29 32 34 37 38 43 44 45 46])

(def comparison-results
  {24 {:alphaleantap-e :closes, :legacy-ep :no-proof, :greenfield-first-order :timeout}
   25 {:alphaleantap-e :closes, :legacy-ep :no-proof, :greenfield-first-order :closes}
   26 {:alphaleantap-e :timeout, :legacy-ep :no-proof, :greenfield-first-order :timeout}
   27 {:alphaleantap-e :closes, :legacy-ep :no-proof, :greenfield-first-order :timeout}
   28 {:alphaleantap-e :closes, :legacy-ep :no-proof, :greenfield-first-order :timeout}
   29 {:alphaleantap-e :closes, :legacy-ep :no-proof, :greenfield-first-order :timeout}
   30 {:alphaleantap-e :closes, :legacy-ep :no-proof, :greenfield-first-order :closes}
   31 {:alphaleantap-e :closes, :legacy-ep :no-proof, :greenfield-first-order :closes}
   32 {:alphaleantap-e :closes, :legacy-ep :no-proof, :greenfield-first-order :timeout}
   34 {:alphaleantap-e :timeout, :legacy-ep :no-proof, :greenfield-first-order :timeout}
   36 {:alphaleantap-e :closes, :legacy-ep :no-proof, :greenfield-first-order :closes}
   37 {:alphaleantap-e :closes, :legacy-ep :no-proof, :greenfield-first-order :timeout}
   38 {:alphaleantap-e :timeout, :legacy-ep :no-proof, :greenfield-first-order :timeout}
   41 {:alphaleantap-e :closes, :legacy-ep :no-proof, :greenfield-first-order :closes}
   43 {:alphaleantap-e :timeout, :legacy-ep :no-proof, :greenfield-first-order :timeout}
   44 {:alphaleantap-e :closes, :legacy-ep :no-proof, :greenfield-first-order :timeout}
   45 {:alphaleantap-e :timeout, :legacy-ep :no-proof, :greenfield-first-order :timeout}
   46 {:alphaleantap-e :timeout, :legacy-ep :no-proof, :greenfield-first-order :timeout}})

(deftest ^:pelletier-comparison comparison-report-covers-adr-0024-baseline
  (testing "every original too-slow Pelletier problem has a three-prover result"
    (is (= (set adr-0024-baseline-too-slow-ids)
           (set (keys comparison-results))))
    (doseq [[id result] comparison-results]
      (is (= #{:alphaleantap-e :legacy-ep :greenfield-first-order}
             (set (keys result)))
          (str "Pelletier Problem " id " should have all prover results")))))

(deftest ^:pelletier-comparison promoted-tranche-matches-comparison-and-catalog
  (testing "the first greenfield first-order closures are the promoted tranche"
    (is (= (set promoted-first-order-ids)
           (set (keep (fn [[id result]]
                        (when (= :closes (:greenfield-first-order result))
                          id))
                      comparison-results)))))
  (testing "the Pelletier catalog agrees with the ADR-0024 tranche split"
    (is (= (set promoted-first-order-ids)
           (set (filter (set promoted-first-order-ids)
                        pelletier/ported-passing-ids))))
    (is (= (set remaining-after-first-tranche-ids)
           (set pelletier/ported-too-slow-ids)))))
