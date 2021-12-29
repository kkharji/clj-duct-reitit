(ns user
  (:require [clj-dev.core :as d]
            [potemkin :as p]))

(p/import-vars [d start pause resume stop restart watch system config go, halt, reset, reset-all])
(d/init {})

