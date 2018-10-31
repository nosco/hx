(ns hx.workshop.core
  (:require [devcards.core :as dc :include-macros true]
            ["react" :as react]
            [hx.hiccup]
            [hx.react :as hx :include-macros true]))

(defn example [props]
  (react/createElement "div" nil (prn-str props)))

(dc/defcard hiccup
  (hx.hiccup/parse [:div {:style {:background-color "red"}}
                    [:h1 "foo"]
                    [:button {:on-click #(js/alert "hi")} "click"]]))

(hx/defnc defnc-example [{:keys [foo children]}]
  [:<>
   [:div "mm"]
   [:div foo]
   (let [x 1
         y 2]
     [:div (+ x y)])
   (for [n [1 2 3]]
     [:div {:key n} (+ n 1)])
   children])

(dc/defcard defnc
  (hx/$ defnc-example {:foo "bar"} "child"))

(hx/defnc rc [{:keys [children]}]
  [:div
   (children 3)])

(dc/defcard render-fn-child
  (hx/$ rc
        nil
        (fn [n]
          [:<>
           [:div (hx/$ "span" "hi")]
           [:span {:style {:color "red"}} (+ n 1)]])))

(hx/defnc shallow* [{:keys [name]}]
  [:div "Hello " [:span {:style {:color "blue"}} name] "!"])

(dc/defcard shallow
  (hx/shallow-render (shallow* {:name "Will"} nil)))

(hx/defcomponent class-comp
  (constructor [this]
               this)
  (render [this]
          [:h1 "foo"]))

(dc/defcard class-component
  (hx/$ class-comp nil nil))

(def some-context (react/createContext))

(hx/defnc context-consumer [_]
  [:div
   [(.-Consumer some-context)
    (fn [v]
      [:div v])]])

(hx/defnc context-provider [_]
  [(.-Provider some-context)
   {:value "context value"}
   [:div
    [context-consumer]]])

(dc/defcard context
  (hx/$ context-provider nil nil))

(hx/defnc ref-consumer* [{:keys [on-click] :as props} ref]
  (println ref)
  [:button {:ref ref :on-click on-click} "Click me"])

(def ref-consumer (react/forwardRef ref-consumer*))

(hx/defnc ref-provider [_]
  (def ref (react/createRef))
  [ref-consumer {:ref ref :on-click #(println ref)}])

(dc/defcard ref
  (hx/$ ref-provider nil nil))

(defn ^:dev/after-load start! []
  (dc/start-devcard-ui!))

(defn init! [] (start!))

(init!)
