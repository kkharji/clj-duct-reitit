(ns duct.reitit.middleware.custom
  (:require [duct.reitit.util :refer [defm new-date new-uuid spy]]
            [duct.reitit.request :as request]
            [duct.reitit.format :as format]))

(defm environment-middleware [{:keys [environment]} _ handler request]
  (->> environment
       (merge {:start-date (new-date) :id (new-uuid)})
       (into request)
       (handler)))

(defm logger-request-middleware  [{{:keys [log level pretty?]} :logger} _ handler request]
  (log level (format/request-starting request pretty?))
  (handler (assoc request :start-ms (System/currentTimeMillis))))

(defm logger-response-middleware [{{:keys [log level pretty?]} :logger} _ handler request]
  (log level (format/request-completed request pretty?))
  (handler request))

