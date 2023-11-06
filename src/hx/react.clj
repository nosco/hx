(ns hx.react)

(defmacro defcomponent
  {:style/indent [1 :form [1]]}
  [display-name constructor & body]
  (let [;; with-compile (compile* body)
        methods (filter #(not (or (= (first %) 'render)
                                  (:static (meta %)))) body)
        render (first (filter #(= (first %) 'render) body))
        render' `(~(first render) ~(second render)
                  (hx.react/parse-body
                   (do ~@(nthrest render 2))))
        statics (->> (filter #(:static (meta %)) body)
                     (map #(apply vector (str (munge (first %))) (rest %)))
                     (into {"displayName" (str *ns* "/" display-name)}))
        method-names (into [] (map #(list 'quote (first %)) methods))]
    `(def ~display-name
       (let [ctor# (fn ~(second constructor)
                     ;; constructor must return `this`
                     ~@(drop 2 constructor))
             class# (hx.react/create-pure-component
                     ctor#
                     ~statics
                     ~method-names)]
         (cljs.core/specify! (.-prototype class#)
           ~'Object
           ~render'
           ~@methods)
         class#))))

(defn- fnc* [display-name props-bindings opts-map body]
  (let [ret (gensym "return_value")]
    ;; maybe-ref for react/forwardRef support
    `(fn ~display-name [props# maybe-ref#]
       (let [~props-bindings [(hx.react/props->clj props#) maybe-ref#]]
         ;; pre-conditions
         ~@(when (:pre opts-map)
             (map (fn [x] `(assert ~x)) (:pre opts-map)))
         (hx.react/parse-body
          ;; post-conditions
          ~(if (:post opts-map)
             ;; save hiccup value of body
             `(let [~ret (do ~@body)]
                ;; apply post-conditions
                ~@(map (fn [x] `(assert ~(replace {'% ret} x)))
                       (:post opts-map))
                ~ret)
             ;; if no post-conditions, do nothing
             `(do ~@body)))))))

(defmacro fnc [display-name props-bindings & body]
  (let [opts-map (when (map? (first body)) (first body))
        body (when (map? (first body)) (next body))]
    (fnc* display-name props-bindings opts-map body)))

(alter-meta! #'fnc assoc
             :arglists '([display-name props-bindings opts-map? & body]))

(defmacro defnc
  {:style/indent :defn}
  [display-name & fdecl]
  (let [m (if (string? (first fdecl)) {:doc (first fdecl)} {})
        fdecl (if (string? (first fdecl)) (next fdecl) fdecl)
        props-bindings (first fdecl)
        fdecl (next fdecl)
        opts-map (when (map? (first fdecl)) (first fdecl))
        body (if (map? (first fdecl)) (next fdecl) fdecl)
        m (assoc m :arglists (list props-bindings))
        m (conj m (meta display-name))
        f (fnc* display-name props-bindings opts-map body)]
    (if (:wrap opts-map)
      (let [wrapped-name (symbol (str display-name "-hx-wrapped"))]
        `(do
           (def ~wrapped-name ~f)
           (when goog/DEBUG
             (hx.react/obj-set ~wrapped-name "displayName" ~(str *ns* "/" display-name)))
           ~(list 'def (with-meta display-name m) `(-> ~wrapped-name ~@(:wrap opts-map)))
           ~display-name))
      `(do
         ~(list 'def (with-meta display-name m) f)
         (when goog/DEBUG
           (hx.react/obj-set ~display-name "displayName" ~(str *ns* "/" display-name)))
         ~display-name))))

(alter-meta! #'defnc assoc
             :arglists '([display-name doc-string? props-bindings opts-map? & body]))

(defmacro shallow-render [& body]
  `(with-redefs [hx.react/parse-body identity]
     ~@body))
