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
    (cond
      (keyword? type) `(create-element
                             ~(name type)
                             (clj->props ~(first args) true)
                             ~@(rest args))
      true `(create-element
             ~type
             (clj->props ~(first args) false)
             ~@(rest args)))

    (cond
      (keyword? type) `(create-element
                             ~(name type)
                             nil
                             ~@args)
      true `(create-element
             ~type
             nil
             ~@args))))

(defprotocol IExtractType
  (-type [factory] "Extracts the underlying type from the factory function."))


(defn type [f]
  (-type f))


#?(:cljs (defn create-factory [type]
           (-> (fn factory [& args]
                 (if (map? (first args))
                   (apply create-element type (clj->props (first args) false) (rest args))
                   (apply create-element type nil args)))
               (specify! IExtractType
                 (-type [_] type)))))


(defn- fnc*
  [display-name props-bindings body]
  (let [opts-map? (map? (first body))
        ret (gensym "return_value")]
    ;; maybe-ref for react/forwardRef support
    `(fn ~display-name
       [props# maybe-ref#]
       (let [~props-bindings [(props->clj props#) maybe-ref#]]
         (do ~@body)))))


(defmacro defnc
  [display-name props-bindings & body]
  (let [wrapped-name (symbol (str display-name "-hx-render"))]
    `(do (def ~wrapped-name ~(fnc* wrapped-name props-bindings body))
         (when goog/DEBUG
           (goog.object/set ~wrapped-name "displayName" ~(str *ns* "/" display-name)))
         (def ~display-name (-> ~wrapped-name
                                (create-factory))))))
