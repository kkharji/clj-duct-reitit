(ns foo.handler
  (:require [clojure.java.io :as io]
            [integrant.core :refer [init-key]]))

(defmethod init-key ::index [_ {:keys [path]}]
  (constantly
   {:status 200
    :body (->> path io/resource slurp)}))

(defmethod init-key ::ping [_ {:keys [message]}]
  (constantly
   {:status 200
    :body {:message message}}))
