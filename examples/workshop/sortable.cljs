(ns workshop.sortable
  (:require [devcards.core :as dc :include-macros true]
            [hx.react :as hx]
            [hx.react.hooks :as hooks :refer [<-state]]
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

;; move-item takes the previous list of items and a SortableContainer
;; move event, and computes the new list of items.
(defn move-item [items ev]
  (let [old-index (.-oldIndex ^js ev)
        new-index (.-newIndex ^js ev)]
    (sort/arrayMove items old-index new-index)) )

(hx/defnc SortableComponent [_]
  ;; use the <-state Hook to keep track of and update the state
  (let [items (<-state #js ["Item 1"
                            "Item 2"
                            "Item 3"
                            "Item 4"
                            "Item 5"
                            "Item 6"])]
    [:div
     "Click and drag an item to re-arrange them!"
     [SortableList {:items @items
                    :onSortEnd #(swap! items move-item %)}]]))

(dc/defcard example
  (hx/f [SortableComponent]))
