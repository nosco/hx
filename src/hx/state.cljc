(ns hx.state
  (:require [hx.react]))

(defmacro defrc
  [name args body]
  `(hx.react/defcomponent ~name
     (constructor [this]
       (set! (.-reactions this) {})
       (set! (aget this "state") (js/Object.))
       this)
     (render ~args
       ~@body)))
