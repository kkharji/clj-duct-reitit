(ns foo.handler
  (:require [clojure.java.io :as io]
            [integrant.core :refer [init-key]]
            [ring.util.http-response :refer [ok content-type not-found]]))

(defmethod init-key ::index [_ {:keys [path]}]
  (constantly
   (or (some-> path io/resource slurp ok (content-type "text/html"))
       (not-found))))

(defmethod init-key ::ping [_ {:keys [message]}]
  (constantly
   (ok {:message message})))
