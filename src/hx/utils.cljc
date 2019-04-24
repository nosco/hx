(ns hx.utils
  (:require [clojure.string :as str]
            #?(:cljs [goog.object :as gobj])))

(def ^:dynamic *perf-debug?* false)

(defmacro measure-perf [tag form]
  (if *perf-debug?*
    (let [begin (str tag "_begin")
          end (str tag "_end")]
      `(do (js/performance.mark ~begin)
           (let [ret# ~form]
             (js/performance.mark ~end)
             (js/performance.measure ~(str "measure_" tag)
                                     ~begin
                                     ~end)
             ret#)))
    form))

(defn also-as
  "If key `k1` is contained in `m`, assocs the value of it into `m` at key `k2`"
  [m k1 k2]
  (if-let [entry (find m k1)]
    (let [[_ v] entry]
      (assoc m k2 v))
    m))

(defn- camel->kebab
  "Converts from camel case (e.g. Foo or FooBar) to kebab case
   (e.g. foo or foo-bar)."
  [s]
  (if (> (count s) 1)
    (str/join "-" (map str/lower-case (re-seq #"\w[a-z0-9\?_\./]*" s)))
    s))

(comment
  (camel->kebab "x")

  (camel->kebab "x1")

  (camel->kebab "xx1")

  (camel->kebab "xX")

  (camel->kebab "xX1")

  (camel->kebab "x1X")

  (camel->kebab "xxX")

  (camel->kebab "xXx")

  (camel->kebab "xxXx")

  (camel->kebab "x1xXx1")

  (camel->kebab "x?")

  (camel->kebab "x_x")

  (camel->kebab "xX.x")

 )

(defn keyword->str [k]
  (let [kw-ns (namespace k)
        kw-name (name k)]
    (if (nil? kw-ns)
      kw-name

      (str kw-ns "/" kw-name))))

#?(:cljs
   (defn shallow-js->clj
     ([x] (shallow-js->clj x :keywordize-keys false :camel-kebab false))
     ([x & opts]
      (let [{:keys [keywordize-keys camel-kebab]
             :or {keywordize-keys false
                  camel-kebab false}} opts
            keyfn (if keywordize-keys
                    (if camel-kebab
                      (comp keyword camel->kebab)
                      keyword)
                    str)
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




;;
;; New impl.
;;

(defn- set-obj [o k v]
  #?(:cljs (do (gobj/set o k v)
               o)
     :clj o))

(defn- join-classes
  "Join the `classes` with a whitespace."
  [classes]
  (->> classes
       (remove nil?)
       (str/join " ")))

(defn- class-name [x]
  (cond (or (nil? x)
            (keyword? x)
            (string? x))
        x

        (or (sequential? x)
            (set? x))
        (join-classes x)

        :else x))

(defn- camel-case*
  "Returns camel case version of the string, e.g. \"http-equiv\" becomes \"httpEquiv\"."
  [s]
  (if (or (keyword? s)
          (string? s)
          (symbol? s))
    (let [[first-word & words] (str/split (name s) #"-")]
      (if (or (empty? words)
              (= "aria" first-word)
              (= "data" first-word))
        s
        (-> (map str/capitalize words)
            (conj first-word)
            str/join)))
    s))

(defn- map->camel+js [x]
  (cond
    (map? x) (loop [ps (seq x)
                    o #js {}]
               (if (nil? ps)
                 o
                 (let [p (first ps)
                       k (key p)
                       v (val p)]
                   ;; side-effecting
                   (set-obj o (camel-case* (name k)) (map->camel+js v))
                   (recur (next ps)
                          o))))
    true x))

(comment
  (map->camel+js {})

  (map->camel+js {:color "red"})

  (map->camel+js {:color "red" :background "green"})

  (next [1 2])
  )

(defn clj->props
  "Shallowly converts props map to a JS obj. Handles certain special cases:

  1. `:class` -> \"className\", and joins collections together as a string
  2. `:for` -> \"htmlFor\"
  3. `:style` -> deeply converts this prop to a JS obj

  By default, converts kebab-case keys to camelCase strings. pass in `false`
  as a second arg to disable this."
  ([props] (clj->props props true))
  ([props camelize?]
   (loop [pxs (seq props)
          js-props #js {}]
     (if (nil? pxs)
       js-props
       (let [p (first pxs)
             k (key p)
             v (val p)]
         ;; side-effecting
         (case k
           :style (set-obj js-props "style" (map->camel+js v))
           :class (set-obj js-props "className" (class-name v))
           :for (set-obj js-props "htmlFor" v)

           (set-obj js-props
                    (if camelize?
                      (camel-case* (name k))
                      (name k))
                    v))
         (recur (next pxs)
                js-props))))))

(comment
  (clj->props {:class "foo"
               :style {:color "red"}})

  (clj->props {:class [nil "foo"]})

  (clj->props {:asdf "jkl"})

  (let [case {:x-x? "asdf"}]
    (-> (clj->props case)))

  (let [case {:x-x.x? "asdf"}]
    (-> (clj->props case)))

  (let [case {:xa1-xb2.x? "asdf"}]
    (-> (clj->props case)))


  (let [case {:ns/xb2.x? "asdf"}]
    (-> (clj->props case)))

  (let [case {:ns/-xb2.x? "asdf"}]
    (-> (clj->props case)))
  )
