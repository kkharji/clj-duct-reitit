(ns duct.reitit.middleware.coercion
  (:require   [reitit.ring.coercion :as rcc]
              [reitit.ring.middleware.exception :as exception]
              [duct.reitit.util :refer [spy]]))

;; TODO: pretty format using format/coercion-pretty-format
(defn get-coercion-exception-handler [status]
  (let [handler (exception/create-coercion-handler status)]
    (fn [exception request]
      (handler exception request))))

   ;; should be nil when 1. logging is enabled and 2. customized message)
(defn- get-coercion-exception [should-use]
  (when-not should-use rcc/coerce-exceptions-middleware))

(defn get-middleware
  [{:keys [enable with-formatted-message?]}
   {:keys [coercions? exceptions?]}]
  (when enable
    (let [with-exception (or coercions? exceptions? with-formatted-message?)]
      {:coerce-exceptions (get-coercion-exception with-exception)
       :coerce-request rcc/coerce-request-middleware
       :coerce-response rcc/coerce-response-middleware})))

(defn get-exception-handler [{:keys [with-formatted-message?] :as _config} enabled?]
  (when (or enabled? with-formatted-message?)
    {:reitit.coercion/request-coercion (get-coercion-exception-handler 400)
     :reitit.coercion/response-coercion (get-coercion-exception-handler 500)}))
