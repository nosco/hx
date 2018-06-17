(ns hx.workshop.core
  (:require [devcards.core :as dc :include-macros true]
            [hx.react :as hx :include-macros true]
            [hx.state]
            [cljs.js]))

(dc/defcard
  macroexpand
  (macroexpand '(hx/compile
                 $ [:div {:style {:color "green"}
                          :id "asdf"} "hello"])))

(dc/defcard
  simple
  (hx/compile
   $ [:div {:style {:color "green"}
            :id "asdf"} "hello"]))

(dc/defcard
  with-children
  (hx/compile
   $ [:ul {:style {:background "lightgrey"}}
      [:li {:style {:font-weight "bold"}} "one"]
      [:li "two"]
      [:li "three"]]))

(dc/defcard
  conditional
  (hx/compile
   $ [:<>
      (when true
        $ [:div "true"])
      (when false
        $ [:div "false"])]))

(dc/defcard
  seq
  (hx/compile
   $ [:ul
      (list $ [:li {:key 1} 1]
            $ [:li {:key 2} 2])]))

(dc/defcard
  map
  (hx/compile
   (let [numbers [1 2 3 4 5]]
     $ [:ul {:style {:list-style-type "square"}}
        (map #(do $ [:li {:key %} %])
             numbers)])))

(dc/defcard css-class
  (hx/compile
   $ [:<>
      [:style {:dangerouslySetInnerHTML #js {:__html ".foo { color: lightblue }"}}]
      [:div {:className "foo"} "asdf jkl"]
      [:div {:class "foo"} "1234 bnm,"]]))

(dc/defcard defnc
  (macroexpand '(hx/defnc greeting [{:keys [name] :as props}]
                  (println props)
                  $ [:span {:style {:font-size "24px"}}
                     "Hello, " name])))

(hx/defnc greeting [{:keys [name] :as props}]
  $ [:span {:style {:font-size "24px"}}
     "Hello, " name])

(dc/defcard
  function-element
  (hx/compile
   $ [greeting {:name "Will"}]))

(hx/defnc with-children [{:keys [children]}]
  $ [:div
     (identity children)])

(dc/defcard with-children
  (hx/compile
   $ [with-children
      [:span "hi"]
      [:div "watup"]]))

(dc/defcard defcomponent
  (macroexpand '(hx/defcomponent some-component
                  (constructor [this]
                               this)
                  (render [this]
                          $ [:div "sup component"]))))

(hx/defcomponent
  some-component
  (constructor [this]
               this)
  (render [this]
          $ [:div "sup component"]))

(dc/defcard class-element
  (hx/compile
   $ [some-component]))

(hx/defcomponent stateful
  (constructor [this]
               (set! (.. this -state) #js {:name "Will"})
               this)
  (update-name! [this e]
                (. this setState #js {:name (.. e -target -value)}))
  (render [this]
          (let [state (. this -state)]
            $ [:div
               [:div (. state -name)]
               [:input {:value (. state -name)
                        :on-change (. this -update-name!)}]])))

(dc/defcard stateful-element
  (hx/compile
   $ [stateful]))

(hx/defcomponent static-property
  (constructor [this]
               this)

  ^:static
  (some-prop "1234")

  (render [this]
          $ [:div (. static-property -some-prop)]))

(dc/defcard static-property
  (hx/compile
   $ [static-property]))

(hx/defcomponent fn-as-child
  (constructor [this]
               (set! (. this -state) #js {:name "Will"})
               this)
  (update-name! [this e]
                (. this setState #js {:name (.. e -target -value)}))
  (render [this]
          (let [state (. this -state)]
            $ [:div
               [:div ((.. this -props -children) (. state -name))]
               [:input {:value (. state -name)
                        :on-change (. this -update-name!)}]])))

(dc/defcard fn-as-child
  (hx/compile
   $ [fn-as-child
      (fn [name]
        $ [:span {:style {:color "red"}} name])]))

(hx/defcomponent render-prop
  (constructor [this]
               (set! (. this -state) #js {:name "Will"})
               this)
  (update-name! [this e]
                (. this setState #js {:name (.. e -target -value)}))
  (render [this]
          (let [state (. this -state)]
            $ [:div
               [:div ((.. this -props -render) (. state -name))]
               [:input {:value (. state -name)
                        :on-change (. this -update-name!)}]])))

(dc/defcard render-prop
  (hx/compile
   $ [render-prop
      {:render (fn [name]
                 $ [:span {:style {:color "red"}} name])}]))

(def js-interop-test
  (fn
    [props]
    (js/JSON.stringify props)))

(dc/defcard js-interop-nested-props
  (hx/compile
   $ [js-interop-test {:nested {:thing {:foo {:bar "baz"}}}}]))

(defonce my-state (atom ""))

;; (cljs.pprint/pprint (macroexpand '
(hx.state/defrc
  reactive
  [props]
  (println "my-state:\"" @my-state "\"")
  $[:div
    [:div "hello " @my-state]
    [:div [:input {:type "text"
                   :value @my-state
                   :on-change #(reset! my-state (.. % -target -value))}]]])
;; ))

;; (println (macroexpand '@my-state))

(dc/defcard reactive
  (hx/compile
   $[reactive {:asdf "jkl"}]))

(defn ^:dev/after-load start! []
  (dc/start-devcard-ui!))

(defn init! [] (start!))

(init!)
