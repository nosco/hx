(ns workshop.sortable
  (:require [devcards.core :as dc :include-macros true]
            [hx.react :as hx]
            ["react-sortable-hoc" :as sort]))

(hx/defnc Item [{:keys [value]}]
  [:li value])

(def SortableItem (sort/SortableElement Item))

(hx/defnc ItemList [{:keys [items]}]
  [:ul (map-indexed
        (fn [i v] [SortableItem {:key (str "item-" i)
                                 :index i
                                 :value v}])
        items)])

(def SortableList (sort/SortableContainer ItemList))

(hx/defcomponent SortableComponent
  (constructor
   [this]
   (set! (. this -state) #js {:items #js ["Item 1" "Item 2" "Item 3" "Item 4" "Item 5" "Item 6"]})
   this)

  (onSortEnd
   [this ev]
   (let [old-index (.-oldIndex ^js ev)
         new-index (.-newIndex ^js ev)]
     (. this setState
        #js {:items (sort/arrayMove (.. this -state -items)
                                    old-index new-index)})))

  (render
   [this]
   [SortableList {:items (.. this -state -items)
                  :onSortEnd (. this -onSortEnd)}]))

(dc/defcard example
  (hx/f [SortableComponent]))
