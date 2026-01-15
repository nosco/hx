(ns hooks.hx
  "clj-kondo hooks for hx.react/defnc macro.

   defnc is similar to defn but with some differences:
   - Single-arity only (component functions)
   - Supports optional ref as second arg: [props ref]
   - Body may have :pre/:post conditions
   - First arg should be a map destructuring {:keys [...]}
   - :wrap option must be a vector
   - Supports :& rest props syntax (like UIx)"
  (:require [clj-kondo.hooks-api :as api]))

(defn- valid-ref-name? [sym]
  (= 'ref sym))

(defn- valid-props-name? [sym]
  (= 'props sym))

(defn- find-map-key-value
  "Find the value associated with a keyword key in a map node.
   Returns nil if not found."
  [map-node kw]
  (when (api/map-node? map-node)
    (let [children (:children map-node)]
      (loop [pairs (partition 2 children)]
        (when (seq pairs)
          (let [[k v] (first pairs)]
            (if (and (api/keyword-node? k)
                     (= kw (api/sexpr k)))
              v
              (recur (rest pairs)))))))))

(defn- find-wrap-value [map-node]
  (find-map-key-value map-node :wrap))

(defn- has-rest-props?
  "Check if a map destructuring contains :& rest props syntax."
  [map-node]
  (and (api/map-node? map-node)
       (some? (find-map-key-value map-node :&))))

(defn- transform-rest-props
  "Transform a map with :& into standard Clojure destructuring.
   {:keys [a b] :& rest} becomes {:keys [a b] :as __props__}
   and we add a let binding for rest.

   Returns [new-map-node rest-symbol extracted-keys] or nil if no :& present."
  [map-node]
  (when (has-rest-props? map-node)
    (let [children (:children map-node)
          rest-sym (find-map-key-value map-node :&)
          ;; Collect all :keys values to know what to exclude from rest
          keys-node (find-map-key-value map-node :keys)
          extracted-keys (when (api/vector-node? keys-node)
                           (mapv api/sexpr (:children keys-node)))
          ;; Build new map without :& but with :as
          new-children (loop [pairs (partition 2 children)
                              result []]
                         (if (seq pairs)
                           (let [[k v] (first pairs)]
                             (if (and (api/keyword-node? k)
                                      (= :& (api/sexpr k)))
                               ;; Skip :& entry
                               (recur (rest pairs) result)
                               ;; Keep other entries
                               (recur (rest pairs) (conj result k v))))
                           result))
          ;; Add :as __props__ for the rest binding
          props-sym (api/token-node '__props__)
          new-map (api/map-node (concat new-children
                                        [(api/keyword-node :as) props-sym]))]
      [new-map rest-sym extracted-keys])))

(defn- wrap-body-with-rest-binding
  "Wrap body with a let binding that computes the rest props."
  [body rest-sym extracted-keys]
  (let [;; Build: (let [rest-sym (dissoc __props__ :a :b ...)] body...)
        ;; extracted-keys are symbols like 'foo, convert to keywords :foo
        dissoc-call (api/list-node
                     (concat [(api/token-node 'dissoc)
                              (api/token-node '__props__)]
                             (map #(api/keyword-node (keyword %)) extracted-keys)))
        let-binding (api/vector-node [rest-sym dissoc-call])
        let-node (api/list-node
                  (concat [(api/token-node 'let)
                           let-binding]
                          body))]
    [let-node]))

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

    ;; Handle :& rest props transformation
    ;; Transform {:keys [a b] :& rest} to {:keys [a b] :as __props__}
    ;; and wrap body with (let [rest (dissoc __props__ :a :b)] ...)
    (let [rest-transform (when (api/map-node? first-arg)
                           (transform-rest-props first-arg))
          [transformed-first-arg rest-sym extracted-keys] rest-transform
          ;; Build new args vector with transformed first arg
          new-args-node (if rest-transform
                          (api/vector-node
                           (concat [transformed-first-arg]
                                   (rest args-children)))
                          args-node)
          ;; Wrap body if we have rest props
          new-body (if rest-transform
                     (wrap-body-with-rest-binding body rest-sym extracted-keys)
                     body)
          ;; Build the new defn node
          new-node (api/list-node
                    (concat
                     [(api/token-node 'defn)
                      name-node]
                     (when docstring [docstring])
                     [new-args-node]
                     new-body))]
      {:node (with-meta new-node (meta node))})))
