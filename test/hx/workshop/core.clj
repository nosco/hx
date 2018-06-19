(ns hx.workshop.core
  (:require [hx.react.interceptors]
            [hx.react]))

(defmacro register! []
  (hx.react/register-interceptor!
   hx.react.interceptors/$-as-compile)
  nil)
