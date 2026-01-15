(ns hooks.hx
  "clj-kondo hooks for hx.react/defnc macro.

   defnc is similar to defn but with some differences:
   - Single-arity only (component functions)
   - Supports optional ref as second arg: [props ref]
   - Body may have :pre/:post conditions
   - First arg should be a map destructuring {:keys [...]}
   - :wrap option must be a vector"
  (:require [clj-kondo.hooks-api :as api]))

(defn- valid-ref-name? [sym]
  (= 'ref sym))

(defn- valid-props-name? [sym]
  (= 'props sym))

(defn- find-wrap-value
  "Find the value associated with :wrap key in a map node.
   Returns nil if not found."
  [map-node]
  (when (api/map-node? map-node)
    (let [children (:children map-node)]
      (loop [pairs (partition 2 children)]
        (when (seq pairs)
          (let [[k v] (first pairs)]
            (if (and (api/keyword-node? k)
                     (= :wrap (api/sexpr k)))
              v
              (recur (rest pairs)))))))))

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
                                  (valid-props-name? (api/sexpr first-arg))))

        ;; Check for opts-map (first element of body that's a map)
        opts-map (first (filter api/map-node? body))
        wrap-value (find-wrap-value opts-map)]

    ;; Check first arg is map destructuring or `props`
    (when (and first-arg (not first-arg-valid?))
      (api/reg-finding! (assoc (meta first-arg)
                               :message "defnc first argument should be a map destructuring like {:keys [...]} or named `props`"
                               :type :hx/defnc-first-arg)))

    ;; Check second arg is named `ref` if present
    (when (and second-arg
               (api/token-node? second-arg)
               (not (valid-ref-name? (api/sexpr second-arg))))
      (api/reg-finding! (assoc (meta second-arg)
                               :message (str "defnc second argument should be named `ref`, got `"
                                             (api/sexpr second-arg) "`")
                               :type :hx/defnc-second-arg)))

    ;; Check :wrap option is a vector, not a bare form
    (when (and wrap-value (not (api/vector-node? wrap-value)))
      (api/reg-finding! (assoc (meta wrap-value)
                               :message "defnc :wrap option must be a vector, e.g. {:wrap [(react/memo)]}"
                               :type :hx/defnc-wrap-not-vector)))

    ;; Build the new defn node
    (let [new-node (api/list-node
                    (concat
                     [(api/token-node 'defn)
                      name-node]
                     (when docstring [docstring])
                     [args-node]
                     body))]
      {:node (with-meta new-node (meta node))})))
