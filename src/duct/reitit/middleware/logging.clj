(ns duct.reitit.middleware.logging
  (:require [integrant.core :refer [init-key]]
            [duct.reitit.middleware.format :refer [ex-format]]
            [duct.logger :as logger]))

(defmethod init-key :duct.reitit/logging
  [_ {{:keys [enable logger pretty? exceptions? coercions?] :as config} :logging}]
  (when (and enable (or exceptions? coercions?))
    (let [enabled    {:coercions? coercions? :exceptions? exceptions?}
          production (and logger (not pretty?))
          loggerfn   (if production #(logger/log logger %1 %2) #(println %2))
          loggerconf (assoc enabled :with-req-info? true :pretty? pretty?)
          ex-logger  (fn [next ex req]
                       (loggerfn :error (ex-format ex req loggerconf))
                       (next ex req))]
      (merge config enabled
             {:log loggerfn
              :ex-logger ex-logger
              :request-logger-middleware nil}))))
