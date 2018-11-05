(ns hx.react
  (:require #?(:cljs [goog.object :as gobj])
            #?(:cljs ["react" :as react])
            [hx.hiccup :as hiccup]
            [hx.utils :as utils])
  #?(:cljs (:require-macros [hx.react])))

#?(:cljs (def f hiccup/parse))

#?(:cljs (defn parse-body [body]
           (if (vector? body)
             (f body)
             body)))

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
                     (into {"displayName" (name display-name)}))
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

(defmacro defnc [name props-bindings & body]
  (let [opts-map? (map? (first body))]
    `(defn ~name [props# maybe-ref#]
       ~(when opts-map? (first body))
       (let [~props-bindings (hx.react/props->clj props# maybe-ref#)]
         (hx.react/parse-body
          (do ~@(if opts-map? (rest body) body)))))))

(defmacro shallow-render [& body]
  `(with-redefs [hx.react/parse-body identity]
     ~@body))

#?(:cljs (def fragment react/Fragment))

#?(:cljs (defmethod hiccup/parse-element
           :<>
           [el & args]
           (hiccup/-parse-element
            hx.react/fragment
            args)))


#?(:cljs (defn props->clj [props maybe-ref]
           (let [props (utils/shallow-js->clj props :keywordize-keys true)]
             [props maybe-ref])))

#?(:clj (defn $ [el & args]
          nil)
   :cljs (defn $ [el & args] (hiccup/make-element el args))
   ;; (defn $ [el p & c]
         ;;   (if (or (string? p) (number? p) (react/isValidElement p))
         ;;     (apply react/createElement el nil p c)

         ;;     ;; if el is a keyword, or is not marked as an hx component,
         ;;     ;; we recursively convert styles
         ;;     (let [js-interop? (string? el)
         ;;           props (utils/clj->props p)]
         ;;       (apply react/createElement el props c))))
   )

#?(:cljs (defn assign-methods [class method-map]
           (doseq [[method-name method-fn] method-map]
             (gobj/set (.-prototype class)
                       (munge (name method-name))
                       method-fn))
           class))

#?(:cljs (defn create-class [super-class init-fn static-properties method-names]
           (let [ctor (fn [props]
                        (this-as this
                          ;; auto-bind methods
                          (doseq [method method-names]
                            (gobj/set this (munge method)
                                      (.bind (gobj/get this (munge method)) this)))

                          (init-fn this props)))]
             ;; set static properties on prototype
             (goog/inherits ctor super-class)
             (doseq [[k v] static-properties]
               (gobj/set ctor k v))
             ctor)))

#?(:cljs (defn create-component [init-fn static-properties method-names]
           (create-class react/Component init-fn static-properties method-names)))

#?(:cljs (defn create-pure-component [init-fn static-properties method-names]
           (create-class react/PureComponent init-fn static-properties method-names)))

(defn factory
  "Takes a React component, and creates a function that returns
  a new React element"
  [component]
  (partial $ component))


