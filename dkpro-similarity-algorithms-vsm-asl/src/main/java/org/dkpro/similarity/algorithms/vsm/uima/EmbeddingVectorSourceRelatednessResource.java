package org.dkpro.similarity.algorithms.vsm.uima;

import java.io.File;
import java.util.Map;

import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.dkpro.similarity.algorithms.vsm.VectorComparator;
import org.dkpro.similarity.algorithms.vsm.store.CachingVectorReader;
import org.dkpro.similarity.algorithms.vsm.store.EmbeddingVectorReader;
import org.dkpro.similarity.algorithms.vsm.store.EmbeddingVectorReader.EmbeddingType;
import org.dkpro.similarity.uima.resource.TextSimilarityResourceBase;

import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;

public final class EmbeddingVectorSourceRelatednessResource
	extends TextSimilarityResourceBase 
{
	  	public static final String PARAM_MODEL_LOCATION = ComponentParameters.PARAM_MODEL_LOCATION;
	    @ConfigurationParameter(name = PARAM_MODEL_LOCATION, mandatory = true)
	    protected String modelLocation;
	    
	    public static final String PARAM_CACHE_SIZE = "CacheSize";
	    @ConfigurationParameter(name = PARAM_CACHE_SIZE, mandatory = true, defaultValue="100")
	    protected String cacheSize;
	    
	    public static final String PARAM_EMBEDDING_TYPE = "PARAM_EMBEDDING_TYPE";
	    @ConfigurationParameter(name = PARAM_EMBEDDING_TYPE, mandatory = true, defaultValue="GLOVE")
	    protected String embeddingtype_asString;
	    
	    protected EmbeddingType embeddingType;

	    @Override
	    public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
	        throws ResourceInitializationException
	    {
	        if (!super.initialize(aSpecifier, aAdditionalParams)) {
	            return false;
	        }
	        
	        this.mode = TextSimilarityResourceMode.list;
	        
	        for (EmbeddingType type : EmbeddingType.values())
	    		{
	    			if (type.toString().equals(embeddingtype_asString))
	    			{
	    				embeddingType = type;
	    			}
	    		}
	       
	        measure = new VectorComparator(new CachingVectorReader(
	                new EmbeddingVectorReader(new File(modelLocation), embeddingType),
	                Integer.parseInt(cacheSize)
	        ));
	        
	        return true;
	    }
}
