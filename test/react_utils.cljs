(ns react-utils
  (:require
   [goog.dom :as dom]
   [goog.object :as gobj]
   [clojure.string :as str]
   ["react-testing-library" :as rtl]))

(def render rtl/render)

(defn root [result]
  (-> result
      (.-container)
      (.-firstChild)))

(defn node= [x y]
  (.isEqualNode x y))

(defn html [s]
  (let [template (dom/createElement "template")]
    (gobj/set template "innerHTML" (str/trim s))
    (gobj/getValueByKeys template "content" "firstChild")))

(defn func []
  (let [call-count (atom 0)
        f (fn spy-func [& x]
            (swap! call-count inc))]
    (set! (.-callCount f) call-count)
    f))

(defn call-count [f]
  @(.-callCount f))

(defn click [node]
  (.click rtl/fireEvent node))

(defn change [node data]
  (.change rtl/fireEvent node data))
