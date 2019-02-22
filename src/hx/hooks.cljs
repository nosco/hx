(ns hx.hooks
  (:require ["react" :as react]
            [goog.object :as gobj]))

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
    (-reset! o (f (-deref o))))
  (-swap! [o f a]
    (-reset! o (f (-deref o) a)))
  (-swap! [o f a b]
    (-reset! o (f (-deref o) a b)))
  (-swap! [o f a b xs]
    (-reset! o (apply f (-deref o) a b xs))))

(defn <-state
  "Takes an initial value. Returns an atom that will re-render component on
  change."
  [initial]
  (Atomified. (react/useState initial) identity))

(defn <-ref
  "Takes an initial value. Returns an atom that will _NOT_ re-render component
  on change."
  [initial]
  (let [react-ref (react/useRef initial)
        update-ref (fn [v] (gobj/set react-ref "current" v))]
  (Atomified. [react-ref update-ref] #(.-current ^js %))))

(defn <-deref
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
          ;; return a function to tell react hook how to unsubscribe
          #(remove-watch a k)))
      ;; pass in deps vector as an array
      ;; resubscribe if `a` changes
      #js [a])
     ;; return value of useState on each run
     v)))

(def <-reducer
  "Just react/useReducer."
  react/useReducer)

(defn <-effect
  "Just react/useEffect"
  ([f]
   (react/useEffect f))
  ([f deps]
   (react/useEffect f (to-array deps))))

(def <-context
  "Just react/useContext"
  react/useContext)

(def <-memo
  "Just react/useMemo"
  react/useMemo)

(defn <-callback
  "Just react/useCallback"
  ([f] (react/useCallback f))
  ([f deps] (react/useCallback f (to-array deps))))

(defn <-imperative-handle
  "Just react/useImperativeHandle"
  ([ref create-handle]
   (react/useImperativeHandle ref create-handle))
  ([ref create-handle deps]
   (react/useImperativeHandle ref create-handle (to-array deps))))

(defn <-layout-effect
  "Just react/useLayoutEffect"
  ([f] (react/useLayoutEffect f))
  ([f deps] (react/useLayoutEffect f (to-array deps))))

(def <-debug-value
  "Just react/useDebugValue"
  react/useDebugValue)
