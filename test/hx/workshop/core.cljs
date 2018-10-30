(ns hx.workshop.core
  (:require [devcards.core :as dc :include-macros true]
            ["react" :as react]
            [hx.hiccup]))

(defn example [props]
  (react/createElement "div" nil (prn-str props)))

(dc/defcard test
  (hx.hiccup/parse [:div {:style {:background-color "red"}}
                    [:h1 "foo"]
                    [:button {:on-click #(js/alert "hi")} "click"]]))

(defn ^:dev/after-load start! []
  (dc/start-devcard-ui!))

(defn init! [] (start!))

(init!)
