(ns hx.html
  (:require [hx.hiccup :as h]))

(defn props->attrs [props]
  ;; TODO
  "")

(defn create-element [config el args]
  (let [first-arg (nth args 0 nil)
        props? (map? first-arg)
        children (if props? (rest args) args)
        ;; first-child (nth children 0 nil)
        ]
  (cond
    (= el "<>") (apply str (map #(h/-as-element % config) children))
    (string? el) (str "<" el (when props? (props->attrs first-arg)) ">"
                      (->> children
                           (map #(h/-as-element % config))
                           (apply str))
                      "</" el ">")
    (ifn? el) (el (assoc (if props? first-arg {})
                         :children children)))))

(defn fragment [{:keys [children]}]
  )

(def html-hiccup-config
  {:create-element create-element
   :is-element? string?
   :fragment fragment})

(defn f [form]
  (h/parse html-hiccup-config
           form))

(comment
  (f [:div [:span "asdf"]])
  (f [:div {:style {:color "red"}} "asdf" "jkl"])
  (f [:<> [:div "asdf"] [:div "jkl"]])
  (f [:aside [:<> [:div "asdf"] [:div "jkl"]]])
  )
