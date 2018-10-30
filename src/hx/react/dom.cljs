(ns hx.react.dom
  (:refer-clojure :exclude [map meta time])
  (:require [hx.react :as r]
            [react :as react])
  (:require-macros [hx.react.dom]))

(hx.react.dom/make-factories)

(def <> (r/factory react/Fragment))
