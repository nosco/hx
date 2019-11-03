(ns hx.react.alpha
  (:refer-clojure :exclude [type])
  #?@(:cljs ((:require [cljs-bean.core :as b]
                       [goog.object :as gobj]
                       [hx.utils :as utils]
                       ["react" :as react])
             (:require-macros [hx.react.alpha]))))


#?(:cljs (def Fragment react/Fragment))


#?(:cljs (def props->clj utils/props->clj))


#?(:cljs (defn clj->props
           [x native?]
           (utils/clj->props x native?)))


#?(:cljs (def create-element react/createElement))


(defmacro $
  [type & args]
  (if (map? (first args))
    (if (keyword? type)
      `(create-element
        ~(name type)
        (clj->props ~(first args) true)
        ~@(rest args))
      `(create-element
        ~type
        (clj->props ~(first args) false)
        ~@(rest args)))

    (if (keyword? type)
      `(create-element
        ~(name type)
        nil
        ~@args)
      `(create-element
        ~type
        nil
        ~@args))))


#?(:clj (defmacro <> [& children]
          `($ Fragment ~@children)))


(defprotocol IExtractType
  (-type [factory] "Extracts the underlying type from the factory function."))


(defn type [f]
  (-type f))


#?(:cljs (defn factory
           "Creates a factory function for an external React component"
           [type]
           (-> (fn factory [& args]
                 (if (map? (first args))
                   (apply create-element type (clj->props (first args) false) (rest args))
                   (apply create-element type nil args)))
               (specify! IExtractType
                 (-type [_] type)))))

#?(:cljs (defn- cljs-factory
           [type]
           (-> (fn hx-factory [& args]
                 (if (map? (first args))
                   (apply create-element type #js {:cljs-props (first args)} (rest args))
                   (apply create-element type nil args)))
               (specify! IExtractType
                 (-type [_]
                   ;; convert js props to clj props
                   (fn wrap-type-props [p r]
                     (type #js {:cljs-props (utils/props->clj p)} r)))))))

#?(:cljs (defn- extract-cljs-props
           [o]
           (gobj/get o "cljs-props")))


(defn- fnc*
  [display-name props-bindings body]
  (let [opts-map? (map? (first body))
        ret (gensym "return_value")]
    ;; maybe-ref for react/forwardRef support
    `(fn ~display-name
       [props# maybe-ref#]
       (let [~props-bindings [(extract-cljs-props props#) maybe-ref#]]
         (do ~@body)))))


(defmacro defnc
  [display-name props-bindings & body]
  (let [wrapped-name (symbol (str display-name "-hx-render"))]
    `(do (def ~wrapped-name ~(fnc* wrapped-name props-bindings body))
         (when goog/DEBUG
           (goog.object/set ~wrapped-name "displayName" ~(str *ns* "/" display-name)))
         (def ~display-name (-> ~wrapped-name
                                (cljs-factory))))))
