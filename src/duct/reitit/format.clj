(ns duct.reitit.format
  (:require [clojure.string :as str]
            [duct.reitit.request :as request]
            [expound.alpha :as expound]
            [duct.reitit.util :refer [spy]]))

(defn spec-print [{:keys [pre problems print-spec?]}]
  (let [cfg {:theme :figwheel-theme :print-specs? print-spec?}
        -print (expound/custom-printer (if pre (assoc cfg :value-str-fn pre) cfg))]
    (-print problems)))

(defn coercion-pretty [problems print-spec? request-info]
  (with-out-str
    (spec-print
     {:problems problems
      :print-spec? print-spec?
      :pre (fn [_name form path _value]
             (let [message (str (pr-str form) "\n\n" "Path: " (pr-str path))]
               (if request-info
                 (str request-info "\n\n" message)
                 message)))})))

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

(defn- wrap-with [title length content]
  (let [header (str title " " (apply str (repeat length "-")))
        footer (apply str (repeat (count header) "-"))]
    (str header "\n\n"
         content
         "\n\n" footer "\n")))

(defn ^:private current-time []
  (-> "hh:mm:ss"
      (java.text.SimpleDateFormat.)
      (.format (java.util.Date.))))

(defn request-starting [request pretty?]
  (let [req-info (request/info request pretty? [:request-method :uri :params])]
    (if-not pretty?
      [:starting req-info]
      (wrap-with "Starting Request" 24 (str "Request Time: " (current-time) "\n" req-info)))))

(defn request-completed [request pretty?]
  (let [ms (- (System/currentTimeMillis) (:start-ms request))
        req-info (request/info request pretty? [:request-method :uri])]
    (if-not pretty?
      [:completed (assoc req-info :completed-in ms)]
      (wrap-with "Finishing Request" 24 (str req-info "\n" "Request Duration: " ms " ms")))))
