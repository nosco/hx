(ns hx.benchmark
  (:require
    ["benchmark" :as b]
    ["react" :as react :rename {createElement rce}]
    ["react-dom/server" :as rdom]
    [hx.react :as hx]
    ;; [shadow.grove.react :as shadow]
    [reagent.core :as reagent]
    ))

(defn reagent-render [{:keys [title body]}]
  (reagent/as-element
    [:div.card
     [:div.card-title title]
     [:div.card-body body]
     [:div.card-footer
      [:div.card-actions
       [:button "ok"]
       [:button "cancel"]]]]))

;; (defn shadow-render [{:keys [title body]}]
;;   (shadow/<<
;;     [:div.card
;;      [:div.card-title title]
;;      [:div.card-body body]
;;      [:div.card-footer
;;       [:div.card-actions
;;        [:button "ok"]
;;        [:button "cancel"]]]]))

(defn react-render [{:keys [title body]}]
  (rce "div" #js {:className "card"}
    (rce "div" #js {:className "card-title"} title)
    (rce "div" #js {:className "card-body"} body)
    (rce "div" #js {:className "card-footer"}
      (rce "div" #js {:className "card-actions"}
        (rce "button" nil "ok")
        (rce "button" nil "cancel")))))

(defn hx-render [{:keys [title body]}]
  (hx/f
   [:div {:class "card"}
    [:div {:class "card-title"} title]
    [:div {:class "card-body"} body]
    [:div {:class "card-footer"}
     [:div {:class "card-actions"}
      [:button "ok"]
      [:button "cancel"]]]]))

(def ^:export hx-render-manual hx-render)

(defn log-cycle [event]
  (println (.toString (.-target event))))

(defn log-complete [event]
  (this-as this
    (js/console.log this)))

(defn ^:export main [& args]
  (let [test-data {:title "hello world"
                   :body "body"}
        test-data-js #js {:title "hello world"
                          :body "body"}]
    (println (rdom/renderToString (react-render test-data)))
    (println (rdom/renderToString (reagent-render test-data)))
    ;; (println (rdom/renderToString (shadow-render test-data)))
    (println (rdom/renderToString (hx-render test-data)))

    (when-not (= (rdom/renderToString (react-render test-data))
                 (rdom/renderToString (reagent-render test-data))
                 ;; (rdom/renderToString (shadow-render test-data))
                 (rdom/renderToString (hx-render test-data)))
      (throw (ex-info "not equal!" {})))

    (-> (b/Suite.)
        (.add "react" #(react-render test-data))
        (.add "reagent" #(reagent-render test-data))
        ;; (.add "shadow" #(shadow-render test-data))
        (.add "hx" #(hx-render test-data))
        (.on "cycle" log-cycle)
        ;; (.on "complete" log-complete)
        (.run))))
