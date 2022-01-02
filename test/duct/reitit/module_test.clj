(ns duct.reitit.module-test
  (:require [clojure.test :refer [deftest testing is are]]
            [duct.reitit.module]
            [foo.handler]
            [foo.handler.plus]
            [duct.core :as core]
            [integrant.core :as ig]
            [taoensso.timbre :refer [spy]]
            [reitit.ring :as ring]
            [reitit.core :as r]
            [fipp.clojure :refer [pprint]]
            [duct.reitit.util :refer [to-edn]]))

(core/load-hierarchy)

(derive :foo/database :duct/const)
(derive :foo/index-path :duct/const)

(def basic-config
  {:duct.module/reitit {}
   :duct.module/logging {:set-root-config? true}
   :duct.profile/base
   {:duct.core/project-ns 'foo
    :duct.core/handler-ns 'handler ; default value
    :duct.core/middleware-ns 'middleware ; default value

    :foo/database [{:author "tami5"}]
    :foo/index-path "resources/index.html"
    :foo.handler/exceptions {}
    :duct.logger/timbre {:set-root-config? true
                         :level :trace}

    :duct.reitit/routes
    [["/" :index]
     ["/author" :get-author]
     ["/ping" {:get {:handler :ping}}]
     ["/plus" {:post :plus/with-body
               :get 'plus/with-query}]
     ["/divide" {:get :divide}]]

    :duct.reitit/registry
    {:index {:path (ig/ref :foo/index-path)}
     :ping  {:message "pong"}
     :plus/with-body {}
     :get-author {}
     :divide {}}

    :duct.reitit/options
    {:muuntaja true ; default true, can be a modified instance of muuntaja.
     :exception {:handlers (ig/ref :foo.handler/exceptions)
                 :log? true ;; default true.
                 :pretty? true} ;; default in dev
     :coercion ;; coercion configuration, default nil.
     {:coercer 'spec ; coercer to be used
      :pretty-coercion? true ; whether to pretty print coercion errors
      :formater nil} ; function that takes spec validation error map and format it
     :environment ;; Keywords to be injected in requests for convenience.
     {:db (ig/ref :foo/database)}
     :middleware [] ;; Global middleware to be injected. expected registry key only
     :cross-origin ;; cross-origin configuration, the following defaults in for dev and local profile
     {:origin [#".*"] ;; What origin to allow
      :methods [:get :post :delete :options]}}}}) ;; which methods to allow

(defmethod ig/init-key :foo.handler/get-author [_ _]
  (fn [{{:keys [db]} :environment}]
    {:status 200 :body (first db)}))

(defn- routes [router]
  (reduce (fn [acc [k v]] (assoc acc k v)) {} (r/routes router)))

(deftest module-test
  (let [config (ig/init (core/prep-config  basic-config))]
    (testing "should read without errors"
      (is (map? config)))

    (testing "should merge registry's integrant keys"
      (are [x] (not= nil (x config))
        :foo.handler/ping
        :foo.handler/index
        :foo.handler.plus/with-body))

    (testing "should initialized duct.router/reitit"
      (let [router (config :duct.router/reitit)
            routes (routes router)]
        (is (= :reitit.core/router (type router)))
        (is (= 5 (count routes)))
        (is (vector? (-> (get routes "/") :environment :db)))
        (are [route path] (fn? (get-in (r/match-by-path router route) path))
          "/"     [:data :handler]
          "/ping" [:data :get :handler]
          "/plus" [:data :get :handler]
          "/plus" [:data :post :handler]
          "/author" [:data :handler])))

    (testing "reitit ring handler"
      (let [handler (:duct.handler/root config)]
        (is (fn? handler))
        (is (nil? (handler {:request-method :get :uri "/not-a-route"})))
        (is (string? (:body (handler {:request-method :get :uri "/"}))))
        (is (= "pong" (-> {:request-method :get :uri "/ping"} handler to-edn :message)))
        (is (= 9 (-> {:request-method :post :uri "/plus" :body-params {:y 3 :x 6}} handler to-edn :total)))
        (is (= 9 (-> {:request-method :get :uri "/plus" :query-params {:y 3 :x 6}} handler to-edn :total)))
        (is (= "tami5" (-> {:request-method :get :uri "/author"} handler to-edn :author)))
        (is (= "Divide by zero" (-> {:request-method :get :uri "/divide" :body-params {:y 0 :x 0}} handler to-edn :cause)))))))
