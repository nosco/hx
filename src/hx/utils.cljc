(ns hx.utils
  (:require [clojure.string :as str]
            #?(:cljs [goog.object :as gobj])))

(defn camel->kebab
  "Converts from camel case (e.g. Foo or FooBar) to kebab case
   (e.g. foo or foo-bar)."
  [s]
  (if (> (count s) 1)
    (str/join "-" (map str/lower-case (re-seq #"\w[a-z]+" s)))
    s))

#?(:cljs (defn shallow-clj->js
           "Shallowly transforms ClojureScript values to JavaScript.
  sets/vectors/lists become Arrays, Keywords and Symbol become Strings,
  Maps become Objects. Arbitrary keys are encoded to by `key->js`.
  Options is a key-value pair, where the only valid key is
  :keyword-fn, which should point to a single-argument function to be
  called on keyword keys. Default to `name`."
           [x & {:keys [keyword-fn]
                 :or   {keyword-fn name}
                 :as options}]
           (letfn [(keyfn [k] (key->js k thisfn))
                   (thisfn [x] (cond
                                 (nil? x) nil
                                 (satisfies? IEncodeJS x) (-clj->js x)
                                 (keyword? x) (keyword-fn x)
                                 (symbol? x) (str x)
                                 (map? x) (let [m (js-obj)]
                                            (doseq [[k v] x]
                                              (gobj/set m (keyfn k) v))
                                            m)
                                 (coll? x) (let [arr (array)]
                                             (doseq [x x]
                                               (.push arr x))
                                             arr)
                                 :else x))]
             (thisfn x))))

#?(:clj (defn shallow-clj->js
          [x & {:keys [keyword-fn]
                :or   {keyword-fn name}
                :as options}]
          x
          (cond
            (nil? x) nil
            (keyword? x) (keyword-fn x)
            (symbol? x) (str x)
            (map? x) (let [kvs (reduce-kv
                                (fn [c k v]
                                  (conj c (hx.utils/shallow-clj->js k) v)) [] x)]
                       (list* 'js-obj kvs))
            (coll? x) (list* 'js/Array x)
            :else x)
          ))

#_(shallow-clj->js [1 2 3])
#_(shallow-clj->js {:a "asdf" :b :y :c 2})

#?(:cljs (defn shallow-js->clj
           ([x] (shallow-js->clj x :keywordize-keys false))
           ([x & opts]
            (let [{:keys [keywordize-keys]} opts
                  keyfn (if keywordize-keys (comp keyword camel->kebab) str)
                  f (fn thisfn [x]
                      (cond
                        (satisfies? IEncodeClojure x)
                        (-js->clj x (apply array-map opts))

                        (seq? x)
                        x

                        (map-entry? x)
                        (MapEntry. (key x) (val x) nil)

                        (coll? x)
                        x

                        (array? x)
                        (vec x)

                        (identical? (type x) js/Object)
                        (into {} (for [k (js-keys x)]
                                   [(keyfn k) (unchecked-get x k)]))

                        :else x))]
              (f x)))))

;; I stole most of this from https://github.com/rauhs/hicada/blob/master/src/hicada/util.clj

#?(:clj (defn join-classes-js
          "Joins strings space separated"
          ([] "")
          ([& xs]
           (let [strs (->> (repeat (count xs) "~{}")
                           (interpose ",")
                           (apply str))]
             (list* 'js* (str "[" strs "].join(' ')") xs)))))

;; in CLJS construct an array out of args
#?(:cljs (defn join-classes-js
           ([] "")
           ([& xs]
            (apply js/Array xs))))

(defn join-classes
  "Join the `classes` with a whitespace."
  [classes]
  (->> (map #(if (string? %) % (seq %)) classes)
       (flatten)
       (remove nil?)
       (str/join " ")))

(defn camel-case
  "Returns camel case version of the key, e.g. :http-equiv becomes :httpEquiv."
  [k]
  (if (or (keyword? k)
          (string? k)
          (symbol? k))
    (let [[first-word & words] (str/split (name k) #"-")]
      (if (or (empty? words)
              (= "aria" first-word)
              (= "data" first-word))
        k
        (-> (map str/capitalize words)
            (conj first-word)
            str/join
            keyword)))
    k))

(defn camel-case-keys
  "Recursively transforms all map keys into camel case."
  [m]
  (cond
    (map? m)
    (reduce-kv
     (fn [m k v]
       (assoc m (camel-case k) v))
     {} m)
    ;; React native accepts :style [{:foo-bar ..} other-styles] so camcase those keys:
    (vector? m)
    (mapv camel-case-keys m)
    :else
    m))

(defmulti reactify-props-kv (fn [name value] name))

(defmethod reactify-props-kv :class [name value]
  (cond (or (nil? value)
            (keyword? value)
            (string? value))
        value

        (and (or (sequential? value)
                 (set? value))
             (every? string? value))
        (join-classes value)

        (vector? value)
        (apply join-classes-js value)

        :else value))

(defmethod reactify-props-kv :style [name value]
  (camel-case-keys value))

(defmethod reactify-props-kv :default [name value]
  value)

(defn reactify-props
  "Converts a HTML attribute map to react (class -> className), camelCases :style."
  [attrs]
  (if (map? attrs)
    (reduce-kv (fn [m k v]
                 (assoc m
                        (case k
                          :class :className
                          :for :htmlFor
                          (if ((some-fn keyword? symbol?) k)
                            (camel-case k)))
                        (reactify-props-kv k v))) {} attrs)
    attrs))

#?(:cljs (defn styles->js [props]
  (cond
    (and (map? props) (:style props))
    (assoc props :style (clj->js (:style props)))

    (gobj/containsKey props "style")
    (do (->> (gobj/get props "style")
             (clj->js)
             (gobj/set props "style"))
        props)

    :default props)))

#?(:cljs (defn clj->props [props]
           (-> props
               (reactify-props)
               (styles->js)
               (shallow-clj->js))))
