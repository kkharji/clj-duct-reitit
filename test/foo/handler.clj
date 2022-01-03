(ns foo.handler
  (:require [clojure.java.io :as io]
            [integrant.core :refer [init-key]]))

(defmethod init-key ::index [_ {:keys [path]}]
  (constantly
   (or (some->> path io/resource slurp
                (hash-map :status 200 :headers {"content-type" "text/html"} :body))
       {:status 404 :body "Not found"})))

(defmethod init-key ::ping [_ {:keys [message]}]
  (constantly
   {:status 200 :body {:message message}}))

(defmethod init-key ::divide [_ _]
  (fn [{{:keys [x y]} :body-params}]
    {:status 200 :body (/ x y)}))

(defmethod init-key ::exceptions [_ _]
  {java.lang.NullPointerException
   (fn [_ r]
     {:status 500
      :body {:cause "No parameters received"
             :uri (:uri r)}})
   java.lang.ArithmeticException
   (fn [e r]
     {:status 500
      :body {:cause (ex-message e)
             :data (:body-params r)
             :uri (:uri r)}})})

(defmethod init-key :foo.handler/get-author [_ _]
  (fn [{:keys [db]}]
    {:status 200 :body (first db)}))


