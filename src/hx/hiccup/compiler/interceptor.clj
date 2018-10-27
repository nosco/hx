(ns hx.hiccup.compiler.interceptor)

(defn execute [initial-context & interceptors]
  (loop [context (merge
                  initial-context
                  {::queue (apply list interceptors)
                   ::stack '()})]
    (let [queue (::queue context)
          stack (::stack context)]
      (case [(empty? queue) (empty? stack)]
        ([false true]
         [false false]) (let [enter (or (:enter (peek queue)) identity)
                              queue' (pop queue)
                              stack' (conj stack (peek queue))]
                          (recur (->> {::queue queue'
                                       ::stack stack'}
                                      (merge context)
                                      (enter))))

        [true false] (let [leave (or (:leave (peek stack)) identity)
                           stack' (pop stack)]
                       (recur (->> {::stack stack'}
                                   (merge context)
                                   (leave))))
        [true true] context))))

#_(execute
   {:enter #(assoc % :val 10)
    :leave #(assoc % :other-val (:val %))}
   {:enter #(update % :val - 1)
    :leave #(update % :val + 1)}
   {:enter #(update % :val * 2)
    :leave #(update % :val / 2)})
