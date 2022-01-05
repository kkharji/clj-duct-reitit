(ns duct.reitit.middleware.exception
  (:require [reitit.ring.middleware.exception :as exception :refer [create-exception-middleware default-handlers]]
            [duct.reitit.middleware.coercion :as coercion]))

(defn- create-middleware [& handlers]
  (->> (cons default-handlers handlers)
       (apply merge)
       (create-exception-middleware)))

(defn get-middleware
  "Create custom exception middleware."
  [{:keys [enable ex-logger coercions? exceptions?]} coercion exception]
  (let [should-wrap (or (and enable coercions?) (and enable ex-logger exceptions?))
        coercion-handlers (coercion/get-exception-handler coercion coercions?)
        exception-wrapper (when should-wrap {::exception/wrap ex-logger})]
    (create-middleware exception-wrapper
                       coercion-handlers
                       exception)))
