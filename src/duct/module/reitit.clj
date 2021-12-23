(ns duct.module.reitit
  (:require [integrant.core :refer [init-key]]))

(defmethod init-key :duct.module/reitit [_ {:keys [routes registry]}]
  (fn [config]
    config))
