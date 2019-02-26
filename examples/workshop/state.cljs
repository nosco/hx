(ns workshop.state
  (:require [devcards.core :as dc :include-macros true]
            [hx.react :as hx :refer [defnc]]
            [hx.hooks :as hooks :refer [<-state <-effect <-context]]
            ["react" :as react]))

;;
;; A simple counter
;;

(defnc Simple [_]
  (let [state (<-state 0)]
    [:<>
     [:div "Counter: " @state]
     [:div [:button {:on-click #(swap! state inc)} "inc"]]]))

(dc/defcard simple
  (hx/f [Simple]))


;;
;; A timer that increments each second
;;

(defnc Timer
  [opts]
  (let [seconds (<-state 0)]
    (<-effect (fn []
                (let [id (js/setInterval #(swap! seconds inc) 1000)]
                  (fn []
                    (js/clearInterval id))))
              [])
    [:div
     "Timer: " @seconds]))

(dc/defcard timer
  (hx/f [Timer]))


;;
;; Using React Context + Hooks for global state management
;;

(def app-state (react/createContext))

(defnc App [{:keys [children]}]
  (let [state (<-state {})]
    [(.-Provider app-state) {:value state}
     children]))

(defnc CounterConsumer [_]
  (let [state (<-context app-state)
        {:keys [counter]} @state]
    [:<>
     [:div "Counter: " counter]
     [:button {:on-click #(swap! state update :counter inc)} "inc"]]))

(defnc PrintStateConsumer [_]
  (let [state (<-context app-state)]
    [:pre (prn-str @state)]))

(defnc TimerConsumer [_]
  (let [state (<-context app-state)
        {:keys [timer]} @state]
    (<-effect (fn []
                (let [id (js/setInterval #(swap! state update :timer inc) 1000)]
                  (fn []
                    (js/clearInterval id))))
              [])
    [:<>
     [:div "Timer: " timer]]))

(dc/defcard context
  (hx/f [App
         [PrintStateConsumer]
         [TimerConsumer]
         [CounterConsumer]]))

