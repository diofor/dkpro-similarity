package org.dkpro.similarity.algorithms.vsm.uima;

import java.io.File;
import java.util.Map;

import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.dkpro.similarity.algorithms.vsm.VectorComparator;
import org.dkpro.similarity.algorithms.vsm.store.CachingVectorReader;
import org.dkpro.similarity.algorithms.vsm.store.EmbeddingVectorReader;
import org.dkpro.similarity.algorithms.vsm.store.EmbeddingVectorReader.EmbeddingFormat;
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
	    protected int cacheSize;
	    
	    public static final String PARAM_EMBEDDING_TYPE = "PARAM_EMBEDDING_TYPE";
	    @ConfigurationParameter(name = PARAM_EMBEDDING_TYPE, mandatory = true, defaultValue="GLOVE")
	    protected EmbeddingType embeddingType;
	    
	    public static final String PARAM_EMBEDDING_FORMAT = "PARAM_EMBEDDING_FORMAT";
	    @ConfigurationParameter(name = PARAM_EMBEDDING_FORMAT, mandatory = true, defaultValue="txt")
	    protected EmbeddingFormat embeddingFormat;
	    
	    public static final String PARAM_FILTER_LOCATION = "PARAM_FILTER_LOCATION";
	    @ConfigurationParameter(name = PARAM_FILTER_LOCATION, mandatory = false)
	    protected String filterLocation;
	    
	    

	    @Override
	    public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
	        throws ResourceInitializationException
	    {
	        if (!super.initialize(aSpecifier, aAdditionalParams)) {
	            return false;
	        }
	        
	        this.mode = TextSimilarityResourceMode.list;
	        
	        measure =	new VectorComparator( 
	        						new CachingVectorReader(
	        								new EmbeddingVectorReader( 
	        										new File(modelLocation),
	        										embeddingType,
	        										embeddingFormat,
	        										new File(filterLocation)
	        								),
	        								cacheSize
	        						)
	        				);
	        
	        return true;
	    }
}
