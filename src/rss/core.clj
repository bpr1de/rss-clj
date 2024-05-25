(ns rss.core
  (:require [clojure.xml :as xml]
            [rss.constants :as const]
            [rss.feeds.atom]
            [rss.feeds.rss]
            [rss.ons]
            [rss.oss])
  (:import (java.io IOException)
           (java.time Clock LocalDateTime)
           (java.time ZoneOffset)
           (java.time.temporal ChronoUnit)
           (java.util.concurrent TimeUnit))
  (:gen-class)
  )

(defn now
  []
  "Get the current time in UTC."
  (.toInstant (LocalDateTime/now) (ZoneOffset/UTC))
  )

(defn out
  [& args]
  "Timestamp-prefixed printer."
  (println (str "[" (now) "] " (apply str args)))
  )

(defn parse-feed
  [xml]
  "Parse an XML feed according to its type, either RSS 2.0 or Atom, and return
   a sequence of articles from the feed."
  (let [tag (:tag xml) content (:content xml)]
    (case tag
      ;; Parse RSS feeds
      :rss (if (and (= (:version (:attrs xml)) "2.0")
                    (= (:tag (first content)) :channel))
             (for [e (:content (first content)) :when (= (:tag e) :item)]
               (rss.feeds.rss/make-article (:content e))))
      ;; Parse Atom feeds
      :feed (for [e (:content xml) :when (= (:tag e) :entry)]
              (rss.feeds.atom/make-article (:content e)))
      (out "Unsupported feed type: " tag)
      )
    )
  )

(defn valid-article?
  [article]
  "Tests whether the article we parsed is suitable for acting on."
  (and article (every? (complement nil?) (vals article)))
  )

(defn get-topic
  [xml]
  "Return the ONS topic OCID from the configuration."
  (if (= (:tag xml) :topic)
    (:ons_ocid (:attrs xml))
    )
  )

 (defn get-interval
   [xml]
   "Return the polling interval from the configuration, or a default.
    Minimum interval is 1 minute."
   (let [v (and xml (= (:tag xml) :topic) (:interval (:attrs xml)))]
     (max 1 (Integer/parseInt (if (nil? v) "10" v)))
     )
   )

(defn get-feeds
  [xml]
  "Return the list of feeds from the configuration."
  (if (= (:tag xml) :topic)
    (for [x (:content xml) :when (= (:tag x) :feed)]
      (:link (:attrs x))
      )
    )
  )

(defn get-config-path
  [& args]
  "Determine a config path from the command-line arguments and environment:
   1.) if a command-line argument was given, use it; else
   2.) if the config path environment variable is set, use it; else
   3.) try the user's own ~/.rss file;"
  (or (first args)
      (System/getenv const/rss-config-path-key)
      (str (System/getProperty "user.home") "/.rss")
      )
  )

(defn get-config-reference
  [name]
  "Get a readable reference to the configuration specified by the name.
   For Object Storage paths (prefixed by \"oss:\"), return an InputStream
   that can be read directly. Other patterns are assumed to be local
   filenames or remote URLs that can be read as-is."
  (if (re-matches #"^oss:.*" name)
    (rss.oss/get-handle-for (next (clojure.string/split name #":")))
    name
    )
  )

(defn read-config
  [config-path]
  "Attempt to read and parse the XML config file specified by the path."
  (try
    (out "Attempting to read '" config-path "'")
    (xml/parse (get-config-reference config-path))
    (catch IOException e
      (out "Error reading configuration '" config-path "': " (.getMessage e))
      )
    )
  )

(defn -main
  [& args]
  "Loop forever reading an XML config file and the RSS feeds described in it,
   publishing each article to the configured notification client. On startup,
   notify for all articles posted in the last week."
  (let [config-path (apply get-config-path args)
        last-cycle (atom (.minus (now) 7 ChronoUnit/DAYS))]

    ;; Loop indefinitely.
    (loop [since (deref last-cycle)]

      (reset! last-cycle (now))

      ;; Reload the config and generate a new notification client on each loop.
      (let [config (read-config config-path)
            notification-client (rss.ons/make-client)]

        ;; If there are no feeds in the configuration, just warn about it.
        (when (nil? (first (get-feeds config)))
          (out "Warning: no feeds in the configuration")
          )

        (out "Checking for articles posted since: " since)

        ;; For each feed in the config...
        (doseq [feed (get-feeds config)]
          (try
            (out "Checking feed " feed "...")
            ;; For each article in the feed...
            (doseq [article (parse-feed (xml/parse feed))]
              (if (valid-article? article)
                ;; If this was posted since the last iteration, notify.
                (when (.isAfter (:date article) since)
                  (rss.ons/notify notification-client (get-topic config) article)
                  )
                (out "Invalid article: " (or article "(null)"))
                )
              )

            (catch Exception e
              (out "Failed to read feed: " feed ": " (.getMessage e))
              )
            )
          )

        (out "Sleeping for " (get-interval config) " minutes")
        (Thread/sleep (.. (TimeUnit/MINUTES) (toMillis (get-interval config))))

        ;; Clean up the notification client.
        (.close notification-client)
        )

      ;; Advance the time window.
      (recur (deref last-cycle))
      )
    )
  )
