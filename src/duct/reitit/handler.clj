(ns duct.reitit.handler
  (:require [integrant.core :as ig :refer [init-key]]
            [reitit.ring :as ring]))

(defmethod init-key :duct.handler/root
  [_ {:keys [router options]}]
  (ring/ring-handler router))
