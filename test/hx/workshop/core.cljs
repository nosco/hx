(ns hx.workshop.core
  (:require [devcards.core :as dc :include-macros true]
            [hx.react :as hx :include-macros true]
            [hx.react.dom :as d :include-macros true]
            [cljs.js]))

(dc/defcard
  macroexpand
  (macroexpand '(hx/c [:div {:style {:color "green"}
                             :id "asdf"} "hello"])))

(dc/defcard
  simple
  (hx/c [:div {:style {:color "green"}
               :id "asdf"} "hello"]))

(dc/defcard
  with-children
  (hx/c [:ul {:style {:background "lightgrey"}}
         [:li {:style {:font-weight "bold"}} "one"]
         [:li "two"]
         [:li "three"]]))

(dc/defcard
  conditional
  (hx/c [:<>
         (when true
           (hx/c [:div "true"]))
         (when false
           (hx/c [:div "false"]))]))

(dc/defcard
  seq
  (hx/c [:ul
         (list (hx/c [:li {:key 1} 1])
               (hx/c [:li {:key 2} 2]))]))

(dc/defcard
  map
  (let [numbers [1 2 3 4 5]]
    (hx/c [:ul {:style {:list-style-type "square"}}
           (map #(hx/c [:li {:key %} %])
                numbers)])))

(dc/defcard css-class
  (hx/c [:<>
         [:style {:dangerouslySetInnerHTML #js {:__html ".foo { color: lightblue }"}}]
         [:div {:className "foo"} "asdf jkl"]
         [:div {:class "foo"} "1234 bnm,"]]))

(dc/defcard defnc
  (macroexpand '(hx/defnc greeting [{:keys [name] :as props}]
                  (println props)
                  (hx/c [:span {:style {:font-size "24px"}}
                     "Hello, " name]))))

(hx/defnc greeting [{:keys [name] :as props}]
  (hx/c [:span {:style {:font-size "24px"}}
         "Hello, " name]))

(dc/defcard
  function-element
  (hx/c [greeting {:name "Will"}]))

(hx/defnc with-children [{:keys [children]}]
  (hx/c [:div
         (identity children)]))

(dc/defcard with-children
  (hx/c [with-children
         [:span "hi"]
         [:div "watup"]]))

(dc/defcard defcomponent
  (macroexpand '(hx/defcomponent some-component
                  (constructor [this]
                               this)
                  (render [this]
                          (hx/c [:div "sup component"])))))

(hx/defcomponent
  some-component
  (constructor [this]
               this)
  (render [this]
          (hx/c [:div "sup component"])))

(dc/defcard class-element
  (hx/c [some-component]))

(hx/defcomponent stateful
  (constructor [this]
               (set! (.. this -state) #js {:name "Will"})
               this)
  (update-name! [this e]
                (. this setState #js {:name (.. e -target -value)}))
  (render [this]
          (let [state (. this -state)]
            (hx/c [:div
               [:div (. state -name)]
               [:input {:value (. state -name)
                        :on-change (. this -update-name!)}]]))))

(dc/defcard stateful-element
  (hx/c [stateful]))

(hx/defcomponent static-property
  (constructor [this]
               this)

  ^:static
  (some-prop "1234")

  (render [this]
          (hx/c [:div (. static-property -some-prop)])))

(dc/defcard static-property
  (hx/c [static-property]))

(hx/defcomponent fn-as-child
  (constructor [this]
               (set! (. this -state) #js {:name "Will"})
               this)
  (update-name! [this e]
                (. this setState #js {:name (.. e -target -value)}))
  (render [this]
          (let [state (. this -state)]
            (hx/c [:div
                   [:div ((.. this -props -children) (. state -name))]
                   [:input {:value (. state -name)
                            :on-change (. this -update-name!)}]]))))

(dc/defcard fn-as-child
  (hx/c [fn-as-child
         (fn [name]
           (hx/c [:span {:style {:color "red"}} name]))]))

(hx/defcomponent render-prop
  (constructor [this]
               (set! (. this -state) #js {:name "Will"})
               this)
  (update-name! [this e]
                (. this setState #js {:name (.. e -target -value)}))
  (render [this]
          (let [state (. this -state)]
            (hx/c [:div
                   [:div ((.. this -props -render) (. state -name))]
                   [:input {:value (. state -name)
                            :on-change (. this -update-name!)}]]))))

(dc/defcard render-prop
  (hx/c [render-prop
         {:render (fn [name]
                    (hx/c [:span {:style {:color "red"}} name]))}]))

(def js-interop-test
  (fn
    [props]
    (js/JSON.stringify props)))

(dc/defcard js-interop-nested-props
  (hx/c [js-interop-test {:nested {:thing {:foo {:bar "baz"}}}}]))

(hx/defnc _s-exp [_]
  (d/open (div "bar")))

(def s-exp* (hx/factory _s-exp))

(dc/defcard s-exp
  (s-exp*))

(defn ^:dev/after-load start! []
  (dc/start-devcard-ui!))

(defn init! [] (start!))

(init!)
