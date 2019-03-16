(ns hx.html
  (:require [hx.hiccup :as h]))

(defn props->attrs [props]
  ;; TODO
  "")

(defn create-element [el props & children]
  (cond
    (string? el) (str "<" el (props->attrs props) ">"
                      (apply str children)
                      "</" el ">")
    (ifn? el) (el (assoc props :children children))))

(defn fragment [{:keys [children]}]
  (apply str children))

(def html-hiccup-config
  {:create-element create-element
   :is-element? string?
   :is-element-type? (fn [x]
                       (ifn? x))
   :fragment fragment})

(defn f [form]
  (h/parse html-hiccup-config
           form))

(defmethod h/parse-element
  :<>
  [config el & args]
  (h/-parse-element
   fragment
   html-hiccup-config
   args))

(comment
  (f [:div [:span "asdf"]])
  (f [:div {:style {:color "red"}} "asdf" "jkl"])
  (f [fragment [:div "asdf"] [:div "jkl"]])
  (f [:<> [:div "asdf"] [:div "jkl"]])
  )
