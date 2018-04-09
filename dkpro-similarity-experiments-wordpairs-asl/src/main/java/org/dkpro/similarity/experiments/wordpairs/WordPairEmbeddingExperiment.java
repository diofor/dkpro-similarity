package org.dkpro.similarity.experiments.wordpairs;


import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createExternalResourceDescription;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.dkpro.similarity.algorithms.vsm.store.EmbeddingVectorReader.EmbeddingFormat;
import org.dkpro.similarity.algorithms.vsm.store.EmbeddingVectorReader.EmbeddingType;
import org.dkpro.similarity.algorithms.vsm.uima.EmbeddingVectorSourceRelatednessResource;
import org.dkpro.similarity.experiments.wordpairs.io.SemanticRelatednessResultWriter;
import org.dkpro.similarity.experiments.wordpairs.io.WordPairReader;

import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase;

public class WordPairEmbeddingExperiment {
	private static String sourcePath;
	private static String filterFilePath;
	
	public static void main(String[] args) 
			throws Exception 
	{
		sourcePath = "classpath:/datasets/wordpairs/en/";
		filterFilePath = "classpath:/datasets/filterfile.txt";
		
		createSetPipeline();
		analysePipeline();
	}
	
	
	private static void createSetPipeline() 
			throws UIMAException, IOException
	{
		CollectionReader reader = createReader(WordPairReader.class, 
				WordPairReader.PARAM_SOURCE_LOCATION,  sourcePath, 
				WordPairReader.PARAM_PATTERNS,
				new String[] { ResourceCollectionReaderBase.INCLUDE_PREFIX + "*.txt" }
		);
		
		AnalysisEngineDescription createSet = createEngineDescription(BuildSetAnnotator.class, 
				BuildSetAnnotator.PARAM_MODEL_LOCATION, filterFilePath
		);
		
		SimplePipeline.runPipeline(reader, createSet);
	}
	
	
	private static void analysePipeline() 
			throws UIMAException, IOException
	{
		CollectionReader reader = createReader(
				WordPairReader.class, 
				WordPairReader.PARAM_SOURCE_LOCATION,  sourcePath, 
				WordPairReader.PARAM_PATTERNS,	
				new String[] { ResourceCollectionReaderBase.INCLUDE_PREFIX + "*.txt" }
		);
		
		
		//	------ Word2Vec ------
		
		/*
		 * 	https://drive.google.com/file/d/0B7XkCwpI5KDYNlNUTTlSS21pQmM/
		 * 	1,65 GB
		 */
		AnalysisEngineDescription word2vecGoogleNews = createEngineDescription(
				ResourceBasedAnnotator.class,
				ResourceBasedAnnotator.SR_RESOURCE,	createExternalResourceDescription(
						EmbeddingVectorSourceRelatednessResource.class, 
						EmbeddingVectorSourceRelatednessResource.PARAM_MODEL_LOCATION,  "GoogleNews-vectors-negative300.bin.gz",
						EmbeddingVectorSourceRelatednessResource.PARAM_EMBEDDING_TYPE, EmbeddingType.Word2Vec.toString(),	
						EmbeddingVectorSourceRelatednessResource.PARAM_EMBEDDING_FORMAT, EmbeddingFormat.bin.toString(),
						EmbeddingVectorSourceRelatednessResource.PARAM_FILTER_LOCATION, filterFilePath
				)
		);
		
		
		/*
		 * 	http://u.cs.biu.ac.il/%7Eyogo/data/syntemb/deps.words.bz2
		 * 	321 MB
		 */
		AnalysisEngineDescription word2vecWikipediaDependency = createEngineDescription(
				ResourceBasedAnnotator.class,
				ResourceBasedAnnotator.SR_RESOURCE,	createExternalResourceDescription(
						EmbeddingVectorSourceRelatednessResource.class, 
						EmbeddingVectorSourceRelatednessResource.PARAM_FILTER_LOCATION, filterFilePath,
						EmbeddingVectorSourceRelatednessResource.PARAM_MODEL_LOCATION,  "deps.words.bz2",
						EmbeddingVectorSourceRelatednessResource.PARAM_EMBEDDING_TYPE, EmbeddingType.GLOVE.toString(),	
						EmbeddingVectorSourceRelatednessResource.PARAM_EMBEDDING_FORMAT, EmbeddingFormat.txt.toString()
				)
		);
		
		//	------ GloVe ------
		
		/*
		 * 	http://nlp.stanford.edu/data/glove.6B.zip
		 * 	863 MB
		 */
		AnalysisEngineDescription gloveWikipediaGigaword = createEngineDescription(
				ResourceBasedAnnotator.class,
				ResourceBasedAnnotator.SR_RESOURCE,	createExternalResourceDescription(
						EmbeddingVectorSourceRelatednessResource.class, 
						EmbeddingVectorSourceRelatednessResource.PARAM_FILTER_LOCATION, filterFilePath,
						EmbeddingVectorSourceRelatednessResource.PARAM_MODEL_LOCATION, "glove.6B.zip",
						EmbeddingVectorSourceRelatednessResource.PARAM_EMBEDDING_TYPE, EmbeddingType.GLOVE.toString(),
						EmbeddingVectorSourceRelatednessResource.PARAM_EMBEDDING_FORMAT, EmbeddingFormat.txt.toString(),
						EmbeddingVectorSourceRelatednessResource.PARAM_CONTAINER_FILE, "glove.6B.50d.txt"
//						EmbeddingVectorSourceRelatednessResource.PARAM_CONTAINER_FILE, "glove.6B.100d.txt"
//						EmbeddingVectorSourceRelatednessResource.PARAM_CONTAINER_FILE, "glove.6B.200d.txt"
//						EmbeddingVectorSourceRelatednessResource.PARAM_CONTAINER_FILE, "glove.6B.300d.txt"
				)
		);
		
		
		/*
		 * 	http://nlp.stanford.edu/data/glove.840B.300d.zip
		 * 	2,18 GB
		 */
		AnalysisEngineDescription gloveCommonCrawl840B = createEngineDescription(
				ResourceBasedAnnotator.class,
				ResourceBasedAnnotator.SR_RESOURCE,	createExternalResourceDescription(
						EmbeddingVectorSourceRelatednessResource.class,
						EmbeddingVectorSourceRelatednessResource.PARAM_MODEL_LOCATION, "glove.840B.300d.zip",  
						EmbeddingVectorSourceRelatednessResource.PARAM_EMBEDDING_TYPE, EmbeddingType.GLOVE.toString(),
						EmbeddingVectorSourceRelatednessResource.PARAM_EMBEDDING_FORMAT, EmbeddingFormat.txt.toString(),
						EmbeddingVectorSourceRelatednessResource.PARAM_FILTER_LOCATION, filterFilePath
				)
		);
		
		/*
		 * 	http://nlp.stanford.edu/data/glove.42B.300d.zip
		 * 	1,88 GB
		 */
		AnalysisEngineDescription gloveCommonCrawl42B = createEngineDescription(
				ResourceBasedAnnotator.class,
				ResourceBasedAnnotator.SR_RESOURCE,	createExternalResourceDescription(
						EmbeddingVectorSourceRelatednessResource.class,
						EmbeddingVectorSourceRelatednessResource.PARAM_MODEL_LOCATION, "glove.42B.300d.zip",  
						EmbeddingVectorSourceRelatednessResource.PARAM_EMBEDDING_TYPE, EmbeddingType.GLOVE.toString(),
						EmbeddingVectorSourceRelatednessResource.PARAM_EMBEDDING_FORMAT, EmbeddingFormat.txt.toString(),
						EmbeddingVectorSourceRelatednessResource.PARAM_FILTER_LOCATION, filterFilePath
				)
		);
		
		/*
		 * 	http://www-nlp.stanford.edu/data/glove.twitter.27B.zip
		 * 	1,52 GB
		 */
		AnalysisEngineDescription gloveTwitter2B = createEngineDescription(
				ResourceBasedAnnotator.class,
				ResourceBasedAnnotator.SR_RESOURCE,	createExternalResourceDescription(
						EmbeddingVectorSourceRelatednessResource.class, 
						EmbeddingVectorSourceRelatednessResource.PARAM_FILTER_LOCATION, filterFilePath,
						EmbeddingVectorSourceRelatednessResource.PARAM_MODEL_LOCATION, "glove.twitter.27B.zip",
						EmbeddingVectorSourceRelatednessResource.PARAM_EMBEDDING_TYPE, EmbeddingType.GLOVE.toString(),
						EmbeddingVectorSourceRelatednessResource.PARAM_EMBEDDING_FORMAT, EmbeddingFormat.txt.toString(),
						EmbeddingVectorSourceRelatednessResource.PARAM_CONTAINER_FILE, "glove.twitter.27B.25d.txt"
//						EmbeddingVectorSourceRelatednessResource.PARAM_CONTAINER_FILE, "glove.twitter.27B.50d.txt"
//						EmbeddingVectorSourceRelatednessResource.PARAM_CONTAINER_FILE, "glove.twitter.27B.100d.txt"
//						EmbeddingVectorSourceRelatednessResource.PARAM_CONTAINER_FILE, "glove.twitter.27B.200d.txt"
				)
		);
		
		//	------ FastText ------
		
		/*
		 * 	https://s3-us-west-1.amazonaws.com/fasttext-vectors/crawl-300d-2M.vec.zip
		 * 	1,52 GB
		 */
		AnalysisEngineDescription fastTextCommonCrawl = createEngineDescription(
				ResourceBasedAnnotator.class,
				ResourceBasedAnnotator.SR_RESOURCE,	createExternalResourceDescription(
						EmbeddingVectorSourceRelatednessResource.class, 
						EmbeddingVectorSourceRelatednessResource.PARAM_FILTER_LOCATION, filterFilePath,
						EmbeddingVectorSourceRelatednessResource.PARAM_MODEL_LOCATION, "crawl-300d-2M.vec.zip",
						EmbeddingVectorSourceRelatednessResource.PARAM_EMBEDDING_TYPE, EmbeddingType.FastText.toString(),
						EmbeddingVectorSourceRelatednessResource.PARAM_EMBEDDING_FORMAT, EmbeddingFormat.txt.toString()
				)
		);
		
		/*
		 * 	https://s3-us-west-1.amazonaws.com/fasttext-vectors/wiki-news-300d-1M.vec.zip
		 * 	683 MB
		 */
		AnalysisEngineDescription fastTextWikiNews = createEngineDescription(
				ResourceBasedAnnotator.class,
				ResourceBasedAnnotator.SR_RESOURCE,	createExternalResourceDescription(
						EmbeddingVectorSourceRelatednessResource.class, 
						EmbeddingVectorSourceRelatednessResource.PARAM_FILTER_LOCATION, filterFilePath,
						EmbeddingVectorSourceRelatednessResource.PARAM_MODEL_LOCATION, "wiki-news-300d-1M.vec.zip",
						EmbeddingVectorSourceRelatednessResource.PARAM_EMBEDDING_TYPE, EmbeddingType.FastText.toString(),
						EmbeddingVectorSourceRelatednessResource.PARAM_EMBEDDING_FORMAT, EmbeddingFormat.txt.toString()
				)
		);
		
		
		AnalysisEngineDescription writer = createEngineDescription(
				SemanticRelatednessResultWriter.class,
				SemanticRelatednessResultWriter.PARAM_SHOW_DETAILS, false
		);
		
		SimplePipeline.runPipeline(
				reader,
				fastTextWikiNews,
				fastTextCommonCrawl,
				word2vecWikipediaDependency,
				word2vecGoogleNews,
				gloveWikipediaGigaword,
				gloveTwitter2B,
				gloveCommonCrawl42B,
				gloveCommonCrawl840B,
				writer
		);
	}
}