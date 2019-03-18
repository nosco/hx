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
    ((second react-ref) f))
  (-swap! [o f a]
    ((second react-ref) #(f % a)))
  (-swap! [o f a b]
    ((second react-ref) #(f % a b)))
  (-swap! [o f a b xs]
    ((second react-ref) #(apply f % a b xs))))

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

(deftype AtomRef [react-ref]
  IDeref
  (-deref [_]
    (deref (.-current react-ref)))

  IReset
  (-reset! [_ v']
    (reset! (.-current react-ref) v'))

  ISwap
  (-swap! [o f]
    (swap! (.-current react-ref) f))
  (-swap! [o f a]
    (swap! (.-current react-ref) #(f % a)))
  (-swap! [o f a b]
    (swap! (.-current react-ref) #(f % a b)))
  (-swap! [o f a b xs]
    (swap! (.-current react-ref) #(apply f % a b xs))))


(defn <-atom [initial-value]
  (let [r (react/useRef (atom initial-value))
        k (gensym "<-atom")
        [v u] (react/useState initial-value)]
    (react/useEffect
     (fn []
       (add-watch (.-current r) k (fn [_ _ _ v']
                                    (u v')))
       (fn [] (remove-watch (.-current r) k)))
     #js [])
    (AtomRef. r)))

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

(def <-reducer
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
(defn- <-clj-deps [deps]
  (let [-deps (react/useRef deps)]
    (when (not= deps (.-current -deps))
      (set! (.-current -deps) deps))
    (.-current -deps)))

(defn <-effect
  "Just react/useEffect"
  ([f]
   (react/useEffect f))
  ([f deps]
   (react/useEffect f #js [(<-clj-deps deps)])))

(def <-context
  "Just react/useContext"
  react/useContext)

(def <-memo
  "Just react/useMemo"
  react/useMemo)

(defn <-callback
  "Just react/useCallback"
  ([f] (react/useCallback f))
  ([f deps] (react/useCallback f #js [(<-clj-deps deps)])))

(defn <-imperative-handle
  "Just react/useImperativeHandle"
  ([ref create-handle]
   (react/useImperativeHandle ref create-handle))
  ([ref create-handle deps]
   (react/useImperativeHandle ref create-handle
                              #js [(<-clj-deps deps)])))

(defn <-layout-effect
  "Just react/useLayoutEffect"
  ([f] (react/useLayoutEffect f))
  ([f deps] (react/useLayoutEffect f #js [<-clj-deps deps])))

(def <-debug-value
  "Just react/useDebugValue"
  react/useDebugValue)
