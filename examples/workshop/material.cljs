(ns workshop.material
  (:require [devcards.core :as dc :include-macros true]
            [hx.react :as hx :refer [defnc]]
            ["@material-ui/core/Button" :default Button]
            ["@material-ui/core/CircularProgress" :default CircularProgress]
            ["@material-ui/core/styles" :refer [withStyles]]))

(dc/defcard adding-styles*
  ;; add the Robot font globally on the page
  (hx/f [:link {:rel "stylesheet"
                :href "https://fonts.googleapis.com/css?family=Roboto:300,400,500"}]))

(defnc ButtonExample [_]
  [:<>
   (for [variant [nil "outlined" "contained" "fab" "extendedFab"]]
     [:div (or variant "default")
      (for [color [nil "primary" "secondary"]]
        [Button {:variant variant :color color
                 :style {:margin "5px"}}
         (or color "default")])])])

(dc/defcard button
  ;; hx/f is used here to instantiate the `ButtonExample` component as a React element
  ;; to work with devcards
  (hx/f [ButtonExample]))

(defnc CircularProgressExample [_]
  [:<>
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
                        :variant "static"}]]]])

(dc/defcard circular-progress
  (hx/f [CircularProgressExample]))

(def styles #js {:root #js {:backgroundColor "red" } })

(defnc PaperWithStyles [{:keys [classes]}] 
  {:wrap [((withStyles styles))]}
  [:div "testing"])

(dc/defcard with-styles
  (hx/f [PaperWithStyles]))
