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


(defn ^:dev/after-load start! []
  (dc/start-devcard-ui!))

(defn init! [] (start!))

(init!)
