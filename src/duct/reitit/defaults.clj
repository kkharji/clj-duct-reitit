(ns duct.reitit.defaults)

(def reitit-module-defaults
  {:base    {:duct.core/handler-ns 'handler
             :duct.core/middleware-ns 'middleware
             :duct.reitit/environment {}
             :duct.reitit/middleware []
             :duct.reitit/muuntaja true
             :duct.reitit/coercion nil
             :duct.reitit/logging {:enable true
                                   :exceptions? true
                                   :pretty? false}}

   :development {:duct.reitit/logging
                 {:pretty? true :coercions? true :requests? true}
                 :duct.reitit/muuntaja true
                 :duct.reitit/cross-origin
                 {:origin [#".*"] :methods [:get :post :delete :options]}}

   :production  {:duct.reitit/logging
                 {:exceptions? false
                  :coercions? false
                  :requests? true
                  :pretty? false}}})
