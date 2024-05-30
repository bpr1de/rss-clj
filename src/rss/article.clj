(ns rss.article)

;; Definition of the record which represents an RSS article.
;; All articles will have a type (e.g. Atom or RSS), a title, a description,
;; a hyperlink, and a publication date.
(defrecord Article [type title description link date]
  Object
  (toString [_] (format "%s\n%s\n%s\n%s" title description link date)))

(defn valid?
  [article]
  "Tests whether the article we parsed is suitable for acting on."
  (and article (every? (complement nil?) (vals article))))

;; Definition of the record which represents a cache of previously-seen RSS
;; articles.
(defrecord Cache [link date])

(defn make-cache
  []
  "Returns an empty cache."
  (atom #{}))

(defn cached?
  [cache {link :link date :date}]
  "Returns true if there is a record in the cache matching the given Article."
  (contains? (deref cache) (->Cache link date)))

(defn add-to-cache
  [cache {link :link date :date}]
  "Add a record matching this Article to the cache if not already present."
  (swap! cache conj (->Cache link date)))

(defn prune-cache
  [pred cache]
  "Removes from the cache all records matching the supplied predicate."
  (let [pruned (into #{} (remove pred (deref cache)))]
    (reset! cache pruned)))
