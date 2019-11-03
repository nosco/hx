(ns workshop.alpha
  (:require [hx.react.alpha :as hx.alpha :refer [$ defnc]]
            [hx.react.dom.alpha :as d]
            [hx.hooks.alpha :as hooks]
            [cljs-bean.core :as b]
            [devcards.core :as dc :include-macros true]))


(defnc subcomponent [{:keys [name]}]
  (d/div name))


(dc/defcard $
  (hx.alpha/$ (hx.alpha/type subcomponent) {:name "$ works"}))


(defnc state-test
  []
  (let [[{:keys [name]} set-state] (hooks/use-state {:name "asdf"})]
    (d/div
     (d/input {:value name
               :on-change #(set-state assoc :name (.. % -target -value))})
     (subcomponent {:name name}))))


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


(defnc lazy-test
  [{:keys [begin end]}]
  (d/<>
   (d/div (str "numbers " (or begin 0) "-" (dec end) ":"))
   (d/ul
    (for [n (range begin end)]
      (d/li n)))
   (d/div "ur welcome")))


(dc/defcard lazy
  (lazy-test {:end 6}))
