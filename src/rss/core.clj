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
  "Parse an XML feed according to its type, either RSS 2.0 or Atom, and return
   a sequence of articles from the feed."
  [xml]
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

(defn valid-article
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

(defn get-feeds
  [xml]
  "Return the list of feeds from the configuration."
  (if (= (:tag xml) :topic)
    (for [x (:content xml) :when (= (:tag x) :feed)]
      (:link (:attrs x))
      )
    )
  )

(defn parse-args
  [& args]
  "Parse the command line arguments and return a configuration from it."
  (try
    ((fnil xml/parse (str (System/getProperty "user.home") "/.rss")) (first args))
    (catch IOException e
      (do
        (println (.getMessage e))
        (System/exit -1)
        )
      )
    )
  )

(defn -main
  [& args]
  "Read an (optionally specified) XML config file and loop forever over the RSS
   feeds described in it, publishing each article to a notification client."
  (let [config (apply parse-args args)
        notification-client (rss.ons/make-client (get-client-type config))]

    ;; If there are no feeds in the configuration, just exit.
    (when (nil? (first (get-feeds config)))
      (println "No feeds in the configuration; exiting.")
      (System/exit -1)
      )

    ;; Loop indefinitely, only notifying for new articles.
    (loop [previous-time (Timestamp/valueOf ^LocalDateTime
                              (.. (LocalDateTime/now) (minusDays 7)))
           sleep-duration 0]

      (Thread/sleep sleep-duration)

      (doseq [feed (get-feeds config)]
        (try
          (doseq [article (parse-feed (xml/parse feed))]
            (if (valid-article article)
              (when (< 0 (.compareTo (:date article) previous-time))
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

      (recur (Timestamp/valueOf (LocalDateTime/now))
             (.. (TimeUnit/MINUTES) (toMillis 10)))
      )
    )
  )