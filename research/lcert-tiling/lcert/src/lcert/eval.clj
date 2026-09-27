(ns lcert.eval
  "The evaluators of λᶜᵉʳᵗ₀ (R4-metatheory.md §5).

  The language runs on evalᴱₙ, the *erasing* budgeted evaluator:
    - it evaluates a program erased from its typing derivation: the argument
      of every application at a usage-0 Π, and the first component of every
      pair at a usage-0 Σ, are replaced by ⋆ (they are never needed at
      runtime);
    - tokens are distinct runtime objects, n of them for a program run in Θₙ;
    - reflect_D v e runs the program a certificate v encodes, when the
      certificate is within the budget cap (‖v‖ ≤ n) and Check accepts it at
      ⌜D⌝.  It erases the decoded derivation, and gives the program m of v's
      own tokens (m < ‖v‖ by strict overhead), taken in preorder;
    - inspect branches on Check and hands the certificate to the branch;
    - H₁ and abort return defaults: they are expected to be unreachable.

  Erasure matters.  An erased position may build a certificate from one token
  used any number of times (review R4-04); the non-erasing evaluator of the
  metatheory's Theorem 4 would then physically build it.  That evaluator is
  kept here, as the {:erase? false} option, only to test that the two agree on
  data results (Theorem 4′, a sketch in the metatheory).

  Runtime values:
    Unit ⋆ → :star     Bool → true/false     Nat → a long     Lbl → a keyword
    Syn → [:sl l] / [:sn l a b]
    ◇ → a Token        R → [:rl l] / [:rn token l r1 r2]
    Π → a Clojure fn   Σ → [:pv a b]"
  (:require [lcert.syntax :as s]
            [lcert.encode :as e]
            [lcert.check :as c]
            [lcert.kernel :as k]
            [lcert.typing :as t]))

(def ^:dynamic *dynamic-cap*
  "Assessor's demo switch: when true, a reflected program runs under its
  caller's budget cap n instead of its own declared budget m."
  false)

(def ^:dynamic *trace*
  "Assessor's demo hook: when bound to a function, it receives one map per
  inspect and per reflect, so a run can be narrated step by step.  The map's
  :depth is the number of reflections the evaluating code sits under."
  nil)

(def ^:dynamic *charged-cap*
  "Assessor's demo switch: when true, a reflected program runs under the cap
  n − ‖v‖ + m, its caller's cap less the tokens the reflection burns.  It
  takes precedence over *dynamic-cap*."
  false)

(defrecord Token [id])

(defn token "A runtime token." [id] (->Token id))

;; ---------------------------------------------------------------------------
;; Lazy finite supplies (proflog ADR-0143 Step 2; METATHEORY.md §2.3).
;;
;; A lazy supply promises N nodes and materializes one only when a program
;; takes it apart.  It stands for one finite tree, the *spine of N*:
;;
;;     node(τ₀, a, leaf a, node(τ₁, a, leaf a, … node(τ_{N−1}, a, leaf a, leaf a)))
;;
;; whose i-th token τᵢ is (token [:lazy sid i]), sid naming the supply.  A
;; LazySupply record is the unmaterialized suffix from index `start`, with
;; `n` nodes still promised.  Forcing it yields that suffix's top node, whose
;; right child is again a LazySupply; forcing is pure, so forcing the same
;; suffix twice yields equal values.  Every operation that looks inside an R
;; value forces as it goes (force-r); `nodes` alone answers from the promise.

(defrecord LazySupply [sid start n])

(defn lazy? "Is v an unmaterialized supply suffix?" [v] (instance? LazySupply v))

(defn lazy-supply
  "A fresh lazy supply promising n nodes (n may be a BigInteger)."
  [n]
  (->LazySupply (keyword (gensym "supply")) 0 (bigint n)))

(def ^:dynamic *materialized*
  "When bound to an atom, every node materialized from a lazy supply
  increments it."
  nil)

(def ^:dynamic *materialize-limit*
  "No run may materialize a supply's node at index *materialize-limit* or
  beyond.  A walk over a supply promised at 10^100 would otherwise run for
  ever; the guard makes it fail fast, and it is stateless: it bounds how far
  into any one supply a program may go, not a global count."
  10000000)

(defn force-r
  "An R value with its top node materialized: v itself, unless v is a lazy
  supply suffix, which yields its top node (or the final leaf)."
  [v]
  (if-not (lazy? v)
    v
    (let [{:keys [sid start n]} v]
      (cond
        (zero? n) [:rl :a]
        (>= start *materialize-limit*)
        (throw (ex-info (str "refusing to materialize supply node " start
                             ": past the limit of " *materialize-limit*)
                        {:type :lcert/materialization-limit :limit *materialize-limit*}))
        :else
        (do (when *materialized* (swap! *materialized* inc))
            [:rn (token [:lazy sid start]) :a [:rl :a] (->LazySupply sid (inc start) (dec n))])))))

;; ---------------------------------------------------------------------------
;; Certificate values.

(defn nodes
  "Internal nodes of an R value.  A lazy supply answers from its promise,
  without materializing anything."
  [v]
  (cond
    (lazy? v) (:n v)
    (= :rn (first v)) (+ 1 (nodes (nth v 3)) (nodes (nth v 4)))
    :else 0))

(defn tokens
  "The tokens of an R value (or of a pair holding R values), in preorder.
  An unmaterialized supply suffix contributes none: its tokens are fresh by
  construction — no value outside the suffix can hold one — and enumerating
  them would materialize it.  So the linearity checks below cover every
  materialized token, and the rest is covered by construction."
  [v]
  (cond
    (lazy? v) []
    (not (vector? v)) []
    (= :rn (first v)) (into [(second v)] (concat (tokens (nth v 3)) (tokens (nth v 4))))
    (= :pv (first v)) (into (tokens (second v)) (tokens (nth v 2)))
    :else []))

(defn materialize
  "The whole tree of an R value, lazy parts forced (subject to the guard)."
  [v]
  (let [v (force-r v)]
    (if (= :rn (first v))
      (let [[_ tok l a b] v] [:rn tok l (materialize a) (materialize b)])
      v)))

(defn assert-linear!
  "Throw if some token object occurs twice in value v: a well-typed program
  run by the erasing evaluator never duplicates a token.  The walk sees
  certificate trees and pairs; tokens captured inside a closure are not
  visible to it (review E2), so a value of function type is not checked."
  [v]
  (let [ts (tokens v)]
    (when (not= (count ts) (count (distinct ts)))
      (throw (ex-info "a token occurs twice in a value" {:type :lcert/linearity :tokens ts})))
    v))

(defn print-value "print: forget the tokens of an R value." [v]
  (let [v (force-r v)]
    (case (first v)
      :rl [:sl (second v)]
      :rn [:sn (nth v 2) (print-value (nth v 3)) (print-value (nth v 4))])))

;; ---------------------------------------------------------------------------
;; Defaults, by skeleton (R4-metatheory.md §3.1: computable choices).

(defn default-value [sk]
  (case (first sk)
    :Unit :star, :Bool false, :Nat 0, :Lbl (first s/labels)
    :Syn [:sl (first s/labels)], :R [:rl (first s/labels)]
    ;; a fresh phantom token each time, so two defaults never share one
    :Dia (token (keyword (gensym "phantom")))
    :Fn (let [v (default-value (nth sk 2))] (fn [_] v))
    :Prod [:pv (default-value (second sk)) (default-value (nth sk 2))]))

;; ---------------------------------------------------------------------------
;; Erasure, from a typing derivation.

(defn erase
  "The runtime term of runtime derivation d, with usage-0 positions replaced
  by ⋆.  Type annotations are kept, but never evaluated."
  [d]
  (let [p (:prems d)
        er (fn [i] (erase (nth p i)))
        t (:term d)]
    (case (:rule d)
      (:Var :Unit :TT :FF :Zero :Lbl) t
      :Conv (er 0)
      :Lam (let [[_ u A _] t] [:lam u A (er 1)])
      :App (let [u (second (:type (nth p 0)))]
             [:app (er 0) (if (= u 0) [:star] (er 1))])
      :Pair (let [[_ S] t u (second S)]
              [:pair S (if (= u 0) [:star] (er 1)) (er 2)])
      :Let [:let (second t) (er 0) (er 2)]
      :Abort [:abort (second t) (er 0)]
      :If [:if (er 0) (er 1) (er 2)]
      :ElimBool [:elimBool (second t) (er 0) (er 2) (er 3)]
      :Succ [:succ (er 0)]
      :RecN [:recN (second t) (er 2) (er 3) (er 0)]
      :CaseLbl [:caseLbl (second t) (er 0) (mapv erase (drop 2 p))]
      :SLeaf [:sleaf (er 0)]
      :SNode [:snode (er 0) (er 1) (er 2)]
      :RecSyn [:recSyn (second t) (er 2) (er 3) (er 0)]
      :Leaf [:leaf (er 0)]
      :Node [:node (er 0) (er 1) (er 2) (er 3)]
      :ItR [:itR (second t) (er 1) (er 2) (er 3)]
      :Print [:print (er 0)]
      :CaseR [:caseR (second t) (er 0) (er 2) (er 3)]
      :Chk [:chk (er 0) (er 1)]
      :H1 [:h1 (er 0) (er 1) (er 2) (er 3) (er 4)]
      :Reflect [:reflect (second t) (er 0) (er 1)]
      :Inspect [:inspect (second t) (er 0) (er 1) (er 3) (er 4)])))

;; ---------------------------------------------------------------------------
;; Evaluation.

(declare eval-deriv)

(defn- ev
  "Evaluate term t in environment env (a vector, innermost last) under the
  budget cap n.  opts: {:erase? bool}.  With :erase? true, t is an erased
  term and certificate nodes are checked never to reuse a token."
  [t env n opts]
  (let [go (fn [x] (ev x env n opts))
        under (fn [vals x] (ev x (into env vals) n opts))]
    (case (first t)
      :var (nth env (- (count env) 1 (second t)))
      :star :star
      :tt true
      :ff false
      :zero 0
      :succ (inc (go (second t)))
      :lbl (second t)
      ;; call-by-value: the argument runs first (review E1); in a typed
      ;; program it cannot produce a value, so the default is never observed
      :abort (do (go (nth t 2)) (default-value (c/skel (second t))))
      :if (let [[_ b x y] t] (if (go b) (go x) (go y)))
      :elimBool (let [[_ _P b x y] t] (if (go b) (go x) (go y)))
      :recN (let [[_ _P z st nn] t
                  k (go nn)]
              (loop [i 0 acc (go z)]
                (if (< i k) (recur (inc i) (under [i acc] st)) acc)))
      :caseLbl (let [[_ _P a bs] t] (go (nth bs (s/label-index (go a)))))
      :sleaf [:sl (go (second t))]
      :snode (let [[_ a c1 c2] t] [:sn (go a) (go c1) (go c2)])
      :recSyn (let [[_ _P tl tn cc] t]
                (letfn [(rec [code]
                          (case (first code)
                            :sl (under [(second code)] tl)
                            :sn (let [[_ l a b] code]
                                  (under [l a b (rec a) (rec b)] tn))))]
                  (rec (go cc))))
      :leaf [:rl (go (second t))]
      :node (let [[_ d a r1 r2] t
                  tok (go d) l (go a) v1 (go r1) v2 (go r2)]
              (when (:erase? opts)
                (when (some #{tok} (concat (tokens v1) (tokens v2)))
                  (throw (ex-info "a token was used twice" {:type :lcert/linearity :token tok}))))
              [:rn tok l v1 v2])
      :itR (let [[_ _X g h rr] t
                 gv (go g) hv (go h)]
             (letfn [(rec [v]
                       (let [v (force-r v)]
                         (case (first v)
                           :rl (gv (second v))
                           :rn (let [[_ tok l a b] v]
                                 ((((hv tok) l) (rec a)) (rec b))))))]
               (rec (go rr))))
      ;; caseR takes one node apart: constant time, and on a lazy supply it
      ;; materializes exactly that node
      :caseR (let [[_ _X rr tl tn] t
                   v (force-r (go rr))]
               (case (first v)
                 :rl (under [(second v)] tl)
                 :rn (let [[_ tok l a b] v] (under [tok l a b] tn))))
      :print (print-value (go (second t)))
      :lam (let [[_ _u _A body] t] (fn [v] (ev body (conj env v) n opts)))
      :app (let [[_ f a] t] ((go f) (go a)))
      :pair (let [[_ _S a b] t] [:pv (go a) (go b)])
      :let (let [[_ _C p body] t
                 [_ x y] (go p)]
             (under [x y] body))
      :chk (let [[_ cc dd] t] (c/check (go cc) (go dd)))
      :h1 (do (doseq [x (rest t)] (go x)) :star)
      :reflect (let [[_ D rr ev0] t
                     v0 (go rr)
                     _ (go ev0)
                     within-cap (<= (nodes v0) n)
                     ;; a certificate within the cap is materialized in full
                     ;; (guarded), so its tokens can be handed on below
                     v (if within-cap (materialize v0) v0)
                     checks (and within-cap (c/check (print-value v) (e/enc-exp D)))]
                 (if checks
                   ;; run the certified program on m of v's own tokens
                   (let [dd (e/dec-deriv (print-value v))
                         m (count (:ctx dd))
                         toks (vec (take m (tokens v)))
                         cap (cond *charged-cap* (+ (- n (nodes v)) m)
                                   *dynamic-cap* n
                                   :else m)]
                     (when *trace*
                       (*trace* {:event :reflect :depth (:depth opts 0) :type D :nodes (nodes v)
                                 :cap n :runs true :m m :decoded-cap cap}))
                     (eval-deriv dd cap (assoc opts :tokens toks :depth (inc (:depth opts 0)))))
                   (do (when *trace*
                         (*trace* {:event :reflect :depth (:depth opts 0) :type D :nodes (nodes v)
                                   :cap n :runs false :within-cap within-cap}))
                       (default-value (c/skel D)))))
      :inspect (let [[_ _X rr cc t1 t2] t
                     v (go rr)
                     tc (go cc)
                     ok (c/check (print-value v) tc)]
                 (when *trace*
                   (*trace* {:event :inspect :depth (:depth opts 0) :type-code tc
                             :nodes (nodes v) :ok ok}))
                 (under [v :star] (if ok t1 t2)))
      (throw (ex-info (str "cannot evaluate " (first t)) {:term t})))))

(defn eval-deriv
  "Evaluate the program of runtime derivation d, whose context is Θₘ, under
  budget cap n.  The m tokens are fresh unless opts supplies them as :tokens.
  With {:erase? true} (the default) the erasing evaluator runs, and the
  result is checked to hold no token twice."
  ([d n] (eval-deriv d n {:erase? true}))
  ([d n opts]
   (let [opts (merge {:erase? true} opts)
         m (count (:ctx d))
         toks (or (:tokens opts) (mapv #(token (inc %)) (range m)))
         term (if (:erase? opts) (erase d) (:term d))
         v (ev term toks n (dissoc opts :tokens))]
     (if (:erase? opts) (assert-linear! v) v))))

(defn run
  "Type check term t in Θₙ and evaluate it with n fresh tokens (erasing)."
  [n t]
  (eval-deriv (t/check-top n t) n))
