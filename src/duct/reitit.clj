(ns duct.reitit
  (:require [duct.core :as core :refer [merge-configs]]
            [duct.core.merge :as m]
            [duct.reitit.handler]
            [duct.reitit.util :as util :refer [get-namespaces resolve-registry with-registry spy]]
            [integrant.core :refer [init-key] :as ig]))

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

(defn- merge-to-options [configs]
  (reduce-kv
   (fn [acc k v]
     (if (= "duct.reitit" (namespace k))
       (assoc-in acc [::options (keyword (name k))] v)
       (assoc acc k v)))
   {} configs))

(defmethod init-key :duct.module/reitit [_ _]
  (fn [{:duct.reitit/keys [registry routes]
        :duct.core/keys [environment] :as config}]
    (let [env-config (or (configs environment) {})
          config     (merge-to-options (merge-configs base-config env-config config))
          namespaces (get-namespaces config)
          registry   (resolve-registry namespaces registry)
          regrefs    (reduce-kv (fn [m k v] (assoc m k (ig/ref (first v)))) {} registry)
          merge      (partial with-registry config registry)]
      (merge
       {::logging           (ig/ref ::options)
        ::middleware        {:options (ig/ref ::options) :logging (ig/ref ::logging)}
        ::registry           regrefs
        :duct.handler/root   {:options (ig/ref ::options) :router (ig/ref :duct.router/reitit)}
        :duct.router/reitit  {:routes routes
                              :middleware (ig/ref ::middleware)
                              :registry (ig/ref ::registry)
                              :options (ig/ref ::options)
                              :namespaces namespaces}}))))
