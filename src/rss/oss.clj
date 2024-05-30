;;
;; Support configuration retrieval from Oracle Object Storage Service (OSS).
;; This requires an OCI tenancy that has been configured with a bucket,
;; object, and policy to support configuration pulls from it.
;;
(ns rss.oss
  (:import (java.io IOException)
           (com.oracle.bmc ConfigFileReader)
           (com.oracle.bmc.auth ConfigFileAuthenticationDetailsProvider
                                InstancePrincipalsAuthenticationDetailsProvider
                                ResourcePrincipalAuthenticationDetailsProvider
                                )
           (com.oracle.bmc.objectstorage ObjectStorageClient)
           (com.oracle.bmc.objectstorage.requests GetObjectRequest)))

(defn make-client
  "Creates an OSS client using resource principal, instance principal, or
  file-based authentication details providers."
  []
  (try
    (cond
      ;; Resource Principals
      (System/getenv rss.constants/resource-principal-key)
      (.build (ObjectStorageClient/builder)
              (.build (ResourcePrincipalAuthenticationDetailsProvider/builder)))
      ;; Instance Principals
      (System/getenv rss.constants/instance-principal-key)
      (.build (ObjectStorageClient/builder)
              (.build (InstancePrincipalsAuthenticationDetailsProvider/builder)))
      ;; Local config file
      true
      (.build (ObjectStorageClient/builder)
              (ConfigFileAuthenticationDetailsProvider.
                (ConfigFileReader/parseDefault))))
    (catch IOException e
      (println (str "Unable to create client for Object Storage Service: "
                    (.getMessage e))))))

(defn get-stream-for
  "Returns an InputStream for an object in Object Storage Service located in
  the given namespace, bucket, and object tuple from the supplied collection."
  [oss-location]
  (let [client (make-client)
        request (-> (GetObjectRequest/builder)
                    (.namespaceName (nth oss-location 0))
                    (.bucketName (nth oss-location 1))
                    (.objectName (nth oss-location 2))
                    (.build))
        response (.getObject client request)
        stream (.getInputStream response)]
    (.close client)
    stream))
