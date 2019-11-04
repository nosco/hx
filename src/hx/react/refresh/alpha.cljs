(ns hx.react.refresh.alpha
  (:require ["react-refresh/runtime" :as refresh]))

(refresh/injectIntoGlobalHook js/window)

(defn ^:dev/after-load refresh! []
  (js/console.log (refresh/performReactRefresh)))
