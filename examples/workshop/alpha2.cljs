(ns workshop.alpha2
  (:require [hx.react :as hx :refer [defnc]]
            [hx.hooks.alpha2 :as hooks]
            [devcards.core :as dc :include-macros true]))


(defnc state-test []
  (let [[count set-count] (hooks/use-state 0)]
    [:div count [:button {:on-click #(set-count inc)} "+"]]))


(dc/defcard use-state
  (hx/f [state-test]))


(defnc fx-test []
  (let [[count set-count] (hooks/use-state 0)
        threes (quot count 3)]
    (hooks/use-fx
     (prn "--------")
     (prn "every render" count))
    (hooks/use-fx
     [count]
     (prn "every render 2" count))
    (hooks/use-fx
     [threes]
     (prn "3rd render" threes))
    (hooks/use-fx :auto-deps (prn "every render auto-deps" count))
    (hooks/use-fx :auto-deps (prn "3rd render auto-deps" threes))
    [:div count [:button {:on-click #(set-count inc)} "+"]]))


(dc/defcard use-fx
  (hx/f [fx-test]))
