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
            [duct.reitit.util :refer [to-edn spy]]
            [clojure.string :as str]))

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
      (is (= [:options :registry :middleware :logging]
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
          [:logging :exceptions?] true ;; default types supported by default
          [:logging :pretty?] false      ;; No pretty logging by default.
          [:logging :logger] nil         ;; No logger by default.
          :muuntaja true                 ;; Muuntaja formatting is enabled by default
          :environment {}                ;; Empty Environment
          :middleware []                 ;; Empty Middleware
          :coercion nil)))               ;; no :coercion configuration

    (testing "Default Development Environment Options"
      (let [base {:duct.profile/dev {}
                  :duct.profile/base
                  {:duct.core/project-ns 'foo}
                  :duct.module/reitit {}}
            [_ in-options] (new-config-handling base [:duct.profile/dev])]
        (are [path value] (-> path in-options (= value))
          [:logging :exceptions?] true ;; default types supported by default
          [:logging :coercions?] true ;; default types supported by default
          [:logging :requests?] true ;; default types supported by default
          [:logging :pretty?] true         ;; pretty logging by default.
          [:logging :logger] nil           ;; No logger by default.
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
          [:logging :requests?] true     ;; default types supported by default
          [:logging :coercions?] false ;; default types supported by default
          [:logging :exceptions?] false ;; default types supported by default
          [:logging :pretty?] false     ;; No pretty logging by default.
          [:logging :logger] nil        ;; No logger by default.
          :muuntaja true                ;; Muuntaja formatting is enabled by default
          :environment {}               ;; Empty Environment
          :middleware []                ;; Empty Middleware
          :cross-origin nil)))))        ;; No Cross-origin

(derive :foo/database :duct/const)
(derive :foo/index-path :duct/const)

(def base-config
  {:duct.core/project-ns 'foo

   ;; Where should handlers keys be localated
   :duct.core/handler-ns 'handler

   ;; Where should middleware keys be located
   :duct.core/middleware-ns 'middleware

   ;; Routes Configuration
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
                           :divide {}}    ;; init foo.handler/divide

   ;; Enable exception handlers through a map of class/types and their response function
   :duct.reitit/exception   (ig/ref :foo.handler/exceptions)
   ;; System specific keys
   :foo/database            [{:author "tami5"}]
   :foo/index-path          "resources/index.html"
   :foo.handler/exceptions  {}})

(defn- with-base-config [config]
  {:duct.module/reitit {}
   :duct.module/logging {}
   :duct.profile/base (merge base-config config)})

(def reitit-module-config
  {:duct.logger/timbre {:set-root-config? true :level :trace}

    ;; Logging Configuration
   :duct.reitit/logging  {:enable true
                          :logger (ig/ref :duct/logger)  ;; Logger to be used in reitit module.
                          :exceptions? true
                          :coercions? true
                          :pretty? true}

    ;; Whether to use muuntaja for formatting. default true, can be a modified instance of muuntaja.
   :duct.reitit/muuntaja true

    ;; Keywords to be injected in requests for convenience.
   :duct.reitit/environment {:db (ig/ref :foo/database)}

    ;; Global middleware to be injected. expected registry key only
   :duct.reitit/middleware  []

    ;; Exception handling configuration
   :duct.reitit/exception   (ig/ref :foo.handler/exceptions)

    ;; Coercion configuration
   :duct.reitit/coercion    {:enable true
                             :coercer 'spec
                             :with-formatted-message? true} ; Coercer to be used

    ;; Cross-origin configuration, the following defaults in for dev and local profile
   :duct.reitit/cross-origin {:origin [#".*"] ;; What origin to allow.
                              :methods [:get :post :delete :options]}}) ;; Which methods to allow.

(defn- routes [router]
  (reduce (fn [acc [k v]] (assoc acc k v)) {} (r/routes router)))

(defn- init
  "Takes reitit options and merge it to base-config for testing"
  [config]
  (-> config with-base-config core/prep-config  ig/init))

(deftest module-test
  (let [config (-> reitit-module-config with-base-config core/prep-config ig/init)]
    (testing "Init Result"
      (is (map? config)))

    (testing "Registry Merge"
      (are [x] (not= nil (x config))
        :foo.handler/ping
        :foo.handler/index
        :foo.handler.plus/with-body))

    (testing "Resulting Router"
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

    (testing "Resulting Ring Handler"
      (let [handler (:duct.handler/root config)]
        (is (fn? handler))

        (testing "Ring Routing"
          (is (nil? (handler {:request-method :get :uri "/not-a-route"})))
          (is (string? (:body (handler {:request-method :get :uri "/"}))))
          (is (= "pong" (-> {:request-method :get :uri "/ping"} handler to-edn :message)))
          (is (= 9 (-> {:request-method :post :uri "/plus" :body-params {:y 3 :x 6}} handler to-edn :total)))
          (is (= 9 (-> {:request-method :get :uri "/plus" :query-params {:y 3 :x 6}} handler to-edn :total))))

        (testing "Environment-Keys-Access"
          (is (= "tami5" (-> {:request-method :get :uri "/author"} handler to-edn :author))))))))

; (derive :duct.reitit :duct.reitit-test)
(derive :duct.reitit/logging ::logging)
(derive :duct.reitit/coercion ::coercion)

(defn- request [method uri req]
  (merge req {:request-method method :uri uri}))

(deftest module-behavior
  (testing "Logging:"
    (let [does-include* (fn [ptr] (fn [str] (str/includes? str ptr)))
          spec-pretty?  (does-include* "-- Spec failed --------------------")
          spec-compact? (does-include* "-- Spec failed --------------------")
          ex-pretty?    (does-include* "-- Exception Thrown ----------------")
          ex-compact?   (does-include* "Exception: :uri")
          test-behavior (fn [method url extra-request-keys base cfg checkfn]
                          (let [request (request method url extra-request-keys)
                                handler (-> base (core/merge-configs cfg) init :duct.handler/root)]
                            (->> request handler with-out-str checkfn)))]

      (testing "Exception-Logging:"
        (let [base {::logging {:enable true :pretty? false :logger nil :exception? true}}]
          (are [checkfn cfg] (test-behavior :get "/divide" {:body-params {:y 0 :x 0}} base cfg checkfn)
            ;; Enabled
            ex-compact? {}
            ;; Enabled + logger (cant' check or confirm that)
            empty? {::logging {:logger (ig/ref :duct/logger)}}
            ;; Enabled + Pretty
            ex-pretty? {::logging {:pretty? true}}
            ;; Disabled
            empty? {::logging {:enable false}}
            ;; Disabled through disabling
            empty? {::logging {:exceptions? false}})))

      (testing "Coercion-Logging:"
        (let [base {::coercion {:enable true :coercer 'spec}
                    ::logging  {:enable true :pretty? false :coercions? true}}]
          (are [checkfn cfg] (test-behavior :get "/plus" {:query-params {:y "str" :x 0}} base cfg checkfn)
            ;; Enabled
            spec-compact? {}
            ;; Enabled + logger (cant' check or confirm that)
            empty? {::logging {:logger (ig/ref :duct/logger)}}
            ;; Enabled + Pretty
            spec-pretty? {::logging {:pretty? true}}
            ;; Disabled Logging
            empty? {::logging {:enable false}}
            ;; Disabled Coercion Logging, loggs with exceptions handler instead
            ex-pretty? {::logging {:pretty? true :exceptions? true :coercions? false}}))))))

