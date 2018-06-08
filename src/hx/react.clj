(ns hx.react
  (:require [hx.core])
  (:refer-clojure :exclude [compile]))

(defmacro compile [_ form]
  (hx.core/compile
   {:create-element 'hx.react/create-element}
   form))

(defmacro defcomponent [display-name constructor & body]
  (let [with-compile (hx.core/convert-compile-sym
                      '$
                      {:create-element 'hx.react/create-element} body)
        method-names (into [] (map #(list 'quote (first %)) body))]
    `(def ~display-name
       (let [ctor# (fn ~(second constructor)
                     ;; constructor must return `this`
                     ~@(drop 2 constructor))
             class# (hx.react/create-pure-component
                     ctor#
                     {"displayName" (name '~display-name)}
                     ~method-names)]
         (cljs.core/specify! (.-prototype class#)
           ~'Object
           ~@with-compile)
         class#))))

(defmacro defnc [name props-bindings & body]
  (let [with-compile (hx.core/convert-compile-sym
                      '$
                      {:create-element 'hx.react/create-element} body)]
    `(defn ~name [props#]
       (let [~@props-bindings (hx.react/props->clj props#)]
         ~@with-compile))))

(defmethod hx.core/create-element
  :<>
  [options el props & children]
  (hx.core/-create-element 'hx.react/fragment options props children))
