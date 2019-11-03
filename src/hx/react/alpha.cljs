(ns hx.react.alpha
  (:refer-clojure :exclude [type])
  (:require [goog.object :as gobj]
             [hx.utils :as utils]
             ["react" :as react])
  (:require-macros [hx.react.alpha]))


(def Fragment react/Fragment)


(def props->clj utils/props->clj)


(defn clj->props
  [x native?]
  (utils/clj->props x native?))


(def create-element react/createElement)


(defn $$ [type & args]
  (let [?p (first args)
        ?c (rest args)]
    (if (map? ?p)
      (apply create-element
             type
             (clj->props ?p (string? type))
             ?c)
      (apply create-element
             type
             nil
             args))))


(defprotocol IExtractType
  (-type [factory] "Extracts the underlying type from the factory function."))


(defn type
  [f]
  (-type f))


(defn factory
  "Creates a factory function for an external React component"
  [type]
  (-> (fn factory [& args]
        (if (map? (first args))
          (apply create-element type (clj->props (first args) false) (rest args))
          (apply create-element type nil args)))
      (specify! IExtractType
        (-type [_] type))))


(defn- wrap-cljs-component
  [type]
  ;; convert js props to clj props
  (let [wrapper (fn wrap-type-props [p r]
                  (type #js {:cljs-props (utils/props->clj p)} r))]
    (when js/goog.DEBUG
      (set! (.-displayName wrapper) (str "cljsProps(" (.-displayName type) ")")))
    wrapper))


(defn- extract-cljs-props
  [o]
  (gobj/get o "cljs-props"))
