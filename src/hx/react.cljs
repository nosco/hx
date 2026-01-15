(ns hx.react
  (:require [goog.object :as gobj]
            [goog.functions :as gfunc]
            ["react" :as react]
            [cljs-bean.core :as bean]
            [hx.hiccup :as hiccup]
            [hx.utils :as utils :include-macros true]
            [uix.core :as uix])
  (:require-macros [hx.react]))

(defn obj-set [o k v]
  (gobj/set o k v))

(def props->clj utils/props->clj)

(defn extract-cljs-props
  "Extract CLJS props map from React props object.
   Handles both UIx-style (props under .-argv) and standard React props.
   This allows hx components to work with React.memo, React.forwardRef, etc."
  [^js react-props]
  (if-some [argv (.-argv react-props)]
    ;; UIx-style: props stored under .argv as CLJS map
    ;; Also merge React's children if present (UIx passes children separately)
    (cond-> argv
      (.-children react-props) (assoc :children (.-children react-props)))
    ;; Standard React props: convert JS object to CLJS map
    (bean/bean react-props)))

(defn extract-cljs-props-with-ref
  "Like extract-cljs-props but also handles the ref argument from forwardRef.
   For old [props ref] pattern compatibility."
  [^js react-props ^js ref-arg]
  (let [props (extract-cljs-props react-props)]
    (if (some? ref-arg)
      ;; forwardRef passed ref as second arg - merge it into props
      (assoc props :ref ref-arg)
      props)))

(defn- props [el first-arg props?]
  (cond
    (and (string? el) props?) (utils/clj->props first-arg)

    props? (utils/clj->props first-arg false)

    true nil))

(defn- fn-as-child [config first-child args]
  (fn [& args]
    (let [ret (apply first-child args)]
      (if (vector? ret)
        (hiccup/-as-element ret config)
        ret))))

(defn- uix-component? [el]
  (and (fn? el) (true? (.-uix-component? ^js el))))

(defn- create-uix-element
  "Create element for UIx component. UIx expects props under .-argv with :children merged."
  [el props-map children config]
  (let [first-child (first children)
        ;; Handle function-as-child pattern: if single child is a function (not a component),
        ;; wrap it so the render function's return value gets parsed as hiccup
        parsed-children (if (and (= 1 (count children))
                                 (gfunc/isFunction first-child)
                                 (not (uix-component? first-child)))
                          ;; Function-as-child: wrap to parse hiccup from render fn result
                          [(fn [& args]
                             (hiccup/-as-element (apply first-child args) config))]
                          ;; Normal children: parse through hiccup
                          (mapv #(hiccup/-as-element % config) children))
        ;; UIx expects :children in the props map (argv)
        argv (if (seq parsed-children)
               (assoc props-map :children (if (= 1 (count parsed-children))
                                            (first parsed-children)
                                            (into-array parsed-children)))
               props-map)]
    (react/createElement el #js {:argv argv})))

(defn create-element [config el args]
  (utils/measure-perf
   "create_element"
   (let [first-arg (utils/measure-perf
                    "first-arg"
                    (nth args 0 nil))
         props? (map? first-arg)
         children (utils/measure-perf
                   "children"
                   (if props? (-rest args) args))
         first-child (utils/measure-perf
                      "first-child"
                      (nth children 0 nil))]
     ;; Check if this is a UIx component
     (if (uix-component? el)
       ;; UIx component: pass props as argv with children merged
       (create-uix-element el (if props? first-arg {}) children config)
       ;; Regular element or hx component: use existing logic
       (let [props (utils/measure-perf
                    "props"
                    (props el first-arg props?))]
         (case (count children)
           0 (utils/measure-perf
              "no_children"
              (react/createElement el props))
           1 (if (utils/measure-perf
                  "fn?"
                  ^boolean (gfunc/isFunction first-child))
               (react/createElement el
                                    props
                                    ;; fn-as-child
                                    ;; wrap in a function to parse hiccup from render-fn
                                    (fn-as-child config first-child args))
               (utils/measure-perf
                "one_child"
                (react/createElement el props (utils/measure-perf
                                               "one_child_parse"
                                               (hiccup/-as-element
                                                first-child config)))))
           ;; use .apply here for performance
           (utils/measure-perf
            "has_children"
            (.apply
             react/createElement nil
             (utils/measure-perf
              "children_parse_loop"
              (loop [a #js [el props]
                     c children]
                (if-not (nil? c)
                  (do
                    (.push a (hiccup/-as-element
                              (-first c) config))
                    (recur
                     a
                     (-next c)))
                  a)))))))))))

(def react-hiccup-config
  {:create-element create-element
   :is-element? react/isValidElement
   :fragment react/Fragment})

(defn f [form]
  (hiccup/parse react-hiccup-config form))

(defn parse-body [body]
  (if (vector? body)
    (f body)
    body))

(def fragment react/Fragment)

(hiccup/extend-tag :<> hx.react/fragment)

(hx.react/defnc Provider [{:keys [context value children]}]
  [(.-Provider ^js context)
   {:value value}
   children])

(hiccup/extend-tag :provider Provider)



(defn $ [el & args] (hiccup/make-element react-hiccup-config el args))

(defn assign-methods [class method-map]
  (doseq [[method-name method-fn] method-map]
    (gobj/set (.-prototype class)
              (munge (name method-name))
              method-fn))
  class)

(defn create-class [super-class init-fn static-properties method-names]
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
    ctor))

(defn create-component [init-fn static-properties method-names]
  (create-class react/Component init-fn static-properties method-names))

(defn create-pure-component [init-fn static-properties method-names]
  (create-class react/PureComponent init-fn static-properties method-names))

(def create-context
  "Just react/createContext"
  react/createContext)

(defn factory
  "Takes a React component, and creates a function that returns
  a new React element"
  [component]
  (partial $ component))
