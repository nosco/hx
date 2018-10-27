(ns hx.hiccup
  (:require [hx.hiccup.compiler.parser :as parser]
            [hx.hiccup.compiler.analyzer :as analyzer]
            [hx.hiccup.compiler.generator :as generator]
            [hx.hiccup.compiler.interceptor :as interceptor]))

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
