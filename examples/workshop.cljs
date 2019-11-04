(ns workshop
  (:require [devcards.core :as dc :include-macros true]
            [workshop.core]
            [workshop.material]
            [workshop.react-dnd]
            [workshop.sortable]
            [workshop.sortable.alpha]
            [workshop.state]
            [workshop.alpha]
            [hx.react.refresh.alpha]))

(defn start! []
  (dc/start-devcard-ui!))

(defn init! [] (start!))

(init!)
