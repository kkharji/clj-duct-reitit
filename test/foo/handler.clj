(ns foo.handler
  (:require [clojure.java.io :as io]
            [integrant.core :refer [init-key]]))

(defmethod init-key ::index [_ {:keys [path]}]
  (constantly
   (or (some->> path io/resource slurp
                (hash-map :status 200 :headers {"content-type" "text/html"} :body))
       {:status 404 :body "Not found"})))

(defmethod init-key ::ping [_ {:keys [message]}]
  (constantly
   {:status 200 :body {:message message}}))
