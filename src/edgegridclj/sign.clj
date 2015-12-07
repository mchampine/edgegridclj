(ns edgegridclj.sign
  (:require [edgegridclj.canonicalize :refer [aka-open-canonicalize]]
            [clj-time.core :as time]
            [clj-time.format :as tfmt]
            [clojure.data.codec.base64 :as b64])
  (:import (java.security MessageDigest)
           (com.google.api.client.http GenericUrl
                                       HttpRequest)))

;; some global config
(def ALGORITHM "EG1-HMAC-SHA256")
(def HMAC_ALG "HmacSHA256")

(defn aka-open-timestamp
  "Timestamp maker. Example timestamp=20130817T02:49:13+0000;"
  []
  (let [timeformat (tfmt/formatter "yyyyMMdd'T'HH:mm:ssZ")]
    (tfmt/unparse timeformat (time/now))))

(defn aka-open-nonce
  "Nonce maker - just java random UUID"
  []
  (java.util.UUID/randomUUID))

(defn aka-open-authdata
  "Build part of the authorization header string from parts"
  [client-token access-token timestamp]
  (str ALGORITHM " "
       "client_token=" client-token ";"
       "access_token=" access-token ";"
       "timestamp="    timestamp    ";"
       "nonce="    (aka-open-nonce) ";"))

(defn aka-open-inner-sign
  "Sign a string via hash-based Message Authentication Code.
   Result is a Base64 encoded String.
     s  = String to sign
   key  = client secret shared with server
   algo = MAC algorithm"
  [s key algo]
  (let [sigkeybytes (javax.crypto.spec.SecretKeySpec. (.getBytes key) algo)
        mac (doto (javax.crypto.Mac/getInstance algo)
              (.init sigkeybytes))
        sigkey (.doFinal mac (.getBytes s))]
    (String. (b64/encode sigkey) "UTF-8")))

(defn aka-open-sign!
  "Primary signing procedure. MAC the canonicalized authdata and request body
  and add this \"signature\" to the authdata by updating the request headers.
   request = HttpRequest to be signed
      cred = user credential: map of clientToken, accessToken and clientSecret"
  [request cred]
  (let [timestamp     (aka-open-timestamp)
        authData      (aka-open-authdata (:clientToken cred)
                                         (:accessToken cred)
                                         timestamp)
        signingKey    (aka-open-inner-sign timestamp (:clientSecret cred) HMAC_ALG)
        requestResult (aka-open-canonicalize request) ;; TBD handle non-retryable case
        stringToSign  (str requestResult authData)
        signature     (aka-open-inner-sign stringToSign signingKey HMAC_ALG)
        authHeader    (str authData "signature=" signature)
        headers       (.getHeaders request)
        _             (.setAuthorization headers authHeader) ; mutate headers!
        signedRequest (.setHeaders request headers)]
    signedRequest))
