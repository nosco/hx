(ns hx.utils)

(def ^:dynamic *perf-debug?* false)

(defmacro measure-perf [tag form]
  (if *perf-debug?*
    (let [begin (str tag "_begin")
          end (str tag "_end")]
      `(do (js/performance.mark ~begin)
           (let [ret# ~form]
             (js/performance.mark ~end)
             (js/performance.measure ~(str "measure_" tag)
                                     ~begin
                                     ~end)
             ret#)))
    form))
