(ns hx.datafy
  (:require [hx.react :as hx]
            [clojure.datafy :as d]))

(extend-protocol IPrintWithWriter
  js/Symbol
  (-pr-writer [sym writer _]
    (-write writer (str "\"" (.toString sym) "\""))))

(d/datafy [1 2 3])

#_(hx/f [:div [:div "hi"]])
