(ns hx.react.interceptors)

(def compile
  {:name ::compile
   :enter
   (fn [{:keys [in] :as context}]
     (assoc context
            :out (hx.compiler.core/compile-hiccup
                  in
                  'hx.react/create-element)))})

(def $-as-compile
  {:name ::$-as-compile
   :enter
   (fn [{:keys [in] :as context}]
     (assoc context
            :out (hx.compiler.core/convert-compile-sym
                  in
                  '$
                  'hx.react/create-element)))})
