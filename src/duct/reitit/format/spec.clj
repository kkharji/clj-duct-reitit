(ns duct.reitit.format.spec
  (:require [expound.alpha :as expound]
            [clojure.spec.alpha :as s]))

(defn- spec-print [{:keys [pre problems print-spec?]}]
  (let [cfg {:theme :figwheel-theme :print-specs? print-spec?}
        -print (expound/custom-printer (if pre (assoc cfg :value-str-fn pre) cfg))]
    (-print problems)))

(defn pretty [data {:keys [print-spec? request-info]}]
  (with-out-str
    (spec-print
     {:problems (:problems data)
      :print-spec? print-spec?
      :pre (fn [_name form path _value]
             (let [message (str (pr-str form) "\n\n" "Path: " (pr-str path))]
               (if request-info
                 (str request-info "\n\n" message)
                 message)))})))

(defn compact [data {:keys [request-info]}]
  (let [errors (->> data :problems ::s/problems (mapv #(if (empty? (:path %))
                                                         (select-keys % [:pred])
                                                         (select-keys % [:path :pred]))))
        message (format "Spec Coercion Errors(%s): %s" (pr-str (:value data)) (pr-str errors))]
    (if request-info
      (str message ", Request: " request-info) message)))
