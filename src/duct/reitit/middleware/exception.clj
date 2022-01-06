(ns duct.reitit.middleware.exception
  (:require [reitit.ring.middleware.exception :as exception :refer [create-exception-middleware default-handlers]]
            [duct.reitit.middleware.coercion :as coercion]
            [duct.reitit.middleware.format :refer [ex-format]]
            [duct.reitit.util :refer [spy]]))

(defn ^:private get-exception-wrapper [log config]
  (let [config (merge config {:with-req-info? true})]
    (fn [handler exception request]
      (log :error (ex-format exception request config))
      (handler exception request))))

(defn get-middleware
  "Create custom exception middleware."
  [{:keys [coercion exception log logging]}]
  (let [{:keys [enable coercions? exceptions?]}  logging
        should-wrap (or (and enable coercions?) (and enable exceptions?))
        coercion-handlers (when coercions? (coercion/get-exception-handler coercion))
        exception-wrapper (when should-wrap {::exception/wrap (get-exception-wrapper log logging)})
        create-middleware #(create-exception-middleware (apply merge default-handlers %))]
    (create-middleware
     [exception-wrapper
      coercion-handlers
      exception])))
