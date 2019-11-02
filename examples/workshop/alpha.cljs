(ns workshop.alpha
  (:require [hx.react.alpha :as hx.alpha :refer [$ defnc]]
            [hx.react.dom.alpha :as d]
            [hx.hooks.alpha :as hooks]
            [devcards.core :as dc :include-macros true]))


(defnc state-test
  []
  (let [[{:keys [count]} set-count] (hooks/use-state {:count 0})]
    ;; [:div count [:button {:on-click #(set-count update :count inc)} "+"]]
    ;; ($ :div count
    ;;    ($ :button {:on-click #(set-count update :count inc)} "+"))
    (d/div count
           (d/button {:on-click #(set-count update :count inc)} "+"))))

(dc/defcard use-state
  (state-test))


(defnc effect-test
  []
  (let [[count set-count] (hooks/use-state 0)
        threes (quot count 3)]
    (hooks/use-effect
     (prn "--------")
     (prn "every render" count))
    (hooks/use-effect
     [count]
     (prn "every render 2" count))
    (hooks/use-effect
     [threes]
     (prn "3rd render" threes))
    (hooks/use-effect :auto-deps (prn "every render auto-deps" count))
    (hooks/use-effect :auto-deps (prn "3rd render auto-deps" threes))
    (d/div count (d/button {:on-click #(set-count inc)} "+"))))


(dc/defcard use-effect
  (effect-test))
