(ns hx.compiler.core
  (:require [clojure.zip :as zip]
            [hx.compiler.parser :as parser]
            [hx.compiler.analyzer :as analyzer]
            [hx.compiler.generator :as generator]
            [hx.compiler.interceptor :as interceptor])
  (:refer-clojure :exclude [compile]))

(defn compile-hiccup [hiccup create-element & {:keys [interceptors]
                                               :or {interceptors []}}]
  (as->
      {::parser/form hiccup
       ::generator/create-element create-element} context

    (apply interceptor/execute
           context
           (concat
            interceptors
            [parser/interceptor
             analyzer/interceptor
             generator/interceptor]))

    (::generator/out context)))

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

(defn transform-sym [form sym transform]
  (loop [loc (seq-vec-map-zip form)]
    (if (not (zip/end? loc))
      (let [node (zip/node loc)]
        (if (= node sym)
          ;; remove the sym
          (let [no-sym (zip/remove loc)
                next (zip/next no-sym)]
            (-> next
                ;; add the hx macro to the next form
                (zip/replace
                 (transform
                  (zip/node next)))
                (recur)))

          (recur (zip/next loc))))
      (zip/root loc))))

(defn convert-compile-sym [form sym create-element & {:keys [interceptors]
                                                      :or {interceptors []}}]
  (transform-sym form sym
                 #(compile-hiccup % create-element
                                  :interceptors interceptors)))

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
