(ns hx.react
  (:require [uix.core]))

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

(defn- wrap-body-with-conditions
  "Wrap body with :pre/:post conditions if present in opts-map."
  [body opts-map]
  (let [ret (gensym "return_value")]
    (cond
      ;; Both pre and post
      (and (:pre opts-map) (:post opts-map))
      `(do
         ~@(map (fn [x] `(assert ~x)) (:pre opts-map))
         (let [~ret (do ~@body)]
           ~@(map (fn [x] `(assert ~(replace {'% ret} x)))
                  (:post opts-map))
           ~ret))

      ;; Just pre
      (:pre opts-map)
      `(do
         ~@(map (fn [x] `(assert ~x)) (:pre opts-map))
         ~@body)

      ;; Just post
      (:post opts-map)
      `(let [~ret (do ~@body)]
         ~@(map (fn [x] `(assert ~(replace {'% ret} x)))
                (:post opts-map))
         ~ret)

      ;; No conditions
      :else
      `(do ~@body))))

(defn- process-opts-map
  "Process the opts-map from a component body.

   Given a body (which may start with an opts-map), returns a map containing:
   - :wrap - vector of wrap expressions (or nil if none)
   - :body - the body wrapped with :pre/:post conditions

   Validates that :wrap is a vector if present.

   Example opts-map:
   {:pre [(some? x)]
    :post [(string? %)]
    :wrap [(react/memo) (react/forwardRef)]}"
  [body]
  (let [opts-map (when (map? (first body)) (first body))
        body-without-opts (if opts-map (next body) body)]
    (when-let [wrap (:wrap opts-map)]
      (when-not (vector? wrap)
        (throw (ex-info ":wrap must be a vector of wrapper functions"
                        {:wrap wrap}))))
    {:wrap (:wrap opts-map)
     :body (wrap-body-with-conditions body-without-opts
                                      (dissoc opts-map :wrap))}))

(defmacro fnc
  "Create an anonymous hx component (like uix.core/fn but with hiccup support).

   Delegates to uix.core/fn for props handling, adds hiccup parsing."
  [display-name props-bindings & body]
  (let [{:keys [wrap body]} (process-opts-map body)]
    ;; TODO: handle wrap expressions in future iteration
    (when wrap
      (println "Warning: :wrap is not yet supported in fnc, ignoring:" wrap))
    ;; Delegate to uix.core/fn, wrapping body with parse-body for hiccup
    `(uix.core/fn ~display-name ~props-bindings
       (hx.react/parse-body ~body))))

(alter-meta! #'fnc assoc
             :arglists '([display-name props-bindings opts-map? & body]))

(defn- move-ref-to-props
  "Handle old [props ref] two-arg pattern by merging ref into props destructuring.
   Returns [new-props-binding ref-sym-or-nil].

   Examples:
   - [{:keys [x]} ref] -> [{:keys [x ref]}] with ref bound
   - [props ref] -> [props] but need to let-bind ref from props"
  [props-bindings]
  (if (and (vector? props-bindings)
           (= 2 (count props-bindings))
           (symbol? (second props-bindings)))
    ;; Old [props ref] pattern detected
    (let [[props-form ref-sym] props-bindings]
      (cond
        ;; Case 1: {:keys [x y]} - add ref to :keys
        (and (map? props-form) (contains? props-form :keys))
        (let [new-keys (conj (vec (:keys props-form)) ref-sym)
              new-props (assoc props-form :keys new-keys)]
          {:props-binding [new-props]
           :ref-sym nil  ; ref is now destructured directly
           })

        ;; Case 2: {x :x} style map destructuring - add ref :ref
        (map? props-form)
        (let [new-props (assoc props-form ref-sym :ref)]
          {:props-binding [new-props]
           :ref-sym nil})

        ;; Case 3: plain symbol - need to extract ref in body
        (symbol? props-form)
        {:props-binding [props-form]
         :ref-sym ref-sym}

        :else
        {:props-binding [props-form]
         :ref-sym nil}))

    ;; Not the old pattern - return as-is
    {:props-binding props-bindings
     :ref-sym nil}))

(defmacro defnc
  "Define an hx component (like uix.core/defui but with hiccup support).

   Delegates to uix.core/defui for all component functionality, adding
   hiccup parsing for the body and support for :pre/:post conditions.

   Supports:
   - ^:memo metadata for React.memo wrapping
   - :pre/:post conditions
   - :& rest props syntax (via UIx)
   - Old [props ref] two-arg pattern (ref merged into props as :ref)

   Examples:
   ;; Simple component
   (defnc Greeting [{:keys [name]}]
     [:div \"Hello, \" name])

   ;; With memo
   (defnc ^:memo MemoizedGreeting [{:keys [name]}]
     [:div \"Hello, \" name])

   ;; With rest props (UIx syntax)
   (defnc Button [{:keys [label] :& rest-props}]
     [:button rest-props label])

   ;; Old forwardRef pattern (ref available as :ref in props)
   (defnc FancyInput [{:keys [placeholder ref]}]
     [:input {:ref ref :placeholder placeholder}])"
  {:style/indent :defn}
  [display-name & fdecl]
  (let [;; Parse docstring if present
        m (if (string? (first fdecl)) {:doc (first fdecl)} {})
        fdecl (if (string? (first fdecl)) (next fdecl) fdecl)
        raw-props-bindings (first fdecl)
        {:keys [props-binding ref-sym]} (move-ref-to-props raw-props-bindings)
        fdecl (next fdecl)

        ;; Process opts-map (extracts :wrap, wraps body with :pre/:post)
        {:keys [wrap body]} (process-opts-map fdecl)

        ;; TODO: handle wrap expressions in future iteration
        _ (when wrap
            (println "Warning: :wrap is not yet supported in defnc, ignoring:" wrap))

        ;; If ref-sym is set, we need to let-bind it from props
        ;; (this happens when props was a plain symbol like `props`)
        body (if ref-sym
               `(let [~ref-sym (:ref ~props-binding)]
                  ~body)
               body)

        ;; Preserve metadata from display-name (including ^:memo)
        m (merge m (meta display-name))]

    ;; Simple delegation to defui with hiccup parsing
    `(do
       (uix.core/defui ~(with-meta display-name m) ~props-binding
         (hx.react/parse-body ~body)))))

(alter-meta! #'defnc assoc
             :arglists '([display-name doc-string? props-bindings opts-map? & body]))

(defmacro shallow-render [& body]
  `(with-redefs [hx.react/parse-body identity]
     ~@body))
