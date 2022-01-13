(ns duct.reitit.format
  (:require [clojure.string :as str]
            [duct.reitit.request :as request]
            [expound.alpha :as expound]
            [duct.reitit.util :refer [spy]]
            [reitit.coercion :refer [-get-name] :rename {-get-name spec-type}]
            [duct.reitit.format.spec :as format.spec]
            [duct.reitit.format.malli :as format.malli]))

(defn spec-print [{:keys [pre problems print-spec?]}]
  (let [cfg {:theme :figwheel-theme :print-specs? print-spec?}
        -print (expound/custom-printer (if pre (assoc cfg :value-str-fn pre) cfg))]
    (-print problems)))

(defmulti coercion
  (fn [data _request-info _opts]
    (-> data :coercion spec-type)))

(defmethod coercion :spec [data {:keys [pretty?] :as opts} request-info]
  (let [opts (assoc opts :request-info request-info)]
    (if pretty?
      (format.spec/pretty data opts)
      (format.spec/compact data opts))))

(defmethod coercion :malli [data {:keys [pretty?] :as opts} request-info]
  (let [opts (assoc opts :request-info request-info)]
    (if pretty?
      (format.malli/pretty data opts)
      (format.malli/compact data opts))))

(defn exception-pretty [req-info ex-trace ex-cause ex-message]
  (let [header "-- Exception Thrown ----------------"
        footer (str "\n" (apply str (repeat (count header) "-")) "\n")
        ifline #(when %1 (str "Exception " (str/upper-case %2) ": " (pr-str %1) "\n"))
        header (str "\n" header "\n\n")]
    (str header
         (when req-info (str req-info "\n"))
         (ifline ex-cause "cause")
         (ifline ex-message "message")
         (ifline ex-trace "trace")
         footer)))

(defn exception-compact [request ex-trace ex-message]
  (format "Exception: :uri %s, :method %s, :params %s, :message %s, :trace %s"
          (request :uri)
          (request :request-method)
          (request/params request)
          ex-message ex-trace))

(defn trace-compact [exception]
  (->> (.getStackTrace exception)
       (map #(str (.getFileName %) ":" (.getLineNumber %)))
       (take 5)
       (str/join " => ")))
