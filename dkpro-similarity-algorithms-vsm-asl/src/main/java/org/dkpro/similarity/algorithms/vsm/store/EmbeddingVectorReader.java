package org.dkpro.similarity.algorithms.vsm.store;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.FilenameUtils;
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
	public enum EmbeddingType{
		GLOVE, Word2Vec, FastText
	}
	
	public enum EmbeddingFormat{
		bin, txt
	}

	private static final int MAX_SIZE = 50;
	private Map<String, double[]> embeddings;
	private final File embeddingsFile; 
	private final File filterFile;
	private String valueSeperatorSequence;
	private EmbeddingType embeddingType;
	private EmbeddingFormat embeddingFormat;
	private boolean ignoreRelevantWordsList;
	
	
	
	public EmbeddingVectorReader(File embeddingFile, EmbeddingType typeOfEmbedding, EmbeddingFormat formatOfEmbedding, File filterFile) {
		this.embeddingsFile = embeddingFile;
		
		this.filterFile = filterFile.exists() ?  filterFile : null;
		ignoreRelevantWordsList = (this.filterFile == null); 
		
		valueSeperatorSequence = " "; //TODO
		
		embeddingFormat = formatOfEmbedding;
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
			switch (embeddingFormat) {
				case bin:	loadBinModel();		break;
				case txt:	loadTextModel();		break;
				default: System.err.println("Undefinded Format of Embedding. "); break;
			}
			
			System.out.printf("Size of embedding is %d. (only the loaded part is considered)%n", embeddings.size() );
		}
		
		return embeddings;
	}
	
	private void loadTextModel()
	{
		switch (embeddingType) {
		case GLOVE:
			try {
				loadTextModelWithoutHeadline();
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		case Word2Vec:
		case FastText:
			try {
				loadTextModelWithHeadline();
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		default:
			System.err.println("Should not be reached. No embeddingType defined in EmbeddingVectorReader");
			break;
		}
	}
	
	private void loadBinModel()
	{
		//handle the different streams causes by different compressions
		try(CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream(
				new BufferedInputStream(new FileInputStream(embeddingsFile)));
				DataInputStream din = new DataInputStream(in))
		{
			switch (embeddingType) {
			case GLOVE:
				System.err.println("No Model known with binary glove format. Not defined in EmbeddingVectorReader");
				break;
			case Word2Vec:
			case FastText:
				loadBinModelWithHeadline();
				break;
			default:
				System.err.println("Should not be reached. No embeddingType defined in EmbeddingVectorReader");
				break;
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CompressorException e) {
			e.printStackTrace();
		}
	}
	
	
	/*
	 * Normally this matches Word2Vec and FastText
	 */
	//
	private void loadTextModelWithHeadline() throws IOException
	{
		try (BufferedReader reader = getReader())
		{
			//read information about #Vectors and #VectorDimensions
	        String[] firstLine = reader.readLine().split(valueSeperatorSequence);
	        int words, vecSize;
	        words = Integer.parseInt(firstLine[0]);
	        vecSize = Integer.parseInt(firstLine[1]);
	        
	        String[] partsOfLine;
	        double[] vector;
	        Set<String> relevantWords = loadSetOfRelevantWords();
	
	        String line;
	        String word;
	        while((line = reader.readLine()) != null)
	        {
	        		partsOfLine = line.split(valueSeperatorSequence);
	            
	        		word = partsOfLine[0];
	        		
//	            if (word.contains("/en/")) word = word.substring(4); //to remove in freebase en the language-marker TODO 
	            
	            if (!ignoreRelevantWordsList && !relevantWords.contains(word)) continue; //abort String to Vector transformation if not on the relevantWordsList
	            
	            vector = new double[vecSize];
	
	            for (int j = 0; j < vecSize; j++) {
	                vector[j] = Double.parseDouble(partsOfLine[j+1]);
	            }
	            
	            	embeddings.put(word, vector);
	        }
		}
	}
	
	
	/*
	 * Normally this is Glove
	 */
	private void loadTextModelWithoutHeadline() throws IOException
	{
		try(BufferedReader reader = getReader()){
			String[] partsOfLine;
			double[] vector;
			Set<String> relevantWords = loadSetOfRelevantWords();
			
			String line;
			String word;
			while((line  = reader.readLine()) != null)
			{
				partsOfLine = line.split(valueSeperatorSequence);
				word = partsOfLine[0];
				
				if (relevantWords.contains(word) || ignoreRelevantWordsList)
				{
					vector = new double[partsOfLine.length-1];
					for (int i = 1; i < partsOfLine.length; i++) {
						vector[i-1] = Double.valueOf(partsOfLine[i]);
					}
					embeddings.put(word, vector);
				}
				partsOfLine = null;
			}
		}
	}
	
	
	/*
	 * Normally only Word2Vec but also able to read FastText
	 * 
	 * Reads byte by byte	
	 */
	private void loadBinModelWithHeadline() throws IOException
	{
		boolean linebreaks = false; //TODO
		int words, vecSize;
		Set<String> relevantWords = loadSetOfRelevantWords();
		try (DataInputStream dis = getInputStream())
		{
	        words = Integer.parseInt(readString(dis));
	        vecSize = Integer.parseInt(readString(dis));
	        double[] vector;
	        String word;
	        
	        System.out.println("Reading Embedding...");
	        short nextStep = 5;
	        for (long i = 0; i < words; i++) {
	        		if (((i*100)/words) >= nextStep)
	        		{
	        			System.out.print(nextStep  + "% | ");
	        			nextStep += 5;
	        		}
	        		
	        		vector = new double[vecSize];
	        		word = readString(dis);
	            	            
	//            if (word.contains("/en/")) word = word.substring(4); //to remove in freebase en the language-marker TODO 
	            int bytesToRead = 4*vecSize;
	            byte[] bytes = new byte[bytesToRead];
	            dis.read(bytes);
	            
	            if (linebreaks) {
	                dis.readByte(); // line break
	            }
	            
	            if (!ignoreRelevantWordsList && !relevantWords.contains(word)) continue;  //abort String to Vector transformation if not on the relevantWordsList
	            
	            byte[] floatBytes = new byte[4];
	            for(int j = 0; j < bytesToRead; j = j+4) {
	            		for (short k = 0 ; k < 4; k++)
	            		{
	            			floatBytes[k] = bytes[j+k];
	            		}
	            		vector[j/4] = getFloat(floatBytes);
	            }
	            
	            	embeddings.put(word, vector);
	        }
		}
	}	
	
	private BufferedReader getReader() 
	{
		String format = FilenameUtils.getExtension(embeddingsFile.getAbsolutePath());
		
		//handle the different streams causes by different compressions		
		if ("zip".equals(format))
		{
			try
			{
				ZipFile zf = new ZipFile(embeddingsFile);
				ArrayList<ZipArchiveEntry> list = Collections.list(zf.getEntries());
				if (list.size() >= 1) {
					return new BufferedReader(new InputStreamReader(zf.getInputStream(list.get(0))));
				} else {
					try {
						zf.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
		{
			try
			{
				CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream(
						new BufferedInputStream(new FileInputStream(embeddingsFile)));
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				return reader;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (CompressorException e) {
				e.printStackTrace();
			}
		}
		
		System.err.println("Returns a unresolved null value.");
		return null;
	}
	
	private DataInputStream getInputStream()
	{
		String format = FilenameUtils.getExtension(embeddingsFile.getAbsolutePath());
		
		//handle the different streams causes by different compressions		
		if ("zip".equals(format))
		{
			try {
				ZipFile zf = new ZipFile(embeddingsFile);
				ArrayList<ZipArchiveEntry> list = Collections.list(zf.getEntries());
				if (list.size() >= 1) {
					return new DataInputStream(zf.getInputStream(list.get(0)));
				} else {
					try {
						zf.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
		{
			try
			{
				CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream(
						new BufferedInputStream(new FileInputStream(embeddingsFile)));
				DataInputStream din = new DataInputStream(in);
				return din;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (CompressorException e) {
				e.printStackTrace();
			}
		}
		return null;
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
	    	System.out.printf("Number of words to be tested is %d (Size of Filter-Set)%n", setOfRelevantWords.size());
    		return setOfRelevantWords;
    }

}
