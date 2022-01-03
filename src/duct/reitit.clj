(ns duct.reitit
  (:require [duct.core :as core :refer [merge-configs]]
            [duct.core.merge :as m]
            [duct.reitit.handler]
            [duct.reitit.util :as util :refer [get-namespaces resolve-registry with-registry spy]]
            [integrant.core :refer [init-key] :as ig]))

(def ^:private base-config
  {:duct.core/handler-ns 'handler
   :duct.core/middleware-ns 'middleware
   ::exception {:log?  true}
   ::environment {}
   ::middleware []
   ::muuntaja true
   ::coercion nil})
   ; ::logger (m/displace (ig/ref :duct/logger))})

(def ^:private configs
  {:development
   {::exception {:pretty? true}
    ::coercion {:pretty? true}
    ::muuntaja true
    ::cross-origin {:origin [#".*"] :methods [:get :post :delete :options]}}
   :production
   {::exception {:pretty? false}
    ::coercion {:pretty? false}}})

(comment
  (merge-configs base-config
                 (configs :development)
                 {:duct.core/handler-ns 'server.handler
                  ::coercion {:coercer 'spec}
                  ::muuntaja true}))

(defn- merge-to-options [configs]
  (reduce-kv
   (fn [acc k v]
     (if (= "duct.reitit" (namespace k))
       (assoc-in acc [::options (keyword (name k))]  v)
       (assoc acc k v)))
   {} configs))

(defmethod init-key :duct.module/reitit [_ _]
  (fn [{:duct.reitit/keys [registry routes]
        :duct.core/keys [environment]
        :as config}]
    (let [env-config (or (configs environment) {})
          config     (merge-to-options (merge-configs base-config env-config config))
          namespaces (get-namespaces config)
          registry   (resolve-registry namespaces registry)
          merge      (partial with-registry config registry)]
      (merge
       {::middleware  (ig/ref ::options)
        ::registry   (reduce-kv (fn [m k v] (assoc m k (ig/ref (first v)))) {} registry)
        :duct.handler/root      (hash-map :options (ig/ref ::options) :router (ig/ref :duct.router/reitit))
        :duct.router/reitit     {:routes routes
                                 :middleware (ig/ref ::middleware)
                                 :registry (ig/ref ::registry)
                                 :options (ig/ref ::options)
                                 :namespaces namespaces}}))))

