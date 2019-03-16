(ns hx.react
  (:require [goog.object :as gobj]
            ["react" :as react]
            ["react-is" :as react-is]
            [hx.hiccup :as hiccup]
            [hx.utils :as utils])
  (:require-macros [hx.react]))

(extend-type react/Component
  hiccup/IElement
  (-parse-element [el config args]
    (hiccup/make-element config el args)))

(def react-hiccup-config
  {:create-element react/createElement
   :is-element? react/isValidElement
   :is-element-type? react-is/isValidElementType
   :fragment react/Fragment})

(defn f [form]
  (hiccup/parse react-hiccup-config form))

(defn parse-body [body]
  (if (vector? body)
    (f body)
    body))

(def fragment react/Fragment)

(defmethod hiccup/parse-element
  :<>
  [config el & args]
  (hiccup/-parse-element
   hx.react/fragment
   react-hiccup-config
   args))

(defmethod hiccup/parse-element
  :provider
  [config el {:keys [context value]} args]
  (hiccup/-parse-element
   (.-Provider context)
   react-hiccup-config
   [{:value value}
    args]))

(defn also-as
  "If key `k1` is contained in `m`, assocs the value of it into `m` at key `k2`"
  [m k1 k2]
  (if-let [entry (find m k1)]
    (let [[_ v] entry]
      (assoc m k2 v))
    m))

(comment
  (also-as {:a 1} :a :b)

  (also-as {:a 1} :b :c)

  )

(defn props->clj [props]
  (let [props (utils/shallow-js->clj props :keywordize-keys true)]
    ;; provide `:class-name` property also as `:class`
    (-> props
        (also-as :class-name :class))))

(comment
  (props->clj #js {"x0" 1})

  (props->clj #js {"test0" 1})

  (props->clj #js {"testAsdf?" 1})

  (props->clj #js {"test_asdf?" 1})

  (props->clj #js {"testTest.asdf?" 1})

  )

(defn $ [el & args] (hiccup/make-element react-hiccup-config el args))

(defn assign-methods [class method-map]
  (doseq [[method-name method-fn] method-map]
    (gobj/set (.-prototype class)
              (munge (name method-name))
              method-fn))
  class)

(defn create-class [super-class init-fn static-properties method-names]
  (let [ctor (fn [props]
               (this-as this
                 ;; auto-bind methods
                 (doseq [method method-names]
                   (gobj/set this (munge method)
                             (.bind (gobj/get this (munge method)) this)))

                 (init-fn this props)))]
    ;; set static properties on prototype
    (goog/inherits ctor super-class)
    (doseq [[k v] static-properties]
      (gobj/set ctor k v))
    ctor))

(defn create-component [init-fn static-properties method-names]
  (create-class react/Component init-fn static-properties method-names))

(defn create-pure-component [init-fn static-properties method-names]
  (create-class react/PureComponent init-fn static-properties method-names))

(def create-context
  "Just react/createContext"
  react/createContext)

(defn factory
  "Takes a React component, and creates a function that returns
  a new React element"
  [component]
  (partial $ component))

