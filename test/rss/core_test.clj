(ns rss.core-test
  (:require [clojure.xml :as xml])
  (:require [clojure.test :refer :all]
            [rss.article :refer :all]
            [rss.core :refer :all])
  (:import (java.time.temporal ChronoUnit)))

;; Test parsing of RSS dates for various formats.
(deftest parse-rss-date
  (testing "Parsing RSS dates"
    (is (= (str (rss.feeds.rss/parse-date
           "Wed, 22 May 2024 19:52:30 -0800"))
           "2024-05-23T03:52:30Z"))
    (is (= (str (rss.feeds.rss/parse-date
           "Wed, 22 May 2024 06:13:59 EST"))
           "2024-05-22T10:13:59Z"))))

;; Test parsing of ATOM dates.
(deftest parse-atom-date
  (testing "Parsing ATOM dates"
    (is (= (str (rss.feeds.atom/parse-date
           "2024-05-23T00:53:14+00:00"))
           "2024-05-23T00:53:14Z"))
    (is (= (str (rss.feeds.atom/parse-date
           "2024-05-22T20:52:01-04:00"))
           "2024-05-23T00:52:01Z"))
    (is (= (str (rss.feeds.atom/parse-date
           "2024-05-22T20:52:01+04:30"))
           "2024-05-22T16:22:01Z"))))

;; Validate that articles' validity can be correctly ascertained.
(deftest article-validity
  (testing "Checking valid/invalid article handling"
    (let [valid-article (parse-feed (xml/parse "test/rss-feed.xml"))
          invalid-articles (parse-feed (xml/parse "test/invalid-rss-feed.xml"))]
      (is (valid? (first valid-article)))
      (is (every? #(not (valid? %)) invalid-articles)))))

;; Parse sample RSS unit test data.
(deftest parse-rss-feed
  (testing "Parse RSS feed"
    (let [articles (parse-feed (xml/parse "test/rss-feed.xml"))]
      ;; First article
      (is (= (:link (nth articles 0))
             (str "https://wsdot.wa.gov/about/news/2024/north-spokane-"
                  "corridor-and-wellesley-avenue-close-one-month-starting-"
                  "tuesday-may-28")))
      (is (= (str (:date (nth articles 0)))
             "2024-05-22T16:40:30Z"))
      (is (= (:title (nth articles 0))
             (str "North Spokane Corridor and Wellesley Avenue to close for "
                  "one month starting Tuesday, May 28")))

      ;; Second article
      (is (= (:link (nth articles 1))
             (str "https://wsdot.wa.gov/about/news/2024/sr-123-cayuse-pass-"
                  "scheduled-reopen-may-24-and-sr-410-chinook-pass-remain-"
                  "closed")))
      (is (= (str (:date (nth articles 1)))
             "2024-05-22T11:38:40Z"))
      (is (= (:type (nth articles 1)) :rss)))))

;; Parse sample ATOM unit test data.
(deftest parse-atom-feed
  (testing "Parse ATOM feed"
    (let [articles (parse-feed (xml/parse "test/atom-feed.xml"))]
      ;; First article
      (is (= (:link (nth articles 0))
             (str "https://api.weather.gov/alerts/urn:oid:2.49.0.1.840.0"
             ".601ab091b19b7cc3ccf16c56e68e88c53ff3d7c3.001.1.cap")))
      (is (= (str (:date (nth articles 0)))
             "2024-05-23T00:52:00Z"))
      (is (= (:title (nth articles 0))
             (str "Flood Warning issued May 22 at 8:52PM EDT until May 26 at "
                  "1:00PM EDT by NWS Tallahassee FL")))

      ;; Second article
      (is (= (:link (nth articles 1))
             (str "https://api.weather.gov/alerts/urn:oid:2.49.0.1.840.0."
                  "b5b8e1443ad052cbcebb03938824c1d699455e55.001.1.cap")))
      (is (= (str (:date (nth articles 1)))
             "2024-05-24T01:02:13Z"))
      (is (= (:type (nth articles 1)) :atom)))))

(deftest cache
  (testing "Single item in cache"
    (let [articles (concat (parse-feed (xml/parse "test/rss-feed.xml"))
                           (parse-feed (xml/parse "test/atom-feed.xml")))
          cache (make-cache)]
      ;; Cache begins empty
      (is (empty? (deref cache)))
      (is (not (cached? cache (first articles))))

      ;; Add one article to cache
      (add-to-cache cache (first articles))
      (is (= (count (deref cache)) 1))
      (is (cached? cache (first articles)))
      (is (not (cached? cache (nth articles 1))))))

  (testing "Multiple items in cache"
    (let [articles (concat (parse-feed (xml/parse "test/rss-feed.xml"))
                           (parse-feed (xml/parse "test/atom-feed.xml")))
          cache (make-cache)]

      ;; Add all articles to cache; validate no duplicates
      (doseq [i articles]
        (add-to-cache cache i))
      (doseq [i articles]
        (add-to-cache cache i))
      (is (= (count (deref cache)) (count articles)))
      (doseq [i articles]
        (is (cached? cache i)))))

  (testing "Cache eviction"
    (let [articles (concat (parse-feed (xml/parse "test/rss-feed.xml"))
                           (parse-feed (xml/parse "test/atom-feed.xml")))
          cache (make-cache)
          eviction-date (.minus (rss.feeds.atom/parse-date
                                  "2024-05-22T16:47:31-00:00")
                                7 ChronoUnit/MINUTES)
          expired? #(.isBefore (:date %) eviction-date)]

      ;; Prune empty cache
      (prune-cache expired? cache)
      ;; Add all articles to cache
      (doseq [i articles]
        (add-to-cache cache i))
      (is (expired? (nth articles 0)))
      (is (expired? (nth articles 1)))
      (is (not (expired? (nth articles 2))))
      (is (not (expired? (nth articles 3))))
      ;; Evict old entries
      (prune-cache expired? cache)
      ;; Compare size
      (is (= (count (deref cache)) 2))
      (is (= (count articles) 4)))))
