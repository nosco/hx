(ns workshop.state
  (:require [devcards.core :as dc :include-macros true]
            [hx.react :as hx :refer [defnc]]
            [hx.hooks :as hooks :refer [<-state-once]]
            ["react" :as react]))

;;
;; A simple counter
;;

(defnc Simple [_]
  (let [[state set-state] (<-state-once ::simple 0)]
    [:<>
     [:div "Counter: " state]
     [:div [:button {:on-click #(set-state inc)} "inc"]]]))

(dc/defcard simple
  (hx/f [Simple]))


;;
;; A timer that increments each second
;;

(defnc Timer
  [opts]
  (let [[seconds update-seconds] (hooks/useState 0)]
    (hooks/useEffect (fn []
                (let [id (js/setInterval #(update-seconds inc) 1000)]
                  (fn []
                    (js/clearInterval id))))
              [])
    [:div
     "Timer: " seconds]))

(dc/defcard timer
  (hx/f [Timer]))


;;
;; Using React Context + Hooks for global state management
;;

(def app-state (react/createContext))

(defn state-reducer [state [type & args]]
  (case type
    :timer/tick (update state :timer inc)
    :counter/click (update state :counter inc)
    (do (js/console.warn "Unknown action type" type)
        state)))

(defnc App [{:keys [children]}]
  (let [[state dispatch] (hooks/useReducer state-reducer {})]
    [:provider {:context app-state
                :value [state dispatch]}
     children]))

(defnc CounterConsumer [_]
  (let [[state dispatch] (hooks/useContext app-state)
        {:keys [counter]} state]
    [:<>
     [:div "Counter: " counter]
     [:button {:on-click #(dispatch [:counter/click])} "inc"]]))

(defnc PrintStateConsumer [_]
  (let [[state] (hooks/useContext app-state)]
    [:pre (prn-str state)]))

(defnc TimerConsumer [_]
  (let [[state dispatch] (hooks/useContext app-state)
        {:keys [timer]} state]
    (hooks/useEffect (fn []
                (let [id (js/setInterval #(dispatch [:timer/tick]) 1000)]
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

