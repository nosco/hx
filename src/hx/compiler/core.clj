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

(defn seq-vec-map-zip [root]
  (zip/zipper
   ;; branch?
   (fn [v] (or (seq? v) (vector? v) (map? v)))

   ;; children
   seq

   ;; make-node
   (fn [node children]
     (cond
       (vector? node)
       (with-meta (vec children) (meta node))

       (map? node)
       (with-meta (into {} children) (meta node))

       :else
       (with-meta children (meta node))))

   root))

(defn convert-compile-sym [form sym create-element]
  (loop [loc (seq-vec-map-zip form)]
    (if (not (zip/end? loc))
      (let [node (zip/node loc)]
        (if (= node sym)
          ;; remove the $ symbol
          (let [no$ (zip/remove loc)
                next (zip/next no$)]
            (-> next
                ;; add the hx macro to the next form
                (zip/replace
                 (hx.compiler.core/compile-hiccup
                  (zip/node next)
                  create-element))
                (recur)))

          (recur (zip/next loc))))
      (zip/root loc))))

#_(convert-compile-sym
   '($[:div
       x
       [widget]
       [:span "wat"]
       (when false
         $[:div "heh"])
       [:input {:type "button"} [:i {:id "jkl"} "sup"]]])
   '$
   'react/createElement)
