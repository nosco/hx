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
     [:div (+ n 1)])
   children])

(dc/defcard defnc
  (hx/$ defnc-example {:foo "bar"} "child"))

(defn ^:dev/after-load start! []
  (dc/start-devcard-ui!))

(defn init! [] (start!))

(init!)
