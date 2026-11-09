(ns proflog.fitting
  "Fitting's Proflog, from M. Fitting, \"Tableaus for Logic Programming\"
  (1993). This namespace is the entry point.

  `ask` takes the three things §2 says a use of the language consists of:
  - a language L, given by its symbols;
  - a program written in L;
  - a query, which is a ground literal of L.

      (ask '{:constants [0] :functions {s 1} :relations {win 1}}
           '[(<- (win x) (exists y (and (or (= x (s y)) (= x (s (s y))))
                                        (not (win y)))))]
           '(win (s (s (s 0)))))
      ;; => {:status :fails, :certificate [:call (win (s (s (s 0)))) ...]}

  The namespaces underneath:
  - `proflog.fitting.syntax`: §2, the language, programs and queries;
  - `proflog.fitting.tableau`: §4–§7, the relation `tableauo`, and
    Definition 6.1, the relation `queryo`;
  - `proflog.fitting.query`: an optional parallel runner with timeouts;
  - `proflog.fitting.programs`: the paper's programs, as text."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer [run fresh ==]]
            [proflog.fitting.query :as query]
            [proflog.fitting.syntax :as syntax]
            [proflog.fitting.tableau :as tableau]))

(defn- accepted?
  "Does `tableauo` accept cert as a closed P-tableau for the sentence X?"
  [P X cert]
  (let [P (syntax/with-sentence P X)]
    (boolean (seq (run 1 [q] (tableau/tableauo P X cert))))))

(defn- checked
  "The answer, after `tableauo` accepts its certificate. Every search runs
  the same rules as `tableauo`, so a rejection would be a defect in this
  implementation, never an answer."
  [P A {:keys [status certificate] :as answer}]
  (when certificate
    (let [X (if (= status :succeeds) (list 'not A) A)]
      (when-not (accepted? P X certificate)
        (throw (ex-info "the relation rejected its own certificate"
                        {:query A :answer answer})))))
  answer)

(defn- run-queryo
  "`queryo`, or `fueled-queryo` when fuel is given, with core.logic's `run`
  on the calling thread. The two sides of the query are interleaved, and
  the first closed tableau on either side answers. If the search ends with
  none, the answer is unresolved."
  [P A fuel]
  (if-let [[status cert] (first (run 1 [q]
                                  (fresh [status cert]
                                    (if fuel
                                      (tableau/fueled-queryo P A fuel status cert)
                                      (tableau/queryo P A status cert))
                                    (== q [status cert]))))]
    {:status status :certificate (syntax/pretty cert)}
    (cond-> {:status :unresolved :reason :exhausted}
      fuel (assoc :fuel fuel))))

(defn ask
  "Ask the query A of the program written in the language L (Definition
  6.1). Returns

      {:status :succeeds, :certificate <closed P-tableau for ¬A>}
      {:status :fails,    :certificate <closed P-tableau for A>}
      {:status :unresolved, :reason :exhausted | :timeout}

  - `language` is `{:constants [c …] :functions {f n …} :relations {R n …}}`
    (§2). Equality is always a relation symbol of L.
  - `program` is a list of clauses `(<- (R x1 … xn) ϕ)` (Definition 2.1).
  - `query` is a ground literal of L: `(R t1 … tn)` or `(not (R t1 … tn))`.

  The certificate is the closed tableau, and `tableauo` has accepted it.

  With no options, the search has no bound and no timeout, as in the paper.
  \"It is possible for a query A to neither succeed nor fail\" (§6), and
  then the search may never end; it ends `:unresolved :exhausted` only when
  there is nothing left to try. Options:

  - `:fuel n`       at most n choices on each branch (`fueled-queryo`), so
                    the search ends; `:unresolved` answers carry `:fuel n`;
  - `:parallel`     the two sides on two threads (`proflog.fitting.query`);
  - `:timeout-ms`   a wall-clock bound (the parallel runner);
  - `:both`         wait for both sides, and throw if both close (the
                    parallel runner)."
  ([language program query] (ask language program query {}))
  ([language program query {:keys [fuel parallel timeout-ms both] :as opts}]
   (let [L (syntax/language language)
         P (syntax/program L program)]
     (when-not (syntax/query? L query)
       (throw (ex-info "a query is a ground literal of L (§2)" {:query query})))
     (checked P query
              (if (or parallel timeout-ms both)
                (query/race P query opts)
                (run-queryo P query fuel))))))

(defn certificate?
  "Does cert describe a closed P-tableau for the sentence X of L, where P is
  the program written in the language L? This is `tableauo` in check mode,
  for a certificate written by hand or kept from an earlier answer."
  [language program X cert]
  (let [L (syntax/language language)]
    (accepted? (syntax/program L program) X cert)))
