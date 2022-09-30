(ns rads.deps-info.git
  (:require [clojure.string :as str]))

(defn- http-get-json [client & args]
  (apply (:http-get-json client) args))

(defn default-branch [client lib]
  (get (http-get-json client (format "https://api.github.com/repos/%s/%s"
                                     (namespace lib) (name lib)))
       :default_branch))

(defn clean-github-lib [lib]
  (let [lib (str/replace lib "com.github." "")
        lib (str/replace lib "io.github." "")
        lib (symbol lib)]
    lib))

(defn latest-github-sha [client lib]
  (let [lib (clean-github-lib lib)
        branch (default-branch client lib)]
    (get (http-get-json client (format "https://api.github.com/repos/%s/%s/commits/%s"
                                       (namespace lib) (name lib) branch))
         :sha)))

(defn list-github-tags [client lib]
  (let [lib (clean-github-lib lib)]
    (http-get-json client (format "https://api.github.com/repos/%s/%s/tags"
                                  (namespace lib) (name lib)))))

(defn latest-github-tag [client lib]
  (-> (list-github-tags client lib)
      first))

(defn find-github-tag [client lib tag]
  (->> (list-github-tags client lib)
       (filter #(= (:name %) tag))
       first))
