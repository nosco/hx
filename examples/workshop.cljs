(ns workshop
  (:require ["react-dom/client" :as react-dom]
            [hx.react :as hx]
            [workshop.core :as core]))

(defonce root-atom (atom nil))

(defn ^:dev/after-load render! []
  (when-let [root @root-atom]
    (.render root (hx/f [core/App]))))

(defn init! []
  (let [container (js/document.getElementById "app")
        root (react-dom/createRoot container)]
    (reset! root-atom root)
    (render!)))

(init!)
