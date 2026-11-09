(ns proflog.fitting.query
  "Definition 6.1 of M. Fitting, \"Tableaus for Logic Programming\" (1993):

      A query A succeeds with program P if there is a closed P-tableau for
      ¬A. A query A fails with program P if there is a closed P-tableau for
      A.

  \"Notice that it is possible for a query A to neither succeed nor fail
  with program P\". By Theorem 7.2 that happens exactly when s_P(A) = ⊥.
  No procedure can recognize ⊥ in general, because the true and false sets
  are only semi-decidable.

  Search control
  --------------
  This namespace only decides how the relation
  `proflog.fitting.tableau/tableauo` is run. It never decides what is a
  closed tableau.

  - Each side (¬A for success, A for failure) is one prove-mode run of the
    relation at fuel `:max-fuel`. Interleaving search finds a closed tableau
    in roughly the same time at any sufficient fuel. A fuel that is too low
    must instead be exhausted before the answer can be \"none\", so there is
    no iterative deepening.
  - `query-status` runs the two sides in parallel and takes the first closed
    tableau. The other side is then cancelled. Lemma 7.5 with Theorem 7.2
    says at most one side can close. `:both` waits for both sides instead,
    and throws if both close, because that would make this implementation
    unsound.
  - `:timeout-ms` bounds the wall-clock time of a search.
  - Every certificate is re-checked by the same relation in check mode before
    it is reported.

  The runs are stepped by `first-answer`, a copy of core.logic's own stream
  loop that also stops when its thread is interrupted. Cancellation is
  therefore possible, and a cancelled search leaves no thread running."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :as logic :refer [run lvar fresh]]
            [proflog.fitting.syntax :as syntax]
            [proflog.fitting.tableau :as tableau]))

(def default-max-fuel
  "Enough for every query of the paper's programs in this suite."
  16)

;;; ---------------------------------------------------------------------
;;; Running the relation

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

(defn closed-tableau
  "A certificate for a closed P-tableau for the sentence X whose branches use
  at most n units of fuel, or nil if there is none. Runs on the calling
  thread and stops if the thread is interrupted. The stream is the one
  core.logic's `run` would build."
  [P X n]
  (let [P (syntax/with-sentence P X)
        goal (fresh [cert]
               (tableau/tableauo P X (tableau/fuel n) cert)
               (logic/reifyg cert))]
    (some-> (first-answer (goal (logic/tabled-s true {:reify-vars true})))
            syntax/pretty)))

(defn certificate?
  "Does the relation accept cert as a closed P-tableau for the sentence X?
  Check mode: cert is ground and the fuel is left fresh."
  [P X cert]
  (let [P (syntax/with-sentence P X)]
    (boolean (seq (run 1 [q] (tableau/tableauo P X (lvar) cert))))))

(defn- checked
  "cert, after the relation accepts it in check mode. A rejection would be a
  defect in this implementation, never an answer."
  [P X cert]
  (when-not (certificate? P X cert)
    (throw (ex-info "the relation rejected its own certificate"
                    {:sentence X :certificate cert})))
  cert)

(defn- check-query! [P A]
  (when-not (syntax/query? (:language P) A)
    (throw (ex-info "a query is a ground literal of L (§2)" {:query A}))))

(defn- negation [A] (list 'not A))

(defn- with-timeout
  "Run f on another thread for at most timeout-ms (forever if nil).
  Returns f's value, or ::timeout after cancelling it."
  [timeout-ms f]
  (if-not timeout-ms
    (f)
    (let [task (future (f))
          v (deref task timeout-ms ::timeout)]
      (when (= v ::timeout) (future-cancel task))
      v)))

;;; ---------------------------------------------------------------------
;;; Definition 6.1

(defn succeeds
  "A closed P-tableau for ¬A (Definition 6.1), or nil if none is found
  within :max-fuel (and :timeout-ms)."
  [P A {:keys [max-fuel timeout-ms] :or {max-fuel default-max-fuel}}]
  (check-query! P A)
  (let [cert (with-timeout timeout-ms #(closed-tableau P (negation A) max-fuel))]
    (when (and cert (not= cert ::timeout))
      (checked P (negation A) cert))))

(defn fails
  "A closed P-tableau for A (Definition 6.1), or nil if none is found within
  :max-fuel (and :timeout-ms)."
  [P A {:keys [max-fuel timeout-ms] :or {max-fuel default-max-fuel}}]
  (check-query! P A)
  (let [cert (with-timeout timeout-ms #(closed-tableau P A max-fuel))]
    (when (and cert (not= cert ::timeout))
      (checked P A cert))))

(defn query-status
  "Run the query A against the program P. Returns

      {:status :succeeds, :certificate <closed P-tableau for ¬A>}
      {:status :fails,    :certificate <closed P-tableau for A>}
      {:status :unresolved, :reason :exhausted | :timeout}

  `:unresolved` with `:exhausted` means neither side has a closed tableau
  within :max-fuel. Options:

  - `:max-fuel`   the fuel of each search (default `default-max-fuel`);
  - `:timeout-ms` wall-clock bound for the whole query;
  - `:both`       search both sides to the end and throw if both close.
                  Lemma 7.5 and Theorem 7.2 say that cannot happen."
  [P A {:keys [max-fuel timeout-ms both] :or {max-fuel default-max-fuel}}]
  (check-query! P A)
  (let [results (java.util.concurrent.LinkedBlockingQueue.)
        side (fn [status X]
               (future
                 (try
                   (.put results [status X (closed-tableau P X max-fuel)])
                   (catch InterruptedException _ nil))))
        searches [(side :succeeds (negation A)) (side :fails A)]
        deadline (when timeout-ms (+ (System/currentTimeMillis) timeout-ms))
        next-result (fn []
                      (if deadline
                        (.poll results (max 0 (- deadline (System/currentTimeMillis)))
                               java.util.concurrent.TimeUnit/MILLISECONDS)
                        (.take results)))]
    (try
      (loop [pending 2 found nil]
        (if (zero? pending)
          (or found {:status :unresolved :reason :exhausted :max-fuel max-fuel})
          (let [[status X cert :as r] (next-result)]
            (cond
              (nil? r)
              {:status :unresolved :reason :timeout :max-fuel max-fuel}

              (and cert found)
              (throw (ex-info "a query both succeeds and fails: the implementation is unsound"
                              {:query A :found found :also [status cert]}))

              (and cert (not both))
              {:status status :certificate (checked P X cert)}

              cert
              (recur (dec pending) {:status status :certificate (checked P X cert)})

              :else
              (recur (dec pending) found)))))
      (finally
        (run! future-cancel searches)))))
