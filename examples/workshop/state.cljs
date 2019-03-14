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
;; Demonstrating the issue with <-state and atom semantics
;;

(defnc AtomStateChild [{:keys [on-update]}]
  (let [state (<-state 0)]
    [:<>
     [:div "Child state: " @state]
     [:div [:button {:on-click (fn [e]
                                 (swap! state inc)
                                 (on-update @state))} "Inc"]]]))

(defnc AtomStateParent [_]
  (let [parent-state (<-state nil)]
    [:<>
     [:div "Parent state: " @parent-state]
     [AtomStateChild {:on-update #(reset! parent-state %)}]]))


(dc/defcard atom-state-issues
  (hx/f [AtomStateParent]))

(deftype AtomRef [react-ref]
  IDeref
  (-deref [_]
    (deref (.-current react-ref)))

  IReset
  (-reset! [_ v']
    (reset! (.-current react-ref) v'))

  ISwap
  (-swap! [o f]
    (swap! (.-current react-ref) f))
  (-swap! [o f a]
    (swap! (.-current react-ref) #(f % a)))
  (-swap! [o f a b]
    (swap! (.-current react-ref) #(f % a b)))
  (-swap! [o f a b xs]
    (swap! (.-current react-ref) #(apply f % a b xs))))


(defn <-atom [initial-value]
  (let [r (react/useRef (atom initial-value))
        k (gensym "<-atom")
        [v u] (react/useState initial-value)]
    (react/useEffect
     (fn []
       (add-watch (.-current r) k (fn [_ _ _ v']
                        (u v')))
       (fn [] (remove-watch (.-current r) k)))
     #js [])
    (AtomRef. r)))


(defnc NewAtomStateChild [{:keys [on-update]}]
  (let [state (<-atom 0)]
    [:<>
     [:div "Child state: " @state]
     [:div [:button {:on-click (fn [e]
                                 (swap! state inc)
                                 (on-update @state))} "Inc"]]]))

(defnc NewAtomStateParent [_]
  (let [parent-state (<-atom nil)]
    [:<>
     [:div "Parent state: " @parent-state]
     [NewAtomStateChild {:on-update #(reset! parent-state %)}]]))


(dc/defcard  new-atom-state-issues
  (hx/f [NewAtomStateParent]))

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
