(ns hx.react.alpha
  #?@(:cljs ((:require [cljs-bean.core :as b]
                       [goog.object :as gobj]
                       [hx.utils :as utils]
                       ["react" :as react])
             (:require-macros [hx.react.alpha]))))

#?(:cljs (defn props->clj [x]
           (b/bean x)))

#?(:cljs (defn clj->props [x native?]
           (utils/clj->props x native?)))

#?(:cljs (def create-factory react/createFactory))

#?(:cljs (def create-element react/createElement))

(defmacro $ [type & args]
  (prn type args)
  (doto (if (map? (first args))
          (cond
            (or (symbol? type)
                (keyword? type)) `(create-element
                                   ~(name type)
                                   (clj->props ~(first args) true)
                                   ~@(rest args))
            true `(create-element
                   ~(name type)
                   (clj->props ~(first args))
                   ~@(rest args)))

          (cond
            (or (symbol? type)
                (keyword? type)) `(create-element
                                   ~(name type)
                                   nil
                                   ~@args)
            true `(create-element
                   ~(name type)
                   nil
                   ~@args)))
    (prn)))

(defn- fnc* [display-name props-bindings body]
  (let [opts-map? (map? (first body))
        ret (gensym "return_value")]
    ;; maybe-ref for react/forwardRef support
    `(fn ~display-name
       ([props#]
        (~display-name props# nil))
       ([props# maybe-ref#]
        (let [~props-bindings [(props->clj props#) maybe-ref#]]
          (do ~@body))))))

(defmacro defnc [display-name props-bindings & body]
  (let [wrapped-name (symbol (str display-name "-hx-render"))]
    `(do (def ~wrapped-name ~(fnc* wrapped-name props-bindings body))
         (when goog/DEBUG
           (goog.object/set ~wrapped-name "displayName" ~(str *ns* "/" display-name)))
         (def ~display-name (-> ~wrapped-name
                                (create-factory))))))
