(ns hx.workshop.core
  (:require [devcards.core :as dc :include-macros true]
            [hx.react :as react]))

(dc/defcard
  macroexpand
  (macroexpand '(react/compile
                $[:div {:style {:color "green"}
                        :id "asdf"} "hello"])))

(dc/defcard
  simple
  (react/compile
   $[:div {:style {:color "green"}
          :id "asdf"} "hello"]))

(dc/defcard
  with-children
  (react/compile
   $[:ul {:style {:background "lightgrey"}}
     [:li {:style {:font-weight "bold"}} "one"]
     [:li "two"]
     [:li "three"]]))

(dc/defcard
  conditional
  (react/compile
   $[:<>
     (when true
       $[:div "true"])
     (when false
       $[:div "false"])]))

(dc/defcard
  seq
  (react/compile
   $[:ul
     (list $[:li {:key 1} 1]
           $[:li {:key 2} 2])]))

(dc/defcard
  map*
  (react/compile
   $[:ul
     [:li "Title"]
     (map (fn [n] $[:li {:key n} n])
          [1 2 3 4 5])]))

(dc/defcard defnc
  (macroexpand '(react/defnc greeting [{:keys [name] :as props}]
                  (println props)
                  $[:span {:style {:font-size "24px"}}
                    "Hello, " name])))

(react/defnc greeting [{:keys [name] :as props}]
  $[:span {:style {:font-size "24px"}}
    "Hello, " name])

(dc/defcard
  function-element
  (react/compile
   $[greeting {:name "Will"}]))

(react/defnc with-children [{:keys [children]}]
  $[:div
    (identity children)])

(dc/defcard with-children
  (react/compile
   $[with-children
     [:span "hi"]
     [:div "watup"]]))

(dc/defcard defcomponent
  (macroexpand '(react/defcomponent some-component
                  (constructor [this]
                               this)
                  (render [this]
                          $[:div "sup component"]))))

(react/defcomponent
  some-component
  (constructor [this]
               this)
  (render [this]
          $[:div "sup component"]))

(dc/defcard class-element
  (react/compile
   $[some-component]))

(react/defcomponent stateful
  (constructor [this]
               (set! (.. this -state) #js {:name "Will"})
               this)
  (update-name! [this e]
                (. this setState #js {:name (.. e -target -value)}))
  (render [this]
          (let [state (. this -state)]
            $[:div
              [:div (. state -name)]
              [:input {:value (. state -name)
                       :on-change (. this -update-name!)}]])))

(dc/defcard stateful-element
  (react/compile
   $[stateful]))

(react/defcomponent static-property
  (constructor [this]
               this)

  ^:static
  (some-prop "1234")

  (render [this]
          $[:div (. static-property -some-prop)]))

(dc/defcard stateful-element
  (react/compile
   $[static-property]))

(defn ^:dev/after-load start! []
  (dc/start-devcard-ui!))

(defn init! [] (start!))

(init!)
