(ns hx.react.alpha)

(defmacro $
  [type & args]
  (if (map? (first args))
    `(create-element
      ~(if (keyword? type)
         (name type)
         type)
      (clj->props ~(first args) ~(keyword? type))
      ~@(rest args))

    ;; bail to runtime detection of props
    `($$ ~(if (keyword? type)
            (name type)
            type)
         ~@args)))


(defmacro <>
  [& children]
  `($ Fragment ~@children))


(defn- fnc*
  [display-name props-bindings body]
  (let [ret (gensym "return_value")]
    ;; maybe-ref for react/forwardRef support
    `(fn ~display-name
       [props# maybe-ref#]
       (let [~props-bindings [(extract-cljs-props props#) maybe-ref#]]
         (do ~@body)))))


(defmacro defnc
  [display-name & form-body]
  (let [docstring (when (string? (first form-body))
                    (first form-body))
        props-bindings (if (nil? docstring)
                         (first form-body)
                         (second form-body))
        body (if (nil? docstring)
               (rest form-body)
               (rest (rest form-body)))
        wrapped-name (symbol (str display-name "-hx-render"))
        opts-map? (map? (first body))
        opts (if opts-map?
               (first body)
               {})]
    `(do (def ~wrapped-name ~(fnc* wrapped-name props-bindings
                                   (if opts-map?
                                     (rest body)
                                     body)))
         (when goog/DEBUG
           (goog.object/set ~wrapped-name "displayName" ~(str *ns* "/" display-name)))
         (def ~display-name
           ~@(when-not (nil? docstring)
               (list docstring))
           (-> ~wrapped-name
               (wrap-cljs-component)
               ~@(-> opts :wrap)
               (factory))))))

