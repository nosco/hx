(ns refresh-example
  (:require [hx.react.alpha :as hx :refer [<> defnc]]
            [hx.react.dom.alpha :as d]
            [hx.hooks.alpha :as hooks]
            [hx.react.refresh.alpha :as refresh]
            ["react-dom" :as rdom]))

(defnc app
  []
  (let [[name set-name] (hooks/use-state "John")]
    (d/div
     {:style {:text-align "center"
              :padding "10px"
              :color "green"
              :font-family "sans-serif"}}
     (d/div "Hello, " name "!")
        (d/div
         (d/input {:value name :on-change #(set-name (.. % -target -value))})))))


(defn ^:export start []
  (rdom/render
   (app)
   (js/document.getElementById "app")))
