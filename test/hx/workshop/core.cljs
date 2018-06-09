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

(react/defnc greeting [{:keys [name]}]
  $[:span {:style {:font-size "24px"}}
    "Hello, " name])

(dc/defcard
  function-element
  (react/compile
   $[greeting {:name "Will"}]))


(defn ^:dev/after-load start! []
  (dc/start-devcard-ui!))

(defn init! [] (start!))

(init!)
