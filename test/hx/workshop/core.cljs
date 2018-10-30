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

(hx/defnc defnc-example [props]
  (hx.hiccup/parse [:div "mm"]))

(dc/defcard defnc
  (react/createElement defnc-example #js {:foo "bar"} "child"))

(defn ^:dev/after-load start! []
  (dc/start-devcard-ui!))

(defn init! [] (start!))

(init!)
