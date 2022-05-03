(ns lucene.custom.analyzer-test
  (:require [clojure.test :refer [deftest is testing]]
            [lucene.custom.analyzer :as analyzer])
  (:import (org.apache.lucene.analysis Analyzer)
           (org.apache.lucene.analysis.custom CustomAnalyzer)
           (org.apache.lucene.analysis.standard StandardTokenizerFactory)
           (org.apache.lucene.analysis.core KeywordTokenizerFactory LowerCaseFilterFactory)
           (org.apache.lucene.analysis.charfilter HTMLStripCharFilterFactory)
           (org.apache.lucene.analysis.miscellaneous ASCIIFoldingFilterFactory)))

(deftest analyzer-building
  (testing "tokenizer params handling"
    (let [analyzer (analyzer/create {:tokenizer {"standard" nil}})]
      (is (instance? Analyzer analyzer))
      (is (instance? CustomAnalyzer analyzer))
      (is (= StandardTokenizerFactory
             (class (.getTokenizerFactory ^CustomAnalyzer analyzer)))))

    (let [analyzer (analyzer/create {:tokenizer {"keyword" nil}})]
      (is (= KeywordTokenizerFactory
             (class (.getTokenizerFactory ^CustomAnalyzer analyzer))))))

  (testing "char filter params handling"
    (let [analyzer (analyzer/create {:char-filters [{"htmlStrip" nil}]})]
      (is (= 1 (count (.getCharFilterFactories ^CustomAnalyzer analyzer))))
      (is (= HTMLStripCharFilterFactory
             (class (first (.getCharFilterFactories ^CustomAnalyzer analyzer))))))

    (let [analyzer (analyzer/create {:char-filters [{"htmlStrip" nil} {"htmlStrip" nil}]})]
      (is (= 2 (count (.getCharFilterFactories ^CustomAnalyzer analyzer))))
      (is (= [HTMLStripCharFilterFactory HTMLStripCharFilterFactory]
             (map #(class %) (.getCharFilterFactories ^CustomAnalyzer analyzer))))))

  (testing "token filter params handling"
    (let [analyzer (analyzer/create {:token-filters [{"asciiFolding" nil}]})]
      (is (= 1 (count (.getTokenFilterFactories ^CustomAnalyzer analyzer))))
      (is (= [ASCIIFoldingFilterFactory]
             (map #(class %) (.getTokenFilterFactories ^CustomAnalyzer analyzer)))))

    (let [analyzer (analyzer/create {:token-filters [{"asciiFolding" nil} {"lowercase" nil}]})]
      (is (= 2 (count (.getTokenFilterFactories ^CustomAnalyzer analyzer))))
      (is (= [ASCIIFoldingFilterFactory LowerCaseFilterFactory]
             (map #(class %) (.getTokenFilterFactories ^CustomAnalyzer analyzer))))))

  (testing "offset gap and position increment gap param handling"
    (let [^CustomAnalyzer analyzer (analyzer/create {:offset-gap             12
                                                     :position-increment-gap 3})]
      (is (= 12 (.getOffsetGap analyzer "")))
      (is (= 3 (.getPositionIncrementGap analyzer ""))))))
