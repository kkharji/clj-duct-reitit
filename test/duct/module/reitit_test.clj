(ns duct.module.reitit-test
  (:require [clojure.test :refer [deftest testing is are]]
            [duct.module.reitit]
            [foo.handler]
            [foo.handler.plus]
            [duct.core :as core]
            [integrant.core :as ig]))

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
  (let [config (core/build-config basic-config)]
    (testing "should read without errors"
      (is (map? config)))
    (testing "should merge registry's integrant keys"
      (are [x] (not= nil (x config))
        :foo.handler/ping
        :foo.handler.plus/with-body))))
