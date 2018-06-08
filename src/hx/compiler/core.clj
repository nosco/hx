(ns hx.compiler.core
  (:require [clojure.zip :as zip]
            [hx.compiler.parser :as parser]
            [hx.compiler.analyzer :as analyzer]
            [hx.compiler.generator :as generator])
  (:refer-clojure :exclude [compile]))

(defn compile-hiccup [hiccup create-element]
  (-> hiccup
      (parser/parse)
      (analyzer/analyze)
      (generator/generate create-element)))

(defn seq-vec-zip [root]
  (zip/zipper
   ;; branch?
   (fn [v] (or (seq? v) (vector? v)))

   ;; children
   seq

   ;; make-node
   (fn [node children]
     (if (vector? node)
       (with-meta (vec children) (meta node))
       (with-meta children (meta node))))

   root))

(defn convert-compile-sym [form sym]
  (loop [loc (seq-vec-zip form)]
    (if (not (zip/end? loc))
      (let [node (zip/node loc)]
        (if (= node sym)
          ;; remove the $ symbol
          (let [no$ (zip/remove loc)
                next (zip/next no$)]
            (-> next
                ;; add the hx macro to the next form
                (zip/replace
                 (list 'hx.compiler.core/compile-hiccup (zip/node next)))
                (recur)))

          (recur (zip/next loc))))
      (zip/root loc))))

(defn compile* [form create-element]
  (-> form
      (convert-compile-sym '$)
      (compile-hiccup create-element)))

#_(compile*
   '($[:div
       x
       [widget]
       [:span "wat"]
       (when false
         $[:div "heh"])
       [:input {:type "button"} [:i {:id "jkl"} "sup"]]])
   'react/createElement)
