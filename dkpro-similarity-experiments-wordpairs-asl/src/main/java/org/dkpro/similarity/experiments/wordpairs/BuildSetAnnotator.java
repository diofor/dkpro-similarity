package org.dkpro.similarity.experiments.wordpairs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.similarity.type.WordPair;



public class BuildSetAnnotator
	extends JCasAnnotator_ImplBase
{
	public static final String PARAM_MODEL_LOCATION = "Model Location";
    @ConfigurationParameter(name = PARAM_MODEL_LOCATION, mandatory = true)
    protected String modelLocation;
	
	
	private Set<String> containedWords;
	
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		containedWords = new HashSet<String>();
	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		String term1 = null;
        String term2 = null;

        for (WordPair wp : JCasUtil.select(jcas, WordPair.class)) {
            term1 = wp.getWord1();
            term2 = wp.getWord2();

            // are the terms initialized?
            if (term1 != null) containedWords.add(term1);
            if (term2 != null) containedWords.add(term2); 
        }
	}
	
	
	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException{
		super.collectionProcessComplete();
		//Make sure there exists a clean file f
		File f = new File(modelLocation);
		try {
			if (f.exists())	f.delete();
			f.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Write out the Set
		try ( BufferedWriter br = new BufferedWriter( new FileWriter(f))){
			for (String key : containedWords)
			{
				br.write(key);
				br.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
