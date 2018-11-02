(ns workshop.react-dnd
  (:require [devcards.core :as dc :include-macros true]
            [hx.react :as hx :include-macros true]
            [goog.object :as gobj]
            ["react-dnd" :as dnd]
            ["react-dnd-html5-backend" :default html5-backend]))

;; Used by react-dnd to handle drag events and map to props for our component
(def box-spec
  #js {:beginDrag (fn [props]
                    #js {:left (gobj/get props "left")
                         :top (gobj/get props "top")
                         :text (gobj/get props "text")})})

(hx/defnc BoxRender [{:keys [connect-drag-source top left text] :as props}]
  (connect-drag-source
   ;; we have to call hx/f directly here since `connect-drag-source` expects
   ;; a react element as it's argument, and returns a react element
   (hx/f [:div {:style {:top top
                        :left left
                        :position "absolute"
                        :border "1px solid #333"}}
          text])))

(def DraggableBox
  ;; use react-dnd's HOC here to make it draggable
  (-> BoxRender
      ((dnd/DragSource "box"
                       box-spec
                       ;; this maps the react-dnd context to props for our
                       ;; component
                       (fn [connect]
                         #js {:connectDragSource (. connect dragSource)})))))

(hx/defcomponent Container
  (constructor [this]
               ;; set some initial state
               (set! (. this -state) #js {:box {:top 0 :left 0}})
               this)

  (moveBox [this left top]
           ;; update the state on move
           (. this setState #js {:box {:left left :top top}}))

  (render [this]
          (let [{:keys [top left]} (.. this -state -box)
                connect-drop-target (.. this -props -connectDropTarget)]
            (connect-drop-target
             (hx/f [:div {:style {:height 300 :width 300
                                  :border "1px solid black"
                                  :position "relative"}}
                    [DraggableBox {:top top
                                   :left left
                                   :text "Drag me!"}]])))))

;; used by react-dnd to handle drop events and map to state changes
;; in our component
(def target-spec
  #js {:drop (fn [props monitor component]
               (if (nil? component) nil
                   (let [item (. monitor getItem)
                         delta (. monitor getDifferenceFromInitialOffset)
                         left (. js/Math round (+ (. item -left) (. delta -x)))
                         top (. js/Math round (+ (. item -top) (. delta -y)))]
                     (. component moveBox left top))))})

(def DropContainer
  (-> Container
      ;; use react-dnd's HOCs to create a drop target
      ;; and create an HTML5 context
      ((dnd/DropTarget
        "box"
        target-spec
        (fn [connect monitor]
          #js {:connectDropTarget (. connect dropTarget)})))
      ((dnd/DragDropContext html5-backend))))

(dc/defcard drag-and-drop
  (hx/f [DropContainer]))
