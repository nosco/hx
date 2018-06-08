(ns hx.workshop.core
  (:require [devcards.core :as dc :include-macros true]
            [hx.react :as react]))

(dc/defcard
  (react/compile
   [:div {:style {:color "green"}
          :id "asdf"} "hello"]))


(defn ^:dev/after-load start []
  (dc/start-devcard-ui!))
