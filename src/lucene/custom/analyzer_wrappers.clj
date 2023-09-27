(ns lucene.custom.analyzer-wrappers
  (:require [lucene.custom.analyzer :as analyzer])
  (:import (java.util HashMap Map)
           (org.apache.lucene.analysis Analyzer)
           (org.apache.lucene.analysis.miscellaneous PerFieldAnalyzerWrapper)))

(defn per-field-analyzer-wrapper
  "Creates a PerFieldAnalyzerWrapper.
  TIP: if field->analyzer Map is mutable, mutations done after the creation
  are going to be visible for subsequent analysis.
  Params:
  * default: default Analyzer object,
  * field->analyzer: Map<String, Analyzer>"
  (^Analyzer [^Analyzer default]
   (PerFieldAnalyzerWrapper. default))
  (^Analyzer [^Analyzer default ^Map field->analyzer]
   (PerFieldAnalyzerWrapper. default field->analyzer)))

(defn ->per-field-analyzer-wrapper
  "Constructs a PerFieldAnalyzerWrapper.
  Tip: in case you want a query string to be parsed per field, this can be handy.
  Params:
   * default: a default analyzer configuration
   * field->analyzer: a map from field name to the analyzer configuration"
  [default ^Map field->analyzer]
  (let [default-analyzer (analyzer/create default)]
    (per-field-analyzer-wrapper
      default-analyzer
      (reduce-kv (fn construct-normalize-and-add! [^Map mapping k v]
                   (doto mapping (.put (name k) (analyzer/create v))))
                 (HashMap.)
                 field->analyzer))))

(comment
  (->per-field-analyzer-wrapper {} {}))
