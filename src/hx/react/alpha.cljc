(ns hx.react.alpha
  (:refer-clojure :exclude [type])
  #?@(:cljs ((:require [goog.object :as gobj]
                       [hx.utils :as utils]
                       ["react" :as react])
             (:require-macros [hx.react.alpha]))))


#?(:cljs (def Fragment react/Fragment))


#?(:cljs (def props->clj utils/props->clj))


#?(:cljs (defn clj->props
           [x native?]
           (utils/clj->props x native?)))


#?(:cljs (def create-element react/createElement))


#?(:cljs (defn $$ [type & args]
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
                      args)))))


(defmacro $
  [type & args]
  (if (map? (first args))
    `(create-element
      ~(if (keyword? type)
         (name type)
         type)
      (clj->props ~(first args) ~(keyword? type))
      ~@(rest args))

    ;; bail to runtime detection of props
    `($$ ~(if (keyword? type)
            (name type)
            type)
         ~@args)))


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

#?(:cljs (defn- wrap-cljs-component
           [type]
           ;; convert js props to clj props
           (let [wrapper (fn wrap-type-props [p r]
                           (type #js {:cljs-props (utils/props->clj p)} r))]
             (when js/goog.DEBUG
               (set! (.-displayName wrapper) (str "cljsProps(" (.-displayName type) ")")))
             wrapper)))

#?(:cljs (defn- extract-cljs-props
           [o]
           (gobj/get o "cljs-props")))


(defn- fnc*
  [display-name props-bindings body]
  (let [ret (gensym "return_value")]
    ;; maybe-ref for react/forwardRef support
    `(fn ~display-name
       [props# maybe-ref#]
       (let [~props-bindings [(extract-cljs-props props#) maybe-ref#]]
         (do ~@body)))))


(defmacro defnc
  [display-name props-bindings & body]
  (let [wrapped-name (symbol (str display-name "-hx-render"))
        opts-map? (map? (first body))
        opts (if opts-map?
               (first body)
               {})]
    `(do (def ~wrapped-name ~(fnc* wrapped-name props-bindings body))
         (when goog/DEBUG
           (goog.object/set ~wrapped-name "displayName" ~(str *ns* "/" display-name)))
         (def ~display-name (-> ~wrapped-name
                                (wrap-cljs-component)
                                ~@(-> opts :wrap)
                                (factory))))))
