(ns workshop.material
  (:require [devcards.core :as dc :include-macros true]
            [hx.react :as hx]
            ["@material-ui/core/Button" :default Button]
            ["@material-ui/core/CircularProgress" :default CircularProgress]))

(hx/defnc MuStyles [_]
  [:link {:rel "stylesheet"
          :href "https://fonts.googleapis.com/css?family=Roboto:300,400,500"}])

(dc/defcard adding-styles*
  (hx/f [MuStyles]))

(dc/defcard button
  (hx/f [:<>
         (for [variant [nil "outlined" "contained" "fab" "extendedFab"]]
           [:div (or variant "default")
            (for [color [nil "primary" "secondary"]]
              [Button {:variant variant :color color
                       :style {:margin "5px"}}
               (or color "default")])])]))

(dc/defcard circular-progress
  (hx/f [:<>
         [:div "Indeterminate "
          [:div {:style {:padding "5px"}}
           [CircularProgress {:color "primary"}]]]
         [:div "Determinate"
          [:div {:style {:padding "5px"}}
           [CircularProgress {:color "primary" :value 33
                              :variant "determinate"}]]]
         [:div "Static"
          [:div {:style {:padding "5px"}}
           [CircularProgress {:color "primary" :value 33
                              :variant "static"}]]]]))
