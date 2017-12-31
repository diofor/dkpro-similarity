package org.dkpro.similarity.algorithms.vsm.store;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.gzip.GzipUtils;
import org.dkpro.similarity.algorithms.api.SimilarityException;
import org.dkpro.similarity.algorithms.vsm.store.vectorindex.VectorIndexContract;

import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.sparse.SparseVector;

/**
 * 
 * @autor Jonas Müller
 */
public class EmbeddingVectorReader 
	extends VectorReader 
	implements VectorIndexContract 
{

	private static final int MAX_SIZE = 50;
	private Map<String, double[]> embeddings;
	private final File embeddingsFile; 
	private final File filterFile;
	private String lineSeperatorSequence; //TODO: will be used later
	private String valueSeperatorSequence;
	private EmbeddingType embeddingType;
	private boolean ignoreRelevantWordsList;
	
	public enum EmbeddingType{
		GLOVE, Word2Vec
	}
	
	public EmbeddingVectorReader(File embeddingFile, EmbeddingType typeOfEmbedding, File filterFile) {
		this.embeddingsFile = embeddingFile;
		this.filterFile = filterFile.exists() ?  filterFile : null;
		ignoreRelevantWordsList = (this.filterFile == null); 
		switch(typeOfEmbedding)
		{
			case GLOVE:		lineSeperatorSequence = "\\n";
							valueSeperatorSequence = " ";
							break;
			case Word2Vec:	lineSeperatorSequence = "\\n";
							valueSeperatorSequence = " ";
							//TODO
							break;	 
			default: 		//TODO
		}
		
		embeddingType = typeOfEmbedding;
	}
	
	
	
	@Override
	public Vector getVector(String term) throws SimilarityException {
		if (!getEmbeddings().containsKey(term)) return null;
		
		int vectorSize = getEmbeddings().get(term).length;
		int[] indices = IntStream.range(0, vectorSize).toArray();
		return new SparseVector(vectorSize, indices, getEmbeddings().get(term));
	}

	@Override
	public int getConceptCount() throws SimilarityException {
		return getEmbeddings().size();
	}

	@Override
	public String getId() {
		return embeddingsFile.getAbsolutePath();
	}

	@Override
	public void close() {
		if (embeddings != null) {
			embeddings = null;
		}
	}
	
	private Map<String, double[]> getEmbeddings() throws SimilarityException
	{
		if (embeddings == null)
		{
			embeddings = new HashMap<String, double[]>();
			switch (embeddingType)
			{
				case GLOVE: 		loadGlossEmbedding(); break;
				case Word2Vec:	loadWord2VecEmbedding(); break;
				default: 		System.err.println("Undefinded Type of Embedding set.") ;break;
				
			}
			System.out.printf("Size of embedding is %d. (only the loaded part is considered)%n", embeddings.size() );
		}
		
		return embeddings;
	}
	
	private void loadWord2VecEmbedding() throws SimilarityException{
        
        boolean linebreaks = false; //TODO
        int words, vecSize;
        try (BufferedInputStream bis = new BufferedInputStream(
                GzipUtils.isCompressedFilename(embeddingsFile.getName())
                        ? new GZIPInputStream(new FileInputStream(embeddingsFile))
                        : new FileInputStream(embeddingsFile));
//        try (BufferedInputStream bis = new BufferedInputStream(
//        			new CompressorStreamFactory().createCompressorInputStream(
//        					new FileInputStream(embeddingsFile)));
                DataInputStream dis = new DataInputStream(bis)) 
        {
        		Set<String> relevantWords = loadSetOfRelevantWords();
            words = Integer.parseInt(readString(dis));
            vecSize = Integer.parseInt(readString(dis));
            String word;
            for (int i = 0; i < words; i++) {

                word = readString(dis);
                double[] vector = new double[vecSize];

                for (int j = 0; j < vecSize; j++) {
                    vector[j] = readFloat(dis);
                }
                
                if (relevantWords.contains(word) || ignoreRelevantWordsList)
                		embeddings.put(word, vector);


                if (linebreaks) {
                    dis.readByte(); // line break
                }
            }
        }
        catch (IOException e) {
			throw new SimilarityException(e);
		} 
	}

	private void loadGlossEmbedding() throws SimilarityException
	{
		String line;
		String[] partsOfLine;
		String term;
		double[] values;
		try (FileInputStream fis = new FileInputStream(embeddingsFile);
			    BufferedInputStream bis = new BufferedInputStream(fis);
			    CompressorInputStream input = new CompressorStreamFactory().createCompressorInputStream(bis);
				BufferedReader in = new BufferedReader(new InputStreamReader( input ));)
		{
			Set<String> relevantWords = loadSetOfRelevantWords();
			while((line  = in.readLine()) != null)
			{
				partsOfLine = line.split(valueSeperatorSequence);
				term = partsOfLine[0];
				if (relevantWords.contains(term) || ignoreRelevantWordsList)
				{
					values = new double[partsOfLine.length-1];
					for (int i = 1; i < partsOfLine.length; i++) {
						values[i-1] = Double.valueOf(partsOfLine[i]);
					}
					embeddings.put(term, values);
				}
				partsOfLine = null;
			}
		}
		catch (IOException e) {
			throw new SimilarityException(e);
		} catch (CompressorException e) {
			throw new SimilarityException(e);
		}
	}
	
	
	
    /**
     * Read a float from a data input stream Credit to:
     * https://github.com/NLPchina/Word2VEC_java/blob/master/src/com/ansj/vec/Word2VEC.java
     *
     * @param is
     * @return
     * @throws IOException
     */
    private float readFloat(InputStream is)
        throws IOException
    {
        byte[] bytes = new byte[4];
        is.read(bytes);
        return getFloat(bytes);
    }

    /**
     * Read a string from a data input stream Credit to:
     * https://github.com/NLPchina/Word2VEC_java/blob/master/src/com/ansj/vec/Word2VEC.java
     *
     * @param b
     * @return
     */
    private float getFloat(byte[] b)
    {
        int accum = 0;
        accum = accum | (b[0] & 0xff) << 0;
        accum = accum | (b[1] & 0xff) << 8;
        accum = accum | (b[2] & 0xff) << 16;
        accum = accum | (b[3] & 0xff) << 24;
        return Float.intBitsToFloat(accum);
    }

    /**
     * Read a string from a data input stream Credit to:
     * https://github.com/NLPchina/Word2VEC_java/blob/master/src/com/ansj/vec/Word2VEC.java
     *
     * @param dis
     * @return
     * @throws IOException
     */
    private String readString(DataInputStream dis)
        throws IOException
    {
        byte[] bytes = new byte[MAX_SIZE];
        byte b = dis.readByte();
        int i = -1;
        StringBuilder sb = new StringBuilder();
        /*
         * ASCII 32 is SPACE
         * ASCII 10 is LF which means LineFeed
         */
        while (b != 32 && b != 10) {
            i++;
            bytes[i] = b;
            b = dis.readByte();
            if (i == (MAX_SIZE-1)) {
                sb.append(new String(bytes));
                i = -1;
                bytes = new byte[MAX_SIZE];
            }
        }
        sb.append(new String(bytes, 0, i + 1));
        return sb.toString();
    }
    
    
    /**
     * If in the constructor a list of relevant words is given this method will load it as a Set.
     * This defines a filter-functionality for decreasing the memory load. 
     * 
     * @param filterFile (defined in the constructor)
     * @return Set<String>
     */
    private Set<String> loadSetOfRelevantWords()  
    		throws IOException
    {
    		Set<String> setOfRelevantWords = new HashSet<String>();
    		if (this.ignoreRelevantWordsList) { 
    			System.out.printf("Size of Set (relevantWords) is %d%n", setOfRelevantWords.size());
    			return setOfRelevantWords;
    		}
	    	try (BufferedReader br = new BufferedReader(new FileReader(filterFile))) {
	    	    String line;
	    	    while ((line = br.readLine()) != null) {
	    	       setOfRelevantWords.add(line.trim());
	    	    }
	    	}
	    	System.out.printf("Size of Set (relevantWords) is %d%n", setOfRelevantWords.size());
    		return setOfRelevantWords;
    }

}
