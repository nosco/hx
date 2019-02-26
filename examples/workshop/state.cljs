(ns workshop.state
  (:require [devcards.core :as dc :include-macros true]
            [hx.react :as hx :refer [defnc]]
            [hx.hooks :as hooks :refer [<-state <-effect <-context]]
            ["react" :as react]))

(defnc Simple [_]
  (let [state (<-state 0)]
    [:<>
     [:div "Counter: " @state]
     [:div [:button {:on-click #(swap! state inc)} "inc"]]]))

(dc/defcard simple
  (hx/f [Simple]))

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

(def app-state (react/createContext))

(defnc App [{:keys [children]}]
  (let [state (<-state {:counter 0})]
    [(.-Provider app-state) {:value state}
     children]))

(defnc Counter [_]
  (let [state (<-context app-state)
        {:keys [counter]} @state]
    [:<>
     [:div "Counter: " counter]
     [:button {:on-click #(swap! state update :counter inc)} "inc"]]))

(defnc PrintState [_]
  (let [state (<-context app-state)]
    [:pre (prn-str @state)]))

(dc/defcard context
  (hx/f [App
         [PrintState]
         [Counter]]))

