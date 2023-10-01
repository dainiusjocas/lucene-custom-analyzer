(ns lucene.custom.analyzer-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [lucene.custom.text-analysis :as analysis]
            [lucene.custom.analyzer :as analyzer])
  (:import (java.util ArrayList HashMap)
           (org.apache.lucene.analysis Analyzer)
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
             (class (.getTokenizerFactory ^CustomAnalyzer analyzer)))))

    (let [analyzer (analyzer/create {:tokenizer "keyword"})]
      (is (= KeywordTokenizerFactory
             (class (.getTokenizerFactory ^CustomAnalyzer analyzer)))))

    (let [analyzer (analyzer/create {:tokenizer :keyword})]
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
             (map #(class %) (.getCharFilterFactories ^CustomAnalyzer analyzer)))))

    (let [analyzer (analyzer/create {:char-filters [{"htmlStrip" nil} "htmlStrip"]})]
      (is (= 2 (count (.getCharFilterFactories ^CustomAnalyzer analyzer))))
      (is (= [HTMLStripCharFilterFactory HTMLStripCharFilterFactory]
             (map #(class %) (.getCharFilterFactories ^CustomAnalyzer analyzer)))))
    (let [analyzer (analyzer/create {:char-filters [:htmlStrip "htmlStrip"]})]
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
             (map #(class %) (.getTokenFilterFactories ^CustomAnalyzer analyzer)))))

    (let [analyzer (analyzer/create {:token-filters [{"asciiFolding" nil} "lowercase"]})]
      (is (= 2 (count (.getTokenFilterFactories ^CustomAnalyzer analyzer))))
      (is (= [ASCIIFoldingFilterFactory LowerCaseFilterFactory]
             (map #(class %) (.getTokenFilterFactories ^CustomAnalyzer analyzer)))))

    (let [analyzer (analyzer/create {:token-filters [{"asciiFolding" nil} :lowercase]})]
      (is (= 2 (count (.getTokenFilterFactories ^CustomAnalyzer analyzer))))
      (is (= [ASCIIFoldingFilterFactory LowerCaseFilterFactory]
             (map #(class %) (.getTokenFilterFactories ^CustomAnalyzer analyzer))))))

  (testing "offset gap and position increment gap param handling"
    (let [^CustomAnalyzer analyzer (analyzer/create {:offset-gap             12
                                                     :position-increment-gap 3})]
      (is (= 12 (.getOffsetGap analyzer "")))
      (is (= 3 (.getPositionIncrementGap analyzer "")))))

  (testing "config with raw array list"
    (let [analyzer (analyzer/create (doto (HashMap.)
                                      (.put :token-filters (doto (ArrayList.)
                                                             (.add :reverseString)))))]
      (is (= ["oof" "rab" "zab"]
             (analysis/text->token-strings "foo bar baz" analyzer)))))

  (testing "config-dir variants: filesystem and resources"
    (let [file "test/resources/stopwords.txt"
          file-in-resources "stopwords.txt"
          ^Analyzer analyzer-fs
          (analyzer/create {:config-dir    "."
                            :token-filters [{:stop {:words file}}]})
          ^Analyzer analyzer-classpath
          (analyzer/create {:classpath-resources true
                            :token-filters       [{:stop {:words file-in-resources}}]})]
      (is (= ["baz"] (analysis/text->token-strings "foo bar baz" analyzer-fs)))
      (is (= "foo\nbar\n" (slurp file)))
      (is (= "foo\nbar\n" (slurp (io/resource file-in-resources))))
      (is (= ["baz"] (analysis/text->token-strings "foo bar baz" analyzer-classpath)))))

  (testing "conditional token filter"
    (let [^Analyzer analyzer (analyzer/create {:token-filters
                                               [{:conditional {:pattern               "f.*"
                                                               :wrappedFilters        "edgeNGram,uppercase"
                                                               :edgeNGram.minGramSize 1
                                                               :edgeNGram.maxGramSize 5}}
                                                :reverseString]})]
      (is (= ["F" "OF" "OOF" "rab" "zab"] (analysis/text->token-strings "foo bar baz" analyzer))))))
