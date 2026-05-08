(ns proflog.kernel.robinson-q-profile
  "Deduction-modulo conversion profile for Robinson arithmetic Q.

   This namespace is intentionally narrow. It rewrites visible Q arithmetic
   terms to their normal forms, records the conversion steps, then delegates the
   converted formula to the ordinary Proflog kernel. Q3 is handled separately as
   a recorded predecessor-or-zero case split because it is not a terminating
   rewrite rule."
  (:require [proflog.ast :as ast]
            [proflog.kernel :as kernel]))

(defn- zero-term?
  "True when `term` is the Q constant `zero`."
  [term]
  (= (ast/app-term 'zero) term))

(defn- succ-term?
  "Return the predecessor of a Q successor term, or nil otherwise."
  [term]
  (when (and (= 'app (ast/tag-of term))
             (= 's (second term))
             (= 3 (count term)))
    (nth term 2)))

(declare q-normalize-term)

(defn- app-with-normalized-args
  "Normalize all arguments of an application-like term or atom."
  [term]
  (let [[args steps]
        (reduce (fn [[normalized-args collected-steps] arg]
                  (let [[normalized-arg arg-steps] (q-normalize-term arg)]
                    [(conj normalized-args normalized-arg)
                     (into collected-steps arg-steps)]))
                [[] []]
                (nnext term))]
    [(apply ast/app-term (second term) args) steps]))

(defn- rewrite-root
  "Apply one Q conversion rule at the root of an already child-normalized term."
  [term]
  (when (and (= 'app (ast/tag-of term))
             (= 4 (count term)))
    (let [head (second term)
          x (nth term 2)
          y (nth term 3)]
      (case head
        add (cond
              (zero-term? y)
              [:add-zero x]

              (succ-term? y)
              [:add-succ (ast/app-term 's
                                       (ast/app-term 'add x (succ-term? y)))]

              :else nil)
        mul (cond
              (zero-term? y)
              [:mul-zero (ast/app-term 'zero)]

              (succ-term? y)
              [:mul-succ (ast/app-term 'add
                                       (ast/app-term 'mul x (succ-term? y))
                                       x)]

              :else nil)
        nil))))

(defn q-normalize-term
  "Return `[normalized-term rewrite-steps]` for one Q term."
  [term]
  (case (ast/tag-of term)
    var [term []]
    par [term []]
    app (let [[with-normalized-args child-steps] (app-with-normalized-args term)]
          (if-let [[rule rewritten] (rewrite-root with-normalized-args)]
            (let [[normalized rewrite-steps] (q-normalize-term rewritten)]
              [normalized
               (into child-steps
                     (cons (list 'q-rewrite rule with-normalized-args rewritten)
                           rewrite-steps))])
            [with-normalized-args child-steps]))
    [term []]))

(defn- normalize-atom
  "Normalize every term argument in a relation atom."
  [atom]
  (app-with-normalized-args atom))

(defn q-normalize-formula
  "Return `[normalized-formula rewrite-steps]` for one formula."
  [formula]
  (case (ast/tag-of formula)
    true [formula []]
    false [formula []]
    pos (let [[atom steps] (normalize-atom (second formula))]
          [(ast/pos-lit atom) steps])
    neg (let [[atom steps] (normalize-atom (second formula))]
          [(ast/neg-lit atom) steps])
    eq (let [[left left-steps] (q-normalize-term (second formula))
             [right right-steps] (q-normalize-term (nth formula 2))]
         [(ast/eq-lit left right) (into left-steps right-steps)])
    neq (let [[left left-steps] (q-normalize-term (second formula))
              [right right-steps] (q-normalize-term (nth formula 2))]
          [(ast/neq-lit left right) (into left-steps right-steps)])
    and (let [[left left-steps] (q-normalize-formula (second formula))
              [right right-steps] (q-normalize-formula (nth formula 2))]
          [(ast/and-form left right) (into left-steps right-steps)])
    or (let [[left left-steps] (q-normalize-formula (second formula))
             [right right-steps] (q-normalize-formula (nth formula 2))]
         [(ast/or-form left right) (into left-steps right-steps)])
    not (let [[inner steps] (q-normalize-formula (second formula))]
          [(ast/not-form inner) steps])
    implies (let [[left left-steps] (q-normalize-formula (second formula))
                  [right right-steps] (q-normalize-formula (nth formula 2))]
              [(ast/implies-form left right) (into left-steps right-steps)])
    forall (let [tied (second formula)
                 [body steps] (q-normalize-formula (:body tied))]
             [(ast/forall-form (:binding-nom tied) body) steps])
    once-forall (let [tied (second formula)
                      [body steps] (q-normalize-formula (:body tied))]
                  [(ast/once-forall-form (:binding-nom tied) body) steps])
    exists (let [tied (second formula)
                 [body steps] (q-normalize-formula (:body tied))]
             [(ast/exists-form (:binding-nom tied) body) steps])
    [formula []]))

(defn- same-var?
  "True when `term` is exactly the object variable bound by `binding-nom`."
  [binding-nom term]
  (= (ast/var-term binding-nom) term))

(defn- same-successor-var?
  "True when `term` is `s(binding-nom)` in the Q term language."
  [binding-nom term]
  (= (ast/app-term 's (ast/var-term binding-nom)) term))

(defn- neq-var-zero?
  "Recognize either orientation of `x != zero`."
  [binding-nom formula]
  (and (= 'neq (ast/tag-of formula))
       (let [left (second formula)
             right (nth formula 2)]
         (or (and (same-var? binding-nom left)
                  (zero-term? right))
             (and (zero-term? left)
                  (same-var? binding-nom right))))))

(defn- neq-var-successor?
  "Recognize either orientation of `x != s(y)`."
  [x-nom y-nom formula]
  (and (= 'neq (ast/tag-of formula))
       (let [left (second formula)
             right (nth formula 2)]
         (or (and (same-var? x-nom left)
                  (same-successor-var? y-nom right))
             (and (same-successor-var? y-nom left)
                  (same-var? x-nom right))))))

(defn- q3-predecessor-refutation?
  "Recognize the closed branch shape used to refute Q3's negation.

   Proving Q3 by tableau means closing:

     exists x. x != zero and once-forall y. x != s(y)

   This matcher is structural over the normalized formula; it does not evaluate
   arbitrary terms or synthesize predecessor witnesses on the host."
  [formula]
  (when (= 'exists (ast/tag-of formula))
    (let [x-tie (second formula)
          x-nom (:binding-nom x-tie)
          body (:body x-tie)]
      (when (= 'and (ast/tag-of body))
        (let [left (second body)
              right (nth body 2)
              parts [[left right] [right left]]]
          (some (fn [[zero-branch predecessor-branch]]
                  (when (and (neq-var-zero? x-nom zero-branch)
                             (= 'once-forall (ast/tag-of predecessor-branch)))
                    (let [y-tie (second predecessor-branch)
                          y-nom (:binding-nom y-tie)]
                      (when (neq-var-successor? x-nom y-nom (:body y-tie))
                        {:witness x-nom
                         :predecessor y-nom}))))
                parts))))))

(defn- q3-case-split-proof
  "Build auditable proof evidence for the Robinson-Q Q3 theory rule."
  [{:keys [witness predecessor]}]
  (list 'q3-case-split
        'predecessor-or-zero
        (ast/var-term witness)
        (ast/var-term predecessor)))

(defn- profile-proof
  "Wrap a kernel proof with auditable theory-conversion evidence."
  [rewrite-steps proof]
  (list 'profiled 'robinson-q (apply list rewrite-steps) proof))

(defn prove-program
  "Normalize a formula modulo Q conversion, then prove it with the core kernel."
  [program formula proof-limit fuel]
  (let [[normalized rewrite-steps] (q-normalize-formula formula)
        q3-match (q3-predecessor-refutation? normalized)]
    (if q3-match
      (if (pos? proof-limit)
        (list
          (profile-proof
            (conj rewrite-steps (q3-case-split-proof q3-match))
            (list 'q3-close)))
        '())
      (let [proofs (if (nil? fuel)
                     (kernel/prove-program program normalized proof-limit)
                     (kernel/prove-program program normalized proof-limit fuel))]
        (map #(profile-proof rewrite-steps %) proofs)))))
