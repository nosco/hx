(ns hx.react
  (:require [hx.compiler.core]
            [hx.compiler.parser :as parser])
  (:refer-clojure :exclude [compile]))

(defmacro compile [& form]
  (let [with-compile (hx.compiler.core/convert-compile-sym
                      form
                      '$
                      'hx.react/create-element)]
    `(do ~@with-compile)))

(defmacro defcomponent [display-name constructor & body]
  (let [with-compile (hx.compiler.core/convert-compile-sym
                      body
                      '$
                      'hx.react/create-element)
        methods (filter #(not (:static (meta %))) with-compile)
        statics (->> (filter #(:static (meta %)) with-compile)
                     (map #(apply vector (str (munge (first %))) (rest %)))
                     (into {"displayName" (name display-name)}))
        method-names (into [] (map #(list 'quote (first %)) methods))]
    `(def ~display-name
       (let [ctor# (fn ~(second constructor)
                     ;; constructor must return `this`
                     ~@(drop 2 constructor))
             class# (hx.react/create-pure-component
                     ctor#
                     ~statics
                     ~method-names)]
         (cljs.core/specify! (.-prototype class#)
           ~'Object
           ~@methods)
         class#))))

(defmacro defnc [name props-bindings & body]
  (let [with-compile (hx.compiler.core/convert-compile-sym
                      body
                      '$
                      'hx.react/create-element)]
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
     $[:<>
       [:div "wat"
        [:asdf {:class ["123"]}]]]))
