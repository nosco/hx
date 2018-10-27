(ns hx.react.interceptors
  (:require [hx.hiccup])
  (:refer-clojure :exclude [compile]))

(def compile
  {:name ::compile
   :enter
   (fn [{:keys [in] :as context}]
     (assoc context
            :out (hx.hiccup/compile-hiccup
                  in
                  'hx.react/$)))})
