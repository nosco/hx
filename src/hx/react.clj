(ns hx.react
  (:require [hx.compiler.core]
            [hx.compiler.parser :as parser])
  (:refer-clojure :exclude [compile]))

(defmacro compile [form]
  (hx.compiler.core/compile*
   form
   'hx.react/create-element))

(defmacro defcomponent [display-name constructor & body]
  (let [with-compile (hx.compiler.core/convert-compile-sym
                      '$
                      'hx.react/create-element body)
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
  (let [with-compile (hx.compiler.core/convert-compile-sym
                      '$
                      'hx.react/create-element body)]
    `(defn ~name [props#]
       (let [~@props-bindings (hx.react/props->clj props#)]
         ~@with-compile))))

(defmethod parser/parse-element
  :<>
  [el props & children]
  (parser/-parse-element
   'hx.react/fragment
   props
   children))

#_(macroexpand
   '(compile
     [:<>
      [:div "wat"
       [:asdf {:class ["123"]}]]]))
