(ns hooks.hx
  "clj-kondo hooks for hx.react/defnc macro.

   defnc is similar to defn but with some differences:
   - Single-arity only (component functions)
   - Supports optional ref as second arg: [props ref]
   - Body may have :pre/:post conditions
   - First arg should be a map destructuring {:keys [...]}"
  (:require [clj-kondo.hooks-api :as api]))

(defn- valid-ref-name? [sym]
  (= 'ref sym))

(defn- valid-props-name? [sym]
  (or (= 'props sym)
      (= '_ sym)
      (= '_props sym)))

(defn defnc
  "Rewrite defnc to defn for linting purposes.

   (defnc MyComponent [{:keys [foo bar]}]
     {:pre [...]}
     body...)

   becomes:

   (defn MyComponent [{:keys [foo bar]}]
     {:pre [...]}
     body...)

   Also validates:
   - First arg should be a map destructuring OR named `props`
   - Second arg (if present) should be named `ref`"
  [{:keys [node]}]
  (let [children (:children node)
        ;; [defnc name args & body]
        [_defnc name-node & rest-nodes] children
        ;; Check for docstring
        has-docstring? (and (> (count rest-nodes) 1)
                            (api/string-node? (first rest-nodes)))
        docstring (when has-docstring? (first rest-nodes))
        args-and-body (if has-docstring? (rest rest-nodes) rest-nodes)
        args-node (first args-and-body)
        body (rest args-and-body)

        ;; Validate args
        args-children (when (api/vector-node? args-node)
                        (:children args-node))
        first-arg (first args-children)
        second-arg (second args-children)

        ;; First arg is valid if it's a map OR a symbol named `props`
        first-arg-valid? (or (api/map-node? first-arg)
                             (and (api/token-node? first-arg)
                                  (valid-props-name? (api/sexpr first-arg))))]

    ;; Check first arg is map destructuring or `props`/`_`/`_props`
    (when (and first-arg (not first-arg-valid?))
      (api/reg-finding! (assoc (meta first-arg)
                               :message "defnc first argument should be a map destructuring like {:keys [...]} or named `props`, `_`, or `_props`"
                               :type :hx/defnc-first-arg)))

    ;; Check second arg is named `ref` if present
    (when (and second-arg
               (api/token-node? second-arg)
               (not (valid-ref-name? (api/sexpr second-arg))))
      (api/reg-finding! (assoc (meta second-arg)
                               :message (str "defnc second argument should be named `ref`, got `"
                                             (api/sexpr second-arg) "`")
                               :type :hx/defnc-second-arg)))

    ;; Build the new defn node
    (let [new-node (api/list-node
                    (concat
                     [(api/token-node 'defn)
                      name-node]
                     (when docstring [docstring])
                     [args-node]
                     body))]
      {:node (with-meta new-node (meta node))})))
