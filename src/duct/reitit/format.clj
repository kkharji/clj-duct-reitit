(ns duct.reitit.format
  (:require [clojure.string :as str]
            [duct.reitit.request :as request]
            [expound.alpha :as expound]))

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


