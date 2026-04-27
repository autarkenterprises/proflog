(ns proflog.gamma
  "Bounded closed-term candidates for Fitting-style gamma instantiation.

   The kernel keeps the gamma rule readable as \"instantiate this universal
   with one admissible candidate\". This namespace owns the operationally
   necessary finite Herbrand enumeration policy for declared object-language
   constructors."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer [fail membero project]]
            [proflog.ast :as ast]))

(def ^:dynamic *closed-term-depth-cap*
  "Maximum constructor depth generated for one gamma-candidate choice.

   Fuel controls when gamma choices are available; this cap prevents one choice
   from materializing an unbounded Herbrand universe when a language has
   recursive constructors."
  3)

(defn- declaration-order
  "Stable ordering for declared constructor symbols."
  [entry]
  [(val entry) (str (key entry))])

(defn- tuples
  "Cartesian power of `xs` with width `n`."
  [xs n]
  (if (zero? n)
    (list '())
    (for [x xs
          tail (tuples xs (dec n))]
      (cons x tail))))

(defn- ordered-distinct
  "Preserve the first occurrence of each item in `xs`."
  [xs]
  (:items
    (reduce (fn [{:keys [seen] :as acc} x]
              (if (contains? seen x)
                acc
                (-> acc
                    (update :seen conj x)
                    (update :items conj x))))
            {:seen #{}
             :items []}
            xs)))

(defn closed-terms-up-to-depth
  "Enumerate declared-language closed terms up to constructor depth `max-depth`.

   Depth zero contains nullary constructors. Each later stratum uses all terms
   generated so far, but requires at least one argument from the previous exact
   depth, so every term is generated at its minimum constructor depth."
  [lang max-depth]
  (when (neg? max-depth)
    (throw (ex-info "Closed-term depth must be non-negative"
                    {:max-depth max-depth})))
  (let [declared-functions (sort-by declaration-order (:functions lang))
        nullary-terms (->> declared-functions
                           (filter (fn [[_ arity]]
                                     (zero? arity)))
                           (mapv (fn [[sym _]]
                                   (ast/app-term sym))))
        positive-functions (filter (fn [[_ arity]]
                                     (pos? arity))
                                   declared-functions)]
    (loop [depth 0
           terms-up-to nullary-terms
           exact-prev nullary-terms]
      (if (= depth max-depth)
        terms-up-to
        (let [exact-prev-set (set exact-prev)
              exact-next (->> positive-functions
                              (mapcat (fn [[sym arity]]
                                        (for [args (tuples terms-up-to arity)
                                              :when (some exact-prev-set args)]
                                          (apply ast/app-term sym args))))
                              ordered-distinct
                              vec)]
          (recur (inc depth)
                 (into terms-up-to exact-next)
                 exact-next))))))

(defn- fuel->closed-term-depth
  "Map the current micro-fuel slice to a finite constructor-depth bound."
  [fuel]
  (let [cap (max 0 *closed-term-depth-cap*)]
    (cond
      (nil? fuel) cap
      (integer? fuel) (min cap (max 0 fuel))
      :else 0)))

(defn closed-terms-for-fuel
  "Return bounded closed object-language terms for `prog` at the current fuel."
  [prog fuel]
  (if-let [lang (:language prog)]
    (closed-terms-up-to-depth lang (fuel->closed-term-depth fuel))
    []))

(defn closed-term-candidateo
  "Relate `term` to one bounded closed gamma candidate for `prog` and `fuel`."
  [prog fuel term]
  (project [fuel]
    (let [terms (closed-terms-for-fuel prog fuel)]
      (if (seq terms)
        (membero term (apply list terms))
        fail))))
