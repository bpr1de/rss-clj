(ns rss.core-test
  (:require [clojure.xml :as xml])
  (:require [clojure.test :refer :all]
            [rss.core :refer :all]))

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
           "2024-05-23T00:53:14"))
    (is (= (str (rss.feeds.atom/parse-date
           "2024-05-22T20:52:01-04:00"))
           "2024-05-23T00:52:01"))
    (is (= (str (rss.feeds.atom/parse-date
           "2024-05-22T20:52:01+04:30"))
           "2024-05-22T16:22:01"))))

;; Validate that articles' validity can be correctly ascertained.
(deftest article-validity
  (testing "Checking valid/invalid article handling"
    (let [valid-article (parse-feed (xml/parse "test/rss-feed.xml"))
          invalid-articles (parse-feed (xml/parse "test/invalid-rss-feed.xml"))]
      (is (valid-article? (first valid-article)))
      (is (every? #(not (valid-article? %)) invalid-articles)))))

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
             "2024-05-23T00:52"))
      (is (= (:title (nth articles 0))
             (str "Flood Warning issued May 22 at 8:52PM EDT until May 26 at "
                  "1:00PM EDT by NWS Tallahassee FL")))

      ;; Second article
      (is (= (:link (nth articles 1))
             (str "https://api.weather.gov/alerts/urn:oid:2.49.0.1.840.0."
                  "b5b8e1443ad052cbcebb03938824c1d699455e55.001.1.cap")))
      (is (= (str (:date (nth articles 1)))
             "2024-05-23T00:52"))
      (is (= (:type (nth articles 1)) :atom)))))
