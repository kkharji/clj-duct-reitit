(ns duct.reitit.log
  (:require [integrant.core :refer [init-key]]
            [duct.logger :as logger]))

(defmethod init-key :duct.reitit/log
  [_ {{:keys [enable logger pretty? exceptions? coercions?]} :logging}]
  (when (and enable (or exceptions? coercions?))
    (if (and logger (not pretty?))
      (fn [level message]
        (logger/log logger level message))
      #(println %2))))


