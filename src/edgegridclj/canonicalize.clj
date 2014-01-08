(ns edgegridclj.canonicalize
  (:require [clojure.data.codec.base64 :as b64])
  (:import (java.security MessageDigest)
           (com.google.api.client.http GenericUrl
                                       HttpRequest
                                       HttpRequestFactory
                                       HttpRequestInitializer
                                       HttpTransport)
           (com.google.api.client.http.apache ApacheHttpTransport)))

;; configurable
(def headers-to-use [])  ;; or e.g (def headers-to-use ["host" "accept-encoding"])

(defn canonicalizeUri
  "Require URIs to begin with a forward slash"
  [s]
  (if (= \/ (first s)) s (str "/" s)))

(defn canonicalizeHeader
  "Header value canonicalization rules:
     1) Use first header only, lowercase.
     2) No whitespace at start or end.
     3) Internal whitespace reduced to one space."
  [request headername]
  (when-let [hval (.getFirstHeaderStringValue (.getHeaders request) headername)]
    (str (.toLowerCase headername) ":"
         (clojure.string/replace (.trim hval) #"\s+" " ") \tab)))

(defn getContentHash
  "Cryptographic hash of request content. Currently only for POSTs"
  [request]
  (if (= "post" (clojure.string/lower-case (.getRequestMethod request)))
    (->> request
         (.getContent)
         (.getBytes)
         (.digest (MessageDigest/getInstance "SHA-256"))
         (b64/encode))))

(defn aka-open-canonicalize
  "Canonicalize request data"
  [request]
  (str (.toUpperCase (.getRequestMethod request)) \tab
       (.toLowerCase (.getScheme (.toURI (.getUrl request)))) \tab
       (.getFirstHeaderStringValue (.getHeaders request) "host") \tab
       (canonicalizeUri (.buildRelativeUrl (.getUrl request))) \tab
       (apply str (map (partial canonicalizeHeader request) headers-to-use))
       (getContentHash request) \tab))
