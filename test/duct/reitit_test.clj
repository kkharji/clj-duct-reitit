(ns duct.reitit-test
  (:require [clojure.test :refer [deftest testing is are]]
            [duct.reitit]
            [foo.handler]
            [foo.handler.plus]
            [duct.core :as core]
            [integrant.core :as ig]
            [reitit.ring :as ring]
            [reitit.core :as r]
            [fipp.clojure :refer [pprint]]
            [duct.reitit.util :refer [to-edn spy]]))

(core/load-hierarchy)

(defn- new-config-handling [base & [profiles]]
  (let [config (core/build-config base profiles)
        in-config (partial get-in config)
        in-options #(in-config (cons :duct.reitit/options (if (vector? %) % [%])))]
    [config in-options]))

(deftest configuration-handling
  (testing "Configuration keys and values"
    (let [base {:duct.module/reitit {}
                :duct.profile/base {:duct.core/project-ns 'foo}}
          config (core/prep-config base)]

      ;; Reitit Module keys used for futher processing
      (is (= [:options :middleware :registry]
             (->> (keys config)
                  (filterv #(= "duct.reitit" (namespace %)))
                  (mapv #(keyword (name %))))))

      (are [key value] (= value (key config))
        ;; Defaulhandler middleware namespace
        :duct.core/handler-ns    'handler

        ;; Default middleware namespace
        :duct.core/middleware-ns 'middleware

        ;; Configuration Pass it reitit router to initialize it
        :duct.router/reitit      {:routes nil,
                                  :middleware (ig/ref :duct.reitit/middleware)
                                  :registry (ig/ref :duct.reitit/registry)
                                  :options (ig/ref :duct.reitit/options)
                                  :namespaces ["foo.handler" "foo.middleware"]}

        ;; Configuration Pass it ring handler to initialize it
        :duct.handler/root       {:router (ig/ref :duct.router/reitit)
                                  :options (ig/ref :duct.reitit/options)}))

    (testing "Default Environment Options"
      (let [base {:duct.profile/dev {}
                  :duct.profile/base
                  {:duct.core/project-ns 'foo}
                  :duct.module/reitit {}}
            [_ in-options] (new-config-handling base)]
        (are [path value] (-> path in-options (= value))
          [:exception :log?] true ;; log exception enabled by default
          :muuntaja true          ;; Muuntaja formatting is enabled by default
          :environment {}         ;; Empty Environment
          :middleware []          ;; Empty Middleware
          :coercion nil)))        ;; no :coercion configuration

    (testing "Default Development Environment Options"
      (let [base {:duct.profile/dev {}
                  :duct.profile/base
                  {:duct.core/project-ns 'foo}
                  :duct.module/reitit {}}
            [_ in-options] (new-config-handling base [:duct.profile/dev])]
        (are [path value] (-> path in-options (= value))
          [:exception :log?] true          ;; log exception enabled by default
          [:exception :pretty?] true       ;; log exception enabled by default
          [:coercion :pretty?] true        ;; log exception enabled by default
          :muuntaja true                   ;; Muuntaja formatting is enabled by default
          :environment {}                  ;; Empty Environment
          :middleware []                   ;; Empty Middleware
          [:cross-origin :methods] [:get :post :delete :options])  ;; Cross-origin methods
        (is (= ".*" (str (in-options [:cross-origin :origin 0])))))) ;; Cross-origin origin allowed

    (testing "Default Production Environment Options"
      (let [base {:duct.profile/prod {}
                  :duct.profile/base
                  {:duct.core/project-ns 'foo}
                  :duct.module/reitit {}}
            [_ in-options] (new-config-handling base [:duct.profile/prod])]
        (are [path value] (-> path in-options (= value))
          [:exception :log?] true          ;; log exception enabled by default
          [:exception :pretty?] false      ;; No pretty exceptions
          [:coercion :pretty?] false       ;; log exception enabled by default
          :muuntaja true                   ;; Muuntaja formatting is enabled by default
          :environment {}                  ;; Empty Environment
          :middleware []                   ;; Empty Middleware
          :cross-origin nil)))))              ;; No Cross-origin

(derive :foo/database :duct/const)
(derive :foo/index-path :duct/const)

(def basic-config
  {:duct.module/reitit {}
   :duct.module/logging {:set-root-config? true}
   :duct.profile/base
   {:duct.core/project-ns 'foo
    :duct.core/handler-ns 'handler ; default value
    :duct.core/middleware-ns 'middleware ; default value

    :foo/database            [{:author "tami5"}]
    :foo/index-path          "resources/index.html"
    :foo.handler/exceptions  {}

    :duct.logger/timbre      {:set-root-config? true :level :trace}

    :duct.reitit/routes     [["/" :index]
                             ["/author" :get-author]
                             ["/ping" {:get {:handler :ping}}]
                             ["/plus" {:post :plus/with-body
                                       :get 'plus/with-query}]
                             ["/divide" {:get :divide}]]

    ;; Registry to find handlers and local and global middleware
    :duct.reitit/registry  {:index {:path  (ig/ref :foo/index-path)} ;; init foo.handler/index with {:path}
                            :ping  {:message "pong"} ;; init foo.handler/ping with {:message}
                            :plus/with-body {} ;; init foo.handler.plus/with-body
                            :get-author {} ;; init foo.handler/get-author
                            :divide {}} ;; init foo.handler/divide

    ;; Logger to be used in reitit module.
    :duct.reitit/logger      (ig/ref :duct/logger)

    ;; Whether to use muuntaja for formatting. default true, can be a modified instance of muuntaja.
    :duct.reitit/muuntaja   true

    ;; Keywords to be injected in requests for convenience.
    :duct.reitit/environment  {:db (ig/ref :foo/database)}

    ;; Global middleware to be injected. expected registry key only
    :duct.reitit/middleware   []

    ;; Exception handling configuration
    :duct.reitit/exception  {:handlers (ig/ref :foo.handler/exceptions)
                             :log? true ;; default true.
                             :pretty? true} ;; default in dev.

    ;; Coercion configuration
    :duct.reitit/coercion   {:enable true
                             :coercer 'spec ; Coercer to be used
                             :pretty-coercion? true ; Whether to pretty print coercion errors
                             :formater nil} ; Function that takes spec validation error map and format it

    ;; Cross-origin configuration, the following defaults in for dev and local profile
    :duct.reitit/cross-origin {:origin [#".*"] ;; What origin to allow.
                               :methods [:get :post :delete :options]}}}) ;; Which methods to allow.

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
