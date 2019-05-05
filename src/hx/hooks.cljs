(ns hx.hooks
  (:require ["react" :as react]
            [goog.object :as gobj])
  (:require-macros [hx.hooks]))

(deftype Atomified [react-ref deref-lens]
  IDeref
  (-deref [_]
    (deref-lens (first react-ref)))

  IReset
  (-reset! [_ v']
    ((second react-ref) v')
    v')

  ISwap
  (-swap! [o f]
    ((second react-ref) f))
  (-swap! [o f a]
    ((second react-ref) #(f % a)))
  (-swap! [o f a b]
    ((second react-ref) #(f % a b)))
  (-swap! [o f a b xs]
    ((second react-ref) #(apply f % a b xs))))

;; (defn updater
;;   ([x]
;;    )
;;   ([f & xs]
;;    ))

(defn useState
  "Like `React.useState`, but the update function returned can be used similar
  to `swap!`.

  Example:
  ```
  (let [[state set-state] (useState {:count 0})]
   ;; ...
   (set-state update :count inc))
  ```

  If `eq?` is passed in, will use that function to determine whether to update
  the React state. If it returns `true`, it will keep the old state, `false` it
  will render with new state."
  ([initial]
   (let [[v u] (react/useState initial)]
     [v (fn updater
          ([x] (u x))
          ([f & xs]
           (updater (fn spread-updater [x]
                      (apply f x xs)))))]))
  ([initial eq?]
   (let [[v u] (react/useState initial)]
     [v (fn updater
          ([x]
           ;; if x is not a fn, then it's likely not derived from previous state
           ;; so we don't bother checking equality
           (if-not (ifn? x)
             (u x)

             ;; When it is a function, new state will probably be derived from
             ;; previous. We can take advantage of structural sharing to do fast
             ;; equality here and avoid unnecessary re-renders
             (u (fn update [current-state]
                  (let [new-state (x current-state)]
                    (if (eq? current-state new-state)
                      ;; if equal, return the old one to preserve ref equality
                      ;; for React
                      current-state
                      new-state))))))
          ;; Support `(updater f a b c)`
          ([f & xs]
           (updater (fn spread-updater [x]
                      (apply f x xs)))))])))

(defn useIRef
  "Takes an initial value. Returns an atom that will _NOT_ re-render component
  on change."
  [initial]
  (let [react-ref (react/useRef initial)
        update-ref (fn updater
                     ([x]
                      (if-not (ifn? x)
                        (gobj/set react-ref "current" x)
                        (gobj/set react-ref "current"
                                  (x (gobj/get react-ref "current")))))
                     ([f & xs]
                      (updater (fn spread-updater [x]
                                 (apply f x xs)))))]
    (Atomified. [react-ref update-ref] #(.-current ^js %))))


(def useReducer
  "Just react/useReducer."
  react/useReducer)

;; React uses JS equality to check of the current deps are different than
;; previous deps values. This means that Clojure data (e.g. maps, sets, vecs)
;; equality is not respected and will trigger if you e.g. pass in a vec of
;; strings as props and need to depend on that inside of an effect.
;;
;; We can work around this by assigning the previous deps to a ref, and do
;; our own equality check to see if they have changed. If so, we update the
;; ref to equal the current value.
;;
;; We can then just pass this one value into e.g. `useEffect` and it will only
;; change if Clojure's equality detects a difference.
(defn useValue
  "Caches `x`. When a new `x` is passed in, returns new `x` only if it is
  not structurally equal to the previous `x`.

  Useful for optimizing `<-effect` et. al. when you have two values that might
  be structurally equal by referentially different."
  [x]
  (let [-x (react/useRef x)]
    ;; if they are equal, return the prev one to ensure ref equality
    (let [x' (if (= x (.-current -x))
               (.-current -x)
               x)]
      ;; Set the ref to be the last value that was succesfully used to render
      (react/useEffect (fn []
                         (set! (.-current -x) x)
                         js/undefined)
                       #js [x'])
      x')))

;; React `useEffect` expects either a function or undefined to be returned
(defn- wrap-fx [f]
  (fn wrap-fx-return []
    (let [x (f)]
      (if (fn? x)
        x
        js/undefined))))

(defn useEffect
  "Just react/useEffect"
  ([f]
   (react/useEffect (wrap-fx f)))
  ([f deps]
   (react/useEffect (wrap-fx f) (to-array deps))))

(def useContext
  "Just react/useContext"
  react/useContext)

(def useMemo
  "Just react/useMemo"
  react/useMemo)

(defn useCallback
  "Just react/useCallback"
  ([f] (react/useCallback f))
  ([f deps] (react/useCallback f (to-array deps))))

(defn useImperativeHandle
  "Just react/useImperativeHandle"
  ([ref create-handle]
   (react/useImperativeHandle ref create-handle))
  ([ref create-handle deps]
   (react/useImperativeHandle ref create-handle
                              (to-array deps))))

(defn useLayoutEffect
  "Just react/useLayoutEffect"
  ([f] (react/useLayoutEffect f))
  ([f deps] (react/useLayoutEffect f (to-array deps))))


(def useDebugValue
  "Just react/useDebugValue"
  react/useDebugValue)


;;
;; Deprecated
;;

(def ^{:deprecated "Use useState"} <-state useState)

(def ^{:deprecated "Use useIRef"} <-ref useIRef)

(defn ^{:deprecated "Use useState"} <-deref
  "Takes an atom. Returns the currently derefed value of the atom, and re-renders
  the component on change."
  ;; if no deps are passed in, we assume we only want to run
  ;; subscrib/unsubscribe on mount/unmount
  ([a]
   ;; create a react/useState hook to track and trigger renders
   (let [[v u] (react/useState @a)]
     ;; react/useEffect hook to create and track the subscription to the iref
     (react/useEffect
      (fn []
        (let [k (gensym "<-deref")]
          (add-watch a k
                     ;; update the react state on each change
                     (fn [_ _ _ v'] (u v')))
          ;; Check to ensure that a change has not occurred to the atom between
          ;; the component rendering and running this effect.
          ;; If it has updated, then update the state to the current value.
          (when (not= @a v)
            (u @a))
          ;; return a function to tell react hook how to unsubscribe
          #(remove-watch a k)))
      ;; pass in deps vector as an array
      ;; resubscribe if `a` changes
      #js [a])
     ;; return value of useState on each run
     v)))

(def ^{:deprecated "Use useEffect"} <-effect useEffect)

(def ^{:deprecated "Use useReducer"} <-reducer
  "Just react/useReducer."
  react/useReducer)

(def ^{:deprecated "Use useValue"} <-value useValue)

(def ^{:deprecated "Use useContext"} <-context
  "Just react/useContext"
  react/useContext)

(def ^{:deprecated "Use useMemo"} <-memo
  "Just react/useMemo"
  react/useMemo)

(def ^{:deprecated "Use useCallback"} <-callback useCallback)

(def ^{:deprecated "Use useImperativeHandle"} <-imperative-handle useImperativeHandle)

(def ^{:deprecated "Use useDebugValue"} <-debug-value
  "Just react/useDebugValue"
  react/useDebugValue)

(def ^{:deprecated "Use useLayoutEffect"} <-layout-effect useLayoutEffect)
