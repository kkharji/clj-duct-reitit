(ns duct.reitit.middleware.coercion
  (:require   [reitit.ring.coercion :as rcc]
              [reitit.ring.middleware.exception :as exception]
              [duct.reitit.util :refer [spy]]))

;; TODO: pretty format using format/coercion-pretty-format
(defn get-coercion-exception-handler [status]
  (let [handler (exception/create-coercion-handler status)]
    (fn [exception request]
      (handler exception request))))

(defn get-exception-handler
  [{:keys [with-formatted-message?] :as _coercion}]
  (when with-formatted-message?
    {:reitit.coercion/request-coercion (get-coercion-exception-handler 400)
     :reitit.coercion/response-coercion (get-coercion-exception-handler 500)}))

(defn get-middleware
  [{{:keys [enable with-formatted-message?]} :coercion
    {:keys [coercions? exceptions?]} :logging}]
  (when enable
    (let [with-exception (or coercions? exceptions? with-formatted-message?)]
      [(when-not with-exception rcc/coerce-exceptions-middleware)
       rcc/coerce-request-middleware
       rcc/coerce-response-middleware])))
