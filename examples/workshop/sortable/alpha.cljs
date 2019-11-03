(ns workshop.sortable.alpha
  (:require [devcards.core :as dc :include-macros true]
            [hx.hooks.alpha :as hooks]
            [hx.react.alpha :as hx :refer [$ defnc]]
            [hx.react.dom.alpha :as d]
            ["react-sortable-hoc" :as sort]))

(defnc Item [{:keys [value]}]
  (d/li value))

(def SortableItem (-> (hx/type Item)
                      (sort/SortableElement)
                      (hx/factory)))

(defnc ItemList [{:keys [items]}]
  (d/ul (map-indexed
         (fn [i v]
           (SortableItem {:key (str "item-" i)
                          :index i
                          :value v}))
         items)))

(def SortableList (-> (hx/type ItemList)
                      (sort/SortableContainer)
                      (hx/factory)))

;; move-item takes the previous list of items and a SortableContainer
;; move event, and computes the new list of items.
(defn move-item [items ev]
  (let [old-index (.-oldIndex ^js ev)
        new-index (.-newIndex ^js ev)]
    (sort/arrayMove items old-index new-index)) )

(defnc SortableComponent [_]
  ;; use the useState Hook to keep track of and update the state
  (let [[items update-items] (hooks/use-state #js ["Item 1"
                                                   "Item 2"
                                                   "Item 3"
                                                   "Item 4"
                                                   "Item 5"
                                                   "Item 6"])]
    (d/div
     "Click and drag an item to re-arrange them!"
     (SortableList {:items items
                    :onSortEnd #(update-items move-item %)}))))

(dc/defcard example
  (SortableComponent))
