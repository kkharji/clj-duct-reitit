(ns conduit.spec.util
  (:require [malli.registry :as mr]
            [malli.core :as m]))

(defn- inherit-to-type
  "Take a map of keys and spec and returns malli schema
  wrapped with 'type and where each memeber has unqualifed key and the actual
  key."
  [schema mu-type]
  (->> schema
       (mapv (fn [[k _]] [(keyword (name k)) k]))
       (cons mu-type)
       (vec)))

(defn schemas-from-feilds
  "Given a namespace and qualified map of fields
  Return <ns>/<schema> and malli map"
  [fields]
  (let [organize-by-namespace #(assoc-in %1 [(keyword (namespace %2)) %2] %3)
        transfrom-to-malli    #(assoc %1 %2 (inherit-to-type %3 :map))]
    (->> fields
         (reduce-kv organize-by-namespace {})
         (reduce-kv transfrom-to-malli {}))))

(defn optional-keys
  "Same as malli.util/optional-keys but without checking if keys are valid.
  Return the malli map with all keys are optional."
  [m]
  (mapv #(if (vector? %)
           (vec (concat [(first %)] [{:optional true}] (rest %)))
           %) m))

(defn closed-schema
  "Same as malli.util/closed-schema but non-recursive and without checking if
  keys are valid. Return the closed malli map "
  [m]
  (vec (concat [(first m)] [{:closed true}] (rest m))))

(defn set-malli-registry! [& additions]
  (mr/set-default-registry!
   (apply merge (cons (m/default-schemas) additions))))
