(ns proflog.fitting.query
  "An optional parallel runner for Definition 6.1 (ADR-0149 revision 7).

  `proflog.fitting.tableau/queryo` with its status fresh already races the
  two sides of a query, by interleaving them on one thread. This runner
  instead runs `queryo` with the status fixed to :succeeds on one thread and
  to :fails on another, takes the first closed tableau, and cancels the other
  search. It adds what a single `run` cannot:

  - the two sides on two cores;
  - a wall-clock timeout;
  - `:both`, which waits for both sides and throws if both close. Lemma 7.5
    with Theorem 7.2 says that cannot happen, so it is a soundness probe.

  The runner decides nothing about what is a closed tableau. Its answers are
  the relation's, and `proflog.fitting/ask` re-checks every certificate.

  The searches are stepped by `first-answer`, a copy of core.logic's own
  stream loop that also stops when its thread is interrupted. Cancellation
  is therefore possible, and a cancelled search leaves no thread running."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :as logic :refer [fresh]]
            [proflog.fitting.syntax :as syntax]
            [proflog.fitting.tableau :as tableau]))

(defn- first-answer
  "The first answer of a core.logic stream, or nil if it has none. This is
  the loop that `run` performs through `take*`: a stream is nil (no
  answers), a Choice (an answer and the rest), or a thunk (call it to go on).
  It also stops, with an InterruptedException, if the thread is
  interrupted."
  [stream]
  (loop [s stream]
    (when (.isInterrupted (Thread/currentThread))
      (throw (InterruptedException. "search cancelled")))
    (cond
      (nil? s) nil
      (instance? clojure.core.logic.Choice s) (:a s)
      (fn? s) (recur (s))
      :else s)))

(defn- side
  "The first closed tableau showing that A has the given status (Definition
  6.1), with at most `fuel` choices on each branch if fuel is given, or nil
  if the search ends with none. Runs on the calling thread and stops if the
  thread is interrupted. The stream is the one core.logic's `run` would
  build."
  [P A status fuel]
  (let [goal (fresh [cert]
               (if fuel
                 (tableau/fueled-queryo P A fuel status cert)
                 (tableau/queryo P A status cert))
               (logic/reifyg cert))]
    (some-> (first-answer (goal (logic/tabled-s true {:reify-vars true})))
            syntax/pretty)))

(defn race
  "Run the query A, a ground literal of L, against the program P: the two
  sides of `queryo` on two threads. Returns

      {:status :succeeds, :certificate <closed P-tableau for ¬A>}
      {:status :fails,    :certificate <closed P-tableau for A>}
      {:status :unresolved, :reason :exhausted | :timeout}

  `:unresolved` also carries `:fuel` when a bound was given. Options:

  - `:fuel`       the bound for `fueled-queryo`; with none, a side with no
                  closed tableau searches until it is cancelled;
  - `:timeout-ms` the wall-clock bound for the whole query;
  - `:both`       wait for both sides to end, and throw if both close. With
                  no `:fuel` and no `:timeout-ms`, a ⊥ query or the losing
                  side of a query never ends."
  [P A {:keys [fuel timeout-ms both]}]
  (let [results (java.util.concurrent.LinkedBlockingQueue.)
        search (fn [status]
                 (future
                   (try
                     (.put results [status (side P A status fuel)])
                     (catch InterruptedException _ nil))))
        searches [(search :succeeds) (search :fails)]
        deadline (when timeout-ms (+ (System/currentTimeMillis) timeout-ms))
        next-result (fn []
                      (if deadline
                        (.poll results (max 0 (- deadline (System/currentTimeMillis)))
                               java.util.concurrent.TimeUnit/MILLISECONDS)
                        (.take results)))
        unresolved (fn [reason]
                     (cond-> {:status :unresolved :reason reason}
                       fuel (assoc :fuel fuel)))]
    (try
      (loop [pending 2 found nil]
        (if (zero? pending)
          (or found (unresolved :exhausted))
          (let [[status cert :as r] (next-result)]
            (cond
              (nil? r)
              (or found (unresolved :timeout))

              (and cert found)
              (throw (ex-info "a query both succeeds and fails: the implementation is unsound"
                              {:query A :found found :also [status cert]}))

              (and cert (not both))
              {:status status :certificate cert}

              cert
              (recur (dec pending) {:status status :certificate cert})

              :else
              (recur (dec pending) found)))))
      (finally
        (run! future-cancel searches)))))
