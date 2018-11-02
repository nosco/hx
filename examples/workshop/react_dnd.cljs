(ns workshop.react-dnd
  (:require [devcards.core :as dc :include-macros true]
            [hx.react :as hx :include-macros true]
            [goog.object :as gobj]
            ["react-dnd" :as dnd]
            ["react-dnd-html5-backend" :default html5-backend]))

(def card-source
  #js {:beginDrag (fn [props]
                    #js {:left (gobj/get props "left")
                         :top (gobj/get props "top")
                         :text (gobj/get props "text")})})

(defn collect [connect monitor]
  #js {:connectDragSource (. connect dragSource)
       :isDragging (. monitor isDragging)})

(hx/defnc BoxRender [{:keys [connect-drag-source top left text] :as props}]
  (connect-drag-source
   (hx/f [:div {:style {:top top
                        :left left
                        :position "absolute"
                        :border "1px solid #333"}}
          text])))

(def DraggableBox
  (-> BoxRender
      ((dnd/DragSource "box" card-source collect))))

(hx/defcomponent Container
  (constructor [this]
               (set! (. this -state) #js {:top 0 :left 0})
               this)

  (moveBox [this left top]
           (. this setState #js {:left left :top top}))

  (render [this]
          (let [top (.. this -state -top)
                left (.. this -state -left)
                connect-drop-target (.. this -props -connectDropTarget)]
            (connect-drop-target
             (hx/f [:div {:style {:height 300 :width 300
                                  :border "1px solid black"
                                  :position "relative"}}
                    [DraggableBox {:top top
                                   :left left
                                   :text "Drag me!"}]])))))

(def target-spec
  #js {:drop (fn [props monitor component]
               (if (nil? component) nil
                   (let [item (. monitor getItem)
                         delta (. monitor getDifferenceFromInitialOffset)
                         left (. js/Math round (+ (. item -left) (. delta -x)))
                         top (. js/Math round (+ (. item -top) (. delta -y)))]
                     (. component moveBox left top))))})

(def DropContainer (-> Container
                       ((dnd/DropTarget
                         "box"
                         target-spec
                         (fn [connect monitor]
                           #js {:connectDropTarget (. connect dropTarget)})))
                       ((dnd/DragDropContext html5-backend))))

(dc/defcard drag-and-drop
  (hx/f [DropContainer]))
