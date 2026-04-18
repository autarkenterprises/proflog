(ns proflog.query
  "Top-level Proflog query helpers.

   Fitting's semantics are inherently three-valued at the query boundary:
   success is witnessed by a closed tableau for the negated query, failure by a
   closed tableau for the query itself, and some queries support neither proof.
   The kernel remains purely relational; this namespace provides the host-level
   semidecision wrappers and a small concurrent race for operational honesty."
  (:require [proflog.kernel :as kernel]
            [proflog.language :as language]
            [proflog.normalize :as normalize]))

(defn- launch-daemon
  "Run `thunk` on a daemon thread and return a promise for its result."
  [name thunk]
  (let [result (promise)
        worker (Thread.
                 (fn []
                   (try
                     (deliver result (thunk))
                     (catch Throwable t
                       (deliver result t))))
                 name)]
    (.setDaemon worker true)
    (.start worker)
    {:result result
     :thread worker}))

(defn- stop-thread!
  "Best-effort shutdown for one bounded query worker."
  [thread]
  (.interrupt thread)
  (when (.isAlive thread)
    (.stop thread)))

(defn- await-daemon-result
  "Run `thunk` on a daemon thread and wait up to `timeout-ms` for a result."
  [name timeout-ms poll-ms thunk]
  (let [{result :result thread :thread} (launch-daemon name thunk)
        deadline (+ (System/currentTimeMillis) timeout-ms)]
    (loop []
      (cond
        (realized? result)
        (let [value @result]
          (stop-thread! thread)
          (if (instance? Throwable value)
            (throw value)
            value))
        (>= (System/currentTimeMillis) deadline)
        (do
          (stop-thread! thread)
          '())
        :else
        (do
          (Thread/sleep poll-ms)
          (recur))))))

(defn query-succeeds
  "Return up to `n` proofs showing that `query` succeeds with `program`."
  ([program query] (query-succeeds program query 1))
  ([program query n]
   (let [checked-query (language/validate-query (:language program) query)]
     (kernel/prove-program program (normalize/negate-formula checked-query) n))))

(defn query-fails
  "Return up to `n` proofs showing that `query` fails with `program`."
  ([program query] (query-fails program query 1))
  ([program query n]
   (let [checked-query (language/validate-query (:language program) query)]
     (kernel/prove-program program checked-query n))))

(defn query-succeeds-within
  "Run `query-succeeds` with an explicit wall-clock budget."
  ([program query timeout-ms]
   (query-succeeds-within program query 1 timeout-ms))
  ([program query n timeout-ms]
   (await-daemon-result "proflog-query-succeeds-within"
                        timeout-ms
                        5
                        #(query-succeeds program query n))))

(defn query-fails-within
  "Run `query-fails` with an explicit wall-clock budget."
  ([program query timeout-ms]
   (query-fails-within program query 1 timeout-ms))
  ([program query n timeout-ms]
   (await-daemon-result "proflog-query-fails-within"
                        timeout-ms
                        5
                        #(query-fails program query n))))

(defn query-status
  "Race the success and failure semidecision procedures for one query.

   Returns one of:
   - `:succeeds`
   - `:fails`
   - `:unresolved`
   - `:inconsistent` when both proofs are found within the race budget"
  ([program query]
   (query-status program query {}))
  ([program query {:keys [timeout-ms proof-limit poll-ms]
                   :or {timeout-ms 250
                        proof-limit 1
                        poll-ms 5}}]
   (let [{success-result :result success-thread :thread}
         (launch-daemon "proflog-query-succeeds"
                        #(query-succeeds program query proof-limit))
         {failure-result :result failure-thread :thread}
         (launch-daemon "proflog-query-fails"
                        #(query-fails program query proof-limit))
         finish (fn [status]
                  (stop-thread! success-thread)
                  (stop-thread! failure-thread)
                  status)
         deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop []
       (let [success-ready? (realized? success-result)
             failure-ready? (realized? failure-result)
             success-proofs (when success-ready? @success-result)
             failure-proofs (when failure-ready? @failure-result)]
         (cond
           (and (seq success-proofs) (seq failure-proofs)) (finish :inconsistent)
           (seq success-proofs) (finish :succeeds)
           (seq failure-proofs) (finish :fails)
           (and success-ready? failure-ready?) (finish :unresolved)
           (>= (System/currentTimeMillis) deadline)
           (finish :unresolved)
           :else
           (do
             (Thread/sleep poll-ms)
             (recur))))))))
