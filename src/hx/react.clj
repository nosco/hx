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

(defmacro defnc [displayName props-bindings & body]
  (let [opts-map? (map? (first body))
        ret (gensym "return_value")]
    ;; maybe-ref for react/forwardRef support
    `(do
       (defn ~displayName [props# maybe-ref#]
         (let [~props-bindings [(hx.react/props->clj props#) maybe-ref#]]
           ;; pre-conditions
           ~@(when (and opts-map? (:pre (first body)))
               (map (fn [x] `(assert ~x)) (:pre (first body))))
           (hx.react/parse-body
            ;; post-conditions
            ~(if (and opts-map? (:post (first body)))
               ;; save hiccup value of body
               `(let [~ret (do ~@(rest body))]
                  ;; apply post-conditions
                  ~@(map (fn [x] `(assert ~(replace {'% ret} x)))
                         (:post (first body)))
                  ~ret)
               ;; if no post-conditions, do nothing
               `(do ~@body)))))
       (when js/goog.DEBUG
         (set! (.-displayName ~displayName) ~(str *ns* "/" displayName))))))

(defmacro shallow-render [& body]
  `(with-redefs [hx.react/parse-body identity]
     ~@body))
