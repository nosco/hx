(ns hx.compiler.analyzer
  (:require [clojure.walk :as walk]))

(defn child? [leaf]
  (or (:hx/parsed leaf)
      (string? leaf)
      (seq? leaf)))

(defn make-node [leaf & {:keys [props children js-interop?]}]
  (-> leaf
      (assoc
       :hx/analyzed true
       :props props
       :children children
       :js-interop? js-interop?)
      (dissoc :args)))

(defmulti analyze-element
  (fn [leaf]
    (:el leaf))
  :default ::default)

(defmethod analyze-element
  ::default
  [leaf]
  (if (:hx/parsed leaf)
    (let [{:keys [el args]} leaf]
      (if (child? (first args))
        ;; first arg is an element
        (make-node
         leaf
         :props nil
         :children args
         :js-interop? (string? el))

        (make-node
         leaf
         :props (first args)
         :children (rest args)
         :js-interop? (string? el))))
    leaf))

(defn analyze [tree]
  (walk/prewalk analyze-element tree))

#_(analyze
   (hx.compiler.parser/parse
    [:div [:span 'x "wat"]
     [:input {:type "button"} [:i {:id "jkl"} "sup"]]]))
#_(analyze (hx.compiler.parser/parse ['asdf [:span 'jkl "wat"]]))
