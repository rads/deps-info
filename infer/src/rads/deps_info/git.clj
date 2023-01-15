(ns rads.deps-info.git
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [babashka.fs :as fs]
            [babashka.process :refer [sh]]))

(defn- ensure-git-dir [client & args]
  (apply (:ensure-git-dir client) args))

(defn default-branch [client git-url]
  (let [lib-dir (ensure-git-dir client git-url)
        remote-info (sh "git remote show origin" {:dir lib-dir
                                                  :extra-env {"LC_ALL" "C"}})
        [[_ branch]] (->> (:out remote-info)
                          str/split-lines
                          (some #(re-seq #"HEAD branch: (\w+)" %)))]
    branch))

(defn latest-git-sha [client git-url]
  (let [lib-dir (ensure-git-dir client git-url)
        branch (default-branch client git-url)
        log-result (sh ["git" "log" "-n" "1" branch "--pretty=format:\"%H\""]
                       {:dir lib-dir})]
    (edn/read-string (:out log-result))))

(defn find-git-tag [client git-url tag]
  (let [lib-dir (ensure-git-dir client git-url)
        tag-result (sh ["git" "rev-parse" tag] {:dir lib-dir})]
    {:name (str tag)
     :commit {:sha (str/trim (:out tag-result))}}))

(defn latest-git-tag [client git-url]
  (let [lib-dir (ensure-git-dir client git-url)
        log-result (sh "git describe --tags --abbrev=0" {:dir lib-dir})
        tag (edn/read-string (:out log-result))]
    (find-git-tag client git-url tag)))

(defn- clean-github-lib [lib]
  (let [lib (str/replace lib "com.github." "")
        lib (str/replace lib "io.github." "")
        lib (symbol lib)]
    lib))

(defn github-repo-ssh-url [lib]
  (str "git@github.com:" (clean-github-lib lib) ".git"))
