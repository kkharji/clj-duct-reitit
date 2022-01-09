(ns duct.reitit.log
  (:require [integrant.core :refer [init-key]]
            [duct.logger :as logger]))

(defmethod init-key :duct.reitit/log
  [_ {{:keys [logger pretty? exceptions? coercions? requests?]} :logging}]
  (when (or exceptions? coercions? requests?)
    (if (and logger (not pretty?))
      (fn [level message]
        (logger/log logger level message))
      #(println %2))))


