(ns duct.reitit.middleware.logging
  (:require [duct.reitit.util :refer [defm]]
            [fipp.clojure :as fipp]
            [duct.reitit.request :refer [params]]))

(defn ^:private current-time []
  (-> (java.text.SimpleDateFormat. "hh:mm:ss")
      (.format (java.util.Date.))))

(defn ^:private current-ms []
  (System/currentTimeMillis))

(defm request-middleware
  [{{:keys [log level pretty?]} :logger} _ handler {:keys [uri request-method] :as request}]
  (when-not pretty?
    (let [message (str :reitit.request/handling " " (pr-str [request-method uri]) "\n"
                       :reitit.request/handling " " (pr-str [:params (params request)]))]
      (log level message)))
  (->> {:start-ms (current-ms) :time (current-time)}
       (into request)
       (handler)))

(defm response-middleware
  [{{:keys [log level pretty?]} :logger} _ handler {:keys [uri request-method] :as request}]
  (let [ms (str (- (current-ms) (:start-ms request)) " ms")
        pretty  (when pretty?
                  (with-out-str
                    (println "\n" :reitit.request/handling "-----------------------------")
                    (fipp/pprint {request-method uri
                                  :duration ms
                                  :time (:time request)
                                  :params (params request)})
                    (println "")))
        compact (when-not pretty?
                  (str :reitit.request/handling " " (pr-str [:duration ms]) "\n"))]

    (log level (or pretty compact)))
  (handler request))
