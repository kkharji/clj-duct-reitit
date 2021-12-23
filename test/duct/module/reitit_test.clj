(ns duct.module.reitit-test
  (:require [clojure.test :refer [deftest testing is]]
            [duct.module.reitit]
            [duct.core :as core]))

(core/load-hierarchy)

(def basic-config
  '{:duct.module/reitit
    {:routes
     [["/" :index]
      ["/ping" {:get {:handler :pong}}]
      ["/plus" {:get plus/with-query
                :post :plus/with-body}]]
     :registry
     [[:index {:path (ig/ref :index-path)}]
      [:ping {:message "pong"}]
      [:plus/with-body]]}

    :duct.profile/base
    {:duct.core/project-ns foo
     :duct.core/handler-ns handler ; default value
     :duct.core/middleware-ns middleware ; default value

     :foo/database {}
     :foo/index-path "resources/test-index.html"

     :duct.module.reitit/cors ;; defaults in for dev and local environment
     {:origin [#".*"]
      :methods [:get :post :delete :options]}

     :duct.module.reitit/options
     {:coercion :data-spec ; default nil
      :environment {:db (ig/ref :foo/database)} ; default nil
      :middlewares []}}})

(deftest module-test
  (testing "should read without errors"
    (is (core/build-config basic-config))))
