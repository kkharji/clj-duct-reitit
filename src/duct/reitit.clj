(ns duct.reitit
  (:require [duct.core :as core :refer [merge-configs]]
            [duct.reitit.handler]
            [duct.reitit.util :as util :refer [get-namespaces resolve-registry with-registry spy]]
            [integrant.core :refer [init-key] :as ig]
            [duct.logger :as logger]))

(def ^:private base-config
  {:duct.core/handler-ns 'handler
   :duct.core/middleware-ns 'middleware
   ::environment {}
   ::middleware []
   ::muuntaja true
   ::coercion nil
   ::logging {:exceptions? true
              :pretty? false :logger nil}})
   ; ::logger (m/displace (ig/ref :duct/logger))})

(def ^:private configs
  {:development
   {::logging {:pretty? true
               :coercions? true
               :requests? true}
    ::muuntaja true
    ::cross-origin {:origin [#".*"] :methods [:get :post :delete :options]}}
   :production
   {::logging {:exceptions? false
               :coercions? false
               :requests? true
               :pretty? false}}})

(defn- merge-to-options [config]
  (reduce-kv
   (fn [acc k v]
     (if (= "duct.reitit" (namespace k))
       (assoc-in acc [::options (keyword (name k))] v)
       (assoc acc k v)))
   {} config))

(defmethod init-key ::log [_ {{:keys [enable logger pretty? exceptions? coercions?]} :logging}]
  (when (and enable (or exceptions? coercions?))
    (if (and logger (not pretty?))
      (fn [level message]
        (logger/log logger level message))
      println)))

(defmethod init-key :duct.module/reitit [_ _]
  (fn [{:duct.reitit/keys [registry routes]
        :duct.core/keys [environment] :as user-config}]
    (let [env-config (or (configs environment) {})
          config     (merge-to-options (merge-configs base-config env-config user-config))
          namespaces (get-namespaces config)
          registry   (resolve-registry namespaces registry)
          merge      (partial with-registry config registry)]
      (merge
       {::registry          (reduce-kv (fn [m k v] (assoc m k (ig/ref (first v)))) {} registry)
        ::log               (ig/ref ::options)
        :duct.handler/root  {:options (ig/ref ::options)
                             :router (ig/ref :duct.router/reitit)}
        :duct.router/reitit {:routes routes
                             :log (ig/ref ::log)
                             :registry (ig/ref ::registry)
                             :options (ig/ref ::options)
                             :namespaces namespaces}}))))
