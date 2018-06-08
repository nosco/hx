(ns hx.compiler.analyzer
  (:require [clojure.walk :as walk]))

(defn child? [leaf]
  (or (:hx/parsed leaf)
      (string? leaf)))

(defn make-node [leaf & {:keys [props children]}]
  (-> leaf
      (assoc
       :hx/analyzed true
       :props props
       :children children)
      (dissoc :args)))

(defn analyze-element [leaf]
  (if (:hx/parsed leaf)
    (let [{:keys [args]} leaf]
      (if (child? (first args))
        ;; first arg is an element
        (make-node
         leaf
         :props nil
         :children args)

        (make-node
         leaf
         :props (first args)
         :children (rest args))))
    leaf))

(defn analyze [tree]
  (walk/prewalk analyze-element tree))

#_(analyze
   (hx.compiler.parser/parse
    [:div [:span 'x "wat"]
     [:input {:type "button"} [:i {:id "jkl"} "sup"]]]))
#_(analyze (hx.compiler.parser/parse ['asdf [:span 'jkl "wat"]]))
