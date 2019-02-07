(ns hx.react.hooks
  (:require ["react" :as react]
            [goog.object :as gobj]))

(deftype Atomified [react-ref]
  IDeref
  (-deref [_]
    (first react-ref))

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
  (Atomified. (react/useState initial)))

(defn <-ref
  "Takes an initial value. Returns an atom that will _NOT_ re-render component
  on change."
  [initial]
  (let [react-ref (react/useRef)
        update-ref (fn [v] (gobj/set react-ref "current" v))]
  (Atomified. [react-ref update-ref])))

(defn <-deref
  "Takes an atom. Returns the currently derefed value of the atom, and re-renders
  the component on change."
  ;; if no deps are passed in, we assume we only want to run
  ;; subscrib/unsubscribe on mount/unmount
  ([a] (<-deref a []))
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

(defn <-deref-in
  "Takes an atom and a sequence of keys in a nested associative structure atom
  references. Returns the currently derefed value on the key path,
  and re-renders the component if the value changes."
  ;; if no deps are passed in, we assume we only want to run
  ;; subscrib/unsubscribe on mount/unmount
  ([a k] (<-deref-in a k []))
  ([a k deps]
   ;; create a react/useState hook to track and trigger renders
   (let [[v u] (react/useState (get-in @a k))]
     ;; react/useEffect hook to create and track the subscription to the iref
     (react/useEffect
      (fn []
        (add-watch a :use-in-atom
                   ;; update the react state on each change
                   (fn [_ _ v v']
                     (if-not (= (get-in v k) (get-in v' k)) (u v'))))
        ;; return a function to tell react hook how to unsubscribe
        #(remove-watch a :use-in-atom))
      ;; pass in deps vector as an array
      (clj->js deps))
     ;; return value of useState on each run
     v)))

(def <-reducer
  "Just react/useReducer."
  react/useReducer)

(def <-effect
  "Just react/useEffect"
  react/useEffect)

(def <-context
  "Just react/useContext"
  react/useContext)
