(ns hx.react.hooks
  (:require ["react" :as react]
            [goog.object :as gobj]))


(deftype State [react-ref]
  IDeref
  (-deref [_]
    (first react-ref))

  IReset
  (-reset! [_ v']
    ((second react-ref) v')
    v')

  ISwap
  (-swap! [o f]
    (-reset! o (f (-deref o)))))

(defn <- [f & args]
  (State. (apply f args)))

(defn <-state
  "Takes an initial value. Returns an atom that will re-render component on
  change."
  [initial]
  (<- react/useState initial))

(defn <-ref
  "Takes an initial value. Returns an atom that will _NOT_ re-render component
  on change."
  [initial]
  (let [react-ref (react/useRef)
        update-ref (fn [v] (gobj/set react-ref "current" v))]
  (State. [react-ref update-ref])))

(defn <-watch
  "Takes an atom. Returns the currently derefed value of the atom, and re-renders
  the component on change."
  ;; if no deps are passed in, we assume we only want to run
  ;; subscrib/unsubscribe on mount/unmount
  ([a] (<-watch a []))
  ([a deps]
   ;; create a react/useState hook to track and trigger renders
   (let [[v u] (react/useState @a)]
     ;; react/useEffect hook to create and track the subscription to the iref
     (react/useEffect
      (fn []
        (add-watch a :use-atom
                   ;; update the react state on each change
                   (fn [_ _ _ v'] (u v')))
        ;; return a function to tell react hook how to unsubscribe
        #(remove-watch a :use-atom))
      ;; pass in deps vector as an array
      (clj->js deps))
     ;; return value of useState on each run
     v)))

(defn <-reducer
  "Just react/useReducer."
  react/useReducer)

(defn <-effect
  "Just react/useEffect"
  react/useEffect)

(defn <-context
  "Just react/useContext"
  react/useContext)
