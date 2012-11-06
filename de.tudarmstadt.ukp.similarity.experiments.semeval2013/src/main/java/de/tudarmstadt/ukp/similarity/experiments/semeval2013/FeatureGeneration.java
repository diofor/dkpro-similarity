package de.tudarmstadt.ukp.similarity.experiments.semeval2013;

import static de.tudarmstadt.ukp.similarity.experiments.semeval2013.Pipeline.DATASET_DIR;
import static de.tudarmstadt.ukp.similarity.experiments.semeval2013.Pipeline.FEATURES_DIR;
import static de.tudarmstadt.ukp.similarity.experiments.semeval2013.Pipeline.UTILS_DIR;
import static org.uimafit.factory.AnalysisEngineFactory.createPrimitive;
import static org.uimafit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.uimafit.factory.CollectionReaderFactory.createCollectionReader;
import static org.uimafit.factory.ExternalResourceFactory.createExternalResourceDescription;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.pipeline.SimplePipeline;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Document;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.string.LongestCommonSubsequenceComparator;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.string.LongestCommonSubsequenceNormComparator;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.string.LongestCommonSubstringComparator;
import de.tudarmstadt.ukp.similarity.dkpro.annotator.SimilarityScorer;
import de.tudarmstadt.ukp.similarity.dkpro.io.CombinationReader;
import de.tudarmstadt.ukp.similarity.dkpro.io.CombinationReader.CombinationStrategy;
import de.tudarmstadt.ukp.similarity.dkpro.io.SemEvalCorpusReader;
import de.tudarmstadt.ukp.similarity.dkpro.resource.SimpleTextSimilarityResource;
import de.tudarmstadt.ukp.similarity.dkpro.resource.lexical.ngrams.CharacterNGramResource;
import de.tudarmstadt.ukp.similarity.dkpro.resource.lexical.ngrams.WordNGramContainmentResource;
import de.tudarmstadt.ukp.similarity.dkpro.resource.lexical.ngrams.WordNGramJaccardResource;
import de.tudarmstadt.ukp.similarity.dkpro.resource.lexical.string.GreedyStringTilingMeasureResource;
import de.tudarmstadt.ukp.similarity.ml.FeatureConfig;
import de.tudarmstadt.ukp.similarity.ml.io.SimilarityScoreWriter;
import de.tudarmstadt.ukp.similarity.experiments.semeval2013.Pipeline.Dataset;
import de.tudarmstadt.ukp.similarity.experiments.semeval2013.Pipeline.Mode;
import de.tudarmstadt.ukp.similarity.experiments.semeval2013.util.CharacterNGramIdfValuesGenerator;
import de.tudarmstadt.ukp.similarity.experiments.semeval2013.util.StopwordFilter;


public class FeatureGeneration
{
	public static void generateFeatures(Dataset dataset, Mode mode)
		throws Exception
	{
		// Define the features
		List<FeatureConfig> configs = new ArrayList<FeatureConfig>();
		
		// Prerequisites
		int[] ngrams_n = new int[] { 2, 3, 4 };
		for (int n : ngrams_n)
			CharacterNGramIdfValuesGenerator.computeIdfScores(mode, dataset, n);
		
		// TODO
		// EXAMPLE GOES HERE
		
		// String features
		configs.add(new FeatureConfig(
				createExternalResourceDescription(
				    	GreedyStringTilingMeasureResource.class,
				    	GreedyStringTilingMeasureResource.PARAM_MIN_MATCH_LENGTH, "3"),
				Document.class.getName(),
				false,
				"string",
				"GreedyStringTiling_3"
				));
		
		configs.add(new FeatureConfig(
				createExternalResourceDescription(
				    	SimpleTextSimilarityResource.class,
				    	SimpleTextSimilarityResource.PARAM_MODE, "text",
				    	SimpleTextSimilarityResource.PARAM_TEXT_SIMILARITY_MEASURE, LongestCommonSubsequenceComparator.class.getName()),
				Document.class.getName(),
				false,
				"string",
				"LongestCommonSubsequenceComparator"
				));
		
		configs.add(new FeatureConfig(
				createExternalResourceDescription(
				    	SimpleTextSimilarityResource.class,
				    	SimpleTextSimilarityResource.PARAM_MODE, "text",
				    	SimpleTextSimilarityResource.PARAM_TEXT_SIMILARITY_MEASURE, LongestCommonSubsequenceNormComparator.class.getName()),
				Document.class.getName(),
				false,
				"string",
				"LongestCommonSubsequenceNormComparator"
				));
		
		configs.add(new FeatureConfig(
				createExternalResourceDescription(
				    	SimpleTextSimilarityResource.class,
				    	SimpleTextSimilarityResource.PARAM_MODE, "text",
				    	SimpleTextSimilarityResource.PARAM_TEXT_SIMILARITY_MEASURE, LongestCommonSubstringComparator.class.getName()),
				Document.class.getName(),    	
				false,
				"string",
				"LongestCommonSubstringComparator"
				));
		
		ngrams_n = new int[] { 2, 3, 4 };
		for (int n : ngrams_n)
		{
			configs.add(new FeatureConfig(
					createExternalResourceDescription(
							CharacterNGramResource.class,
							CharacterNGramResource.PARAM_N, new Integer(n).toString(),
							CharacterNGramResource.PARAM_IDF_VALUES_FILE, UTILS_DIR + "/character-ngrams-idf/" + mode.toString().toLowerCase() + "/" + n + "/" + dataset.toString() + ".txt"),
					Document.class.getName(),
					false,
					"n-grams",
					"CharacterNGramMeasure_" + n
					));
		}
		
		ngrams_n = new int[] { 1, 2 };
		for (int n : ngrams_n)
		{
			configs.add(new FeatureConfig(
					createExternalResourceDescription(
					    	WordNGramContainmentResource.class,
					    	WordNGramContainmentResource.PARAM_N, new Integer(n).toString()),
					Token.class.getName(),
					true,
					"n-grams",
					"WordNGramContainmentMeasure_" + n + "_stopword-filtered"
					));
		}
		
		ngrams_n = new int[] { 1, 3, 4 };
		for (int n : ngrams_n)
		{
			configs.add(new FeatureConfig(
					createExternalResourceDescription(
					    	WordNGramJaccardResource.class,
					    	WordNGramJaccardResource.PARAM_N, new Integer(n).toString()),
					Token.class.getName(),
					false,
					"n-grams",
					"WordNGramJaccardMeasure_" + n
					));			
		}
		
		ngrams_n = new int[] { 2, 4 };
		for (int n : ngrams_n)
		{
			configs.add(new FeatureConfig(
					createExternalResourceDescription(
					    	WordNGramJaccardResource.class,
					    	WordNGramJaccardResource.PARAM_N, new Integer(n).toString()),
					Token.class.getName(),
					true,
					"n-grams",
					"WordNGramJaccardMeasure_" + n + "_stopword-filtered"
					));			
		}
		
		// ESA
//		configs.add(new FeatureConfig(
//				createExternalResourceDescription(
//				    	VectorIndexSourceRelatednessResource.class,
//				    	VectorIndexSourceRelatednessResource.PARAM_MODEL_LOCATION, "/ESA/VectorIndexes/wp_eng_lem_nc_c"),
//				Lemma.class.getName() + "/value",
//				false,
//				"esa",
//				"ESA_WP"
//				));
//		
//		configs.add(new FeatureConfig(
//				createExternalResourceDescription(
//				    	VectorIndexSourceRelatednessResource.class,
//				    	VectorIndexSourceRelatednessResource.PARAM_MODEL_LOCATION, "/ESA/VectorIndexes/wiktionary_en"),
//				Lemma.class.getName() + "/value",
//				false,
//				"esa",
//				"ESA_WK"
//				));

		
		// Run the pipeline		
		for (FeatureConfig config : configs)
		{			
			System.out.println(config.getMeasureName());
			
			File outputFile = new File(FEATURES_DIR + "/" + mode.toString().toLowerCase() + "/" + dataset.toString() + "/" + config.getTargetPath() + "/" + config.getMeasureName() + ".txt");
			
			if (outputFile.exists())
			{
				System.out.println(" - skipped, feature already generated");
			} 
			else
			{			
				CollectionReader reader = createCollectionReader(SemEvalCorpusReader.class,
						SemEvalCorpusReader.PARAM_INPUT_FILE, DATASET_DIR + "/" + mode.toString().toLowerCase() + "/STS.input." + dataset.toString() + ".txt",
						SemEvalCorpusReader.PARAM_COMBINATION_STRATEGY, CombinationStrategy.SAME_ROW_ONLY.toString());
		
				AnalysisEngineDescription seg = createPrimitiveDescription(BreakIteratorSegmenter.class);
				
				AggregateBuilder builder = new AggregateBuilder();
				builder.add(seg, CombinationReader.INITIAL_VIEW, CombinationReader.VIEW_1);
				builder.add(seg, CombinationReader.INITIAL_VIEW, CombinationReader.VIEW_2);
				AnalysisEngine aggr_seg = builder.createAggregate();
				
//				AnalysisEngineDescription tt = createPrimitiveDescription(
//						TreeTaggerPosLemmaTT4J.class,
//						TreeTaggerPosLemmaTT4J.PARAM_LANGUAGE_CODE, "en");		
//				builder = new AggregateBuilder();
//				builder.add(tt, CombinationReader.INITIAL_VIEW, CombinationReader.VIEW_1);
//				builder.add(tt, CombinationReader.INITIAL_VIEW, CombinationReader.VIEW_2);
//				AnalysisEngine aggr_tt = builder.createAggregate();
				
				AnalysisEngineDescription stopw = createPrimitiveDescription(
						StopwordFilter.class,
						StopwordFilter.PARAM_STOPWORD_LIST, "classpath:/stopwords/stopwords_english_punctuation.txt",
						StopwordFilter.PARAM_ANNOTATION_TYPE_NAME, Lemma.class.getName(),
						StopwordFilter.PARAM_STRING_REPRESENTATION_METHOD_NAME, "getValue");
				builder = new AggregateBuilder();
				builder.add(stopw, CombinationReader.INITIAL_VIEW, CombinationReader.VIEW_1);
				builder.add(stopw, CombinationReader.INITIAL_VIEW, CombinationReader.VIEW_2);
				AnalysisEngine aggr_stopw = builder.createAggregate();
		
				AnalysisEngine scorer = createPrimitive(SimilarityScorer.class,
				    SimilarityScorer.PARAM_NAME_VIEW_1, CombinationReader.VIEW_1,
				    SimilarityScorer.PARAM_NAME_VIEW_2, CombinationReader.VIEW_2,
				    SimilarityScorer.PARAM_SEGMENT_FEATURE_PATH, config.getSegmentFeaturePath(),
				    SimilarityScorer.PARAM_TEXT_SIMILARITY_RESOURCE, config.getResource()
				    );
				
				AnalysisEngine writer = createPrimitive(SimilarityScoreWriter.class,
					SimilarityScoreWriter.PARAM_OUTPUT_FILE, outputFile.getAbsolutePath(),
					SimilarityScoreWriter.PARAM_OUTPUT_SCORES_ONLY, true);
		
				if (config.filterStopwords())
					SimplePipeline.runPipeline(reader, aggr_seg, aggr_stopw, scorer, writer);
				else
					SimplePipeline.runPipeline(reader, aggr_seg, scorer, writer);
				
				System.out.println(" - done");
			}
		}
		
		System.out.println("Successful.");
	}
	
	@SuppressWarnings("unchecked")
	public static void combineFeatureSets(Mode mode, Dataset target, Dataset... sources)
			throws IOException
	{	
		String outputFolderName = target.toString();
		
		System.out.println("Combining feature sets");
		
		// Check if target directory exists. If so, delete it.
		File targetDir = new File(FEATURES_DIR + "/" + mode.toString().toLowerCase() + "/" + target.toString());
		if (targetDir.exists())
		{
			System.out.println(" - cleaned target directory");
			FileUtils.deleteDirectory(targetDir);
		}
		
		String featurePathOfFirstSet = FEATURES_DIR + "/" + mode.toString().toLowerCase() + "/" + sources[0].toString();
		
		Collection<File> features = FileUtils.listFiles(new File(featurePathOfFirstSet), new String[] { "txt" }, true);
		
		for (File feature : features)
		{
			if (!feature.isDirectory())
			{
				// Check that feature exists for all 
				boolean shared = true;
				
				for (int i = 1; i < sources.length; i++)
				{
					if (!new File(feature.getAbsolutePath().replace(sources[0].toString(), sources[i].toString())).exists())
						shared = false;
				}
				
				if (shared)
				{					
					System.out.println(" - processing " + feature.getName());
					
					String concat = FileUtils.readFileToString(feature);
					
					for (int i = 1; i < sources.length; i++)
					{
						File nextFile = new File(feature.getAbsolutePath().replaceAll(sources[0].toString(), sources[i].toString()));
						
						concat += FileUtils.readFileToString(nextFile);
					}
					
					File outputFile = new File(feature.getAbsolutePath().replace(sources[0].toString(), outputFolderName));
					
					FileUtils.writeStringToFile(outputFile, concat);
				}
			}
		}
		
		System.out.println(" - done");
	}
}