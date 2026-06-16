(ns proflog.diagnostics.witness
  "Conservative diagnostic witness extraction for supported open-branch fragments.

   Witness extraction never decides proof acceptance; it only interprets branch
   content that already has a clear classical propositional reading."
  (:require [proflog.ast :as ast]
            [proflog.kernel :as kernel]))

(defn- literal-polarity
  "Return [atom-symbol polarity-keyword] for ground atomic literals."
  [formula]
  (let [tag (ast/tag-of formula)]
    (when (#{'pos 'neg} tag)
      (let [term (second formula)]
        (when (and (= 'app (ast/tag-of term))
                   (empty? (nnext term)))
          [(second term) (if (= tag 'pos) :pos :neg)])))))

(defn- unsupported-form?
  [formula]
  (case (ast/tag-of formula)
    (and or not implies iff true false) false
    (pos neg) false
    true))

(defn- collect-literals
  "Flatten a ground propositional formula into a literal list when possible."
  [formula]
  (case (ast/tag-of formula)
    true '()
    false nil
    (pos neg) (if-let [lit (literal-polarity formula)]
                [lit]
                nil)
    and (let [left (collect-literals (second formula))
              right (collect-literals (nth formula 2))]
          (when (and left right)
            (concat left right)))
    or nil
    not nil
    implies nil
    iff nil
    nil))

(defn- assignment-from-literals
  "Build a classical assignment from nullary pos/neg literals."
  [literals]
  (loop [remaining literals
         assignment {}
         seen #{}]
    (if (empty? remaining)
      {:status :witness
       :assignment assignment}
      (let [[sym polarity] (first remaining)
            value (= polarity :pos)
            neg-value (not value)]
        (cond
          (contains? assignment sym)
          (if (= (get assignment sym) value)
            (recur (rest remaining) assignment seen)
            {:status :closed})

          (contains? seen sym)
          {:status :closed}

          :else
          (recur (rest remaining)
                 (assoc assignment sym value)
                 (conj seen sym)))))))

(defn extract-witness
  "Extract a witness assignment from a supported open-branch fragment.

   Accepts either an object-language formula in the v1 fragment or a map
   `{:literals [[sym polarity] ...]}` using `:pos`/`:neg` polarities."
  [input]
  (cond
    (map? input)
    (let [literals (:literals input)]
      (cond
        (nil? literals)
        {:status :ambiguous
         :reason "Input map lacks a :literals key."}

        (empty? literals)
        {:status :witness :assignment {}}

        (some (fn [[_ pol]] (not (#{:pos :neg} pol))) literals)
        {:status :unsupported
         :reason "Literal polarities must be :pos or :neg."}

        :else
        (assignment-from-literals literals)))

    (unsupported-form? input)
    {:status :unsupported
     :reason "Formula uses constructs outside the ground propositional fragment."}

    :else
    (if (seq (kernel/prove input 1))
      {:status :closed}
      (if-let [literals (collect-literals input)]
        (assignment-from-literals literals)
        {:status :unsupported
         :reason "Formula is not a flat conjunction of ground atomic literals."}))))

(defn witness-for-formula
  "Convenience wrapper that preserves the public status map contract."
  [formula]
  (extract-witness formula))
