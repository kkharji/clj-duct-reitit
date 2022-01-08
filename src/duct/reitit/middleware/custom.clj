(ns duct.reitit.middleware.custom
  (:require [duct.reitit.util :refer [defm new-date new-uuid]]))

(defm environment-middleware [data _ h r]
  (h (into r (data :environment))))

(defm initialize-middleware  [_ _ h r]
  (h (assoc r :id  (new-uuid) :start-date (new-date))))
