(ns workshop.alpha
  (:require [hx.react.alpha :as hx.alpha :refer [$ <> defnc]]
            [hx.react.dom.alpha :as d]
            [hx.hooks.alpha :as hooks]
            [devcards.core :as dc :include-macros true]))


(defnc subcomponent [{:keys [name] :as props}]
  (d/div name))


(dc/defcard $
  ($ (hx.alpha/type subcomponent) {:name "$ works"}))


(defnc state-test
  []
  (let [[{:keys [name]} set-state] (hooks/use-state {:name "asdf"})]
    (d/div
     (d/input {:value name
               :on-change #(set-state assoc :name (.. % -target -value))})
     (subcomponent {:name name}))))


(dc/defcard use-state
  (state-test))


(defnc display-range [{:keys [end color]}]
  (for [n (range end)]
    (d/div {:key n
            :style {:width "10px" :height "10px" :display "inline-block"
                    :background (or color "green") :margin "auto 2px"}})))

(defnc effect-test
  []
  (let [[count set-count] (hooks/use-state 0)
        renders (hooks/use-ref 1)
        [fx-state set-fx-state] (hooks/use-state {:every 0
                                                  :every/auto 0
                                                  :every-third 0
                                                  :every-third/auto 0})
        threes (quot count 3)]
    (hooks/use-effect
     (set! (.-current renders) (inc (.-current renders))))
    (hooks/use-effect
     [count]
     (set-fx-state assoc :every count))
    (hooks/use-effect
     [threes]
     (set-fx-state assoc :every-third threes))
    (hooks/use-effect
     :auto-deps
     (set-fx-state assoc :every/auto count))
    (hooks/use-effect
     :auto-deps
     (set-fx-state assoc :every-third/auto threes))

    (d/div
     (d/button {:on-click #(set-count inc)} "inc")
     (d/div
      (d/div "renders:")
      (display-range {:end (.-current renders) :color "red"}))
     (for [[k v] fx-state]
       (d/div
        (d/div (str k))
        (display-range {:end v}))))))


(dc/defcard use-effect
  (effect-test))


(defnc lazy-test
  [{:keys [begin end]}]
  (<>
   (d/div (str "numbers " (or begin 0) "-" (dec end) ":"))
   (d/ul
    (for [n (range begin end)]
      (d/li n)))
   (d/div "ur welcome")))


(dc/defcard lazy
  (lazy-test {:end 6}))
