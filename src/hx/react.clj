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

(defn- fnc*
  "Generate an hx component function that works with both UIx-style and React-style props.

   Old hx pattern: (defnc Comp [{:keys [x]} ref] body)
   - props-bindings is [{:keys [x]} ref]
   - ref comes as second element (for use with React.forwardRef)

   New pattern: (defnc Comp [{:keys [x ref]}] body)
   - ref is just a regular prop key

   The generated function:
   - Can be called UIx-style (props under .-argv as CLJS map)
   - Can be called React-style (props as JS object, for memo/forwardRef wrappers)
   - For old [props ref] pattern: generates 2-arg function for forwardRef compatibility"
  [display-name props-bindings opts-map body]
  (let [ret (gensym "return_value")
        ;; Check if zero-arg pattern: []
        zero-args? (and (vector? props-bindings)
                        (empty? props-bindings))
        ;; Check if old two-arg pattern: [{:keys [...]} ref]
        has-ref-arg? (and (not zero-args?)
                          (vector? props-bindings)
                          (= 2 (count props-bindings))
                          (symbol? (second props-bindings)))
        ;; Extract the actual props destructuring
        props-destructure (cond
                            zero-args? '_
                            has-ref-arg? (first props-bindings)
                            (vector? props-bindings) (first props-bindings)
                            :else props-bindings)
        ;; Get the ref symbol if old pattern
        ref-sym (when has-ref-arg? (second props-bindings))
        ;; Generate the component body with pre/post conditions
        component-body `(hx.react/parse-body
                         ~(if (:post opts-map)
                            `(let [~ret (do ~@body)]
                               ~@(map (fn [x] `(assert ~(replace {'% ret} x)))
                                      (:post opts-map))
                               ~ret)
                            `(do ~@body)))
        ;; Generate unique symbols for the raw props/ref args
        raw-props-sym (gensym "react-props")
        raw-ref-sym (gensym "react-ref")]
    (if has-ref-arg?
      ;; Old pattern with ref arg: generate 2-arg function for forwardRef compatibility
      ;; The function takes (props, ref) like forwardRef expects
      `(let [f# (fn ~display-name [~raw-props-sym ~raw-ref-sym]
                  (let [~props-destructure (hx.react/extract-cljs-props-with-ref ~raw-props-sym ~raw-ref-sym)
                        ~ref-sym (or ~raw-ref-sym (:ref ~props-destructure))]
                    ~@(when (:pre opts-map)
                        (map (fn [x] `(assert ~x)) (:pre opts-map)))
                    ~component-body))]
         (set! (.-uix-component? f#) true)
         f#)
      ;; New pattern: single-arg function (compatible with both UIx and React props)
      `(let [f# (fn ~display-name [~raw-props-sym]
                  (let [~props-destructure (hx.react/extract-cljs-props ~raw-props-sym)]
                    ~@(when (:pre opts-map)
                        (map (fn [x] `(assert ~x)) (:pre opts-map)))
                    ~component-body))]
         (set! (.-uix-component? f#) true)
         f#))))

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
