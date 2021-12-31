(ns duct.handler.root
  (:require [integrant.core :as ig :refer [init-key]]
            [reitit.ring :as ring]))

(defmethod init-key :duct.handler/root
  [_ {:keys [router opts]}]
  (ring/ring-handler router))
