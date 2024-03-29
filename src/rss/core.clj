(ns rss.core
  (:require [rss.ons])
  (:require [rss.feeds.atom])
  (:require [rss.feeds.rss])
  (:require [clojure.xml :as xml])
  (:import (java.io IOException)
           (java.sql Timestamp)
           (java.time LocalDateTime)
           (java.util.concurrent TimeUnit)
           )
  (:gen-class)
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
      (println (str "Unsupported feed type: " tag))
      )
    )
  )

(defn valid-article?
  [article]
  "Tests whether the article we parsed is suitable for acting on."
  (every? (complement nil?) (vals article))
  )

(defn get-topic
  [xml]
  "Return the topic OCID from the configuration."
  (if (= (:tag xml) :topic)
    (:ocid (:attrs xml))
    )
  )

(defn get-client-type
  [xml]
  "Return the notification client type from the configuration."
  (if (= (:tag xml) :topic)
    (:client (:attrs xml))
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
   2.) if the environment variable $RSS_CONFIG_PATH is set, use it; else
   3.) try the user's own ~/.rss file;"
  (or (first args)
      (System/getenv "RSS_CONFIG_PATH")
      (str (System/getProperty "user.home") "/.rss")
      )
  )

(defn read-config
  [config-path]
  "Attempt to read the XML config file specified by the path (local or remote)."
  (try
    (println (str "Attempting to read '" config-path "'"))
    (xml/parse config-path)
    (catch IOException e
      (println (str "Error reading configuration '" config-path "': "(.getMessage e)))
      )
    )
  )

(defn -main
  [& args]
  "Loop forever reading an XML config file and the RSS feeds described in it,
   publishing each article to the configured notification client."
  (let [config-path (apply get-config-path args)]

    ;; Loop indefinitely, only notifying for new articles.
    (loop [posted-since (Timestamp/valueOf
                          ^LocalDateTime (.. (LocalDateTime/now) (minusDays 7)))
           current-time (Timestamp/valueOf
                          (LocalDateTime/now))]

      (let [config (read-config config-path)
            notification-client (rss.ons/make-client (get-client-type config))]

        ;; If there are no feeds in the configuration, just skip this cycle.
        (when (nil? (first (get-feeds config)))
          (println "Warning: no feeds in the configuration")
          )

        (doseq [feed (get-feeds config)]
          (try
            (println (str "[" current-time "] Checking feed " feed "..."))
            (doseq [article (parse-feed (xml/parse feed))]
              (if (valid-article? article)
                (when (< 0 (.compareTo (:date article) posted-since))
                  (rss.ons/notify notification-client (get-topic config) article)
                  )
                (println (str "Invalid article: " article))
                )
              )

            (catch Exception e
              (println (str "Failed to read feed: " feed ": " (.getMessage e)))
              )
            )
          )
        (println (str "Sleeping for " (get-interval config) " minutes"))
        (Thread/sleep (.. (TimeUnit/MINUTES) (toMillis (get-interval config))))

        ;; Clean up the notification client.
        (.close notification-client)
        )

      ;; Advance the time window.
      (recur current-time (Timestamp/valueOf (LocalDateTime/now)))
      )
    )
  )
