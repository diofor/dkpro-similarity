package org.dkpro.similarity.algorithms.vsm.store;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;
import java.util.stream.IntStream;

import org.apache.commons.io.IOUtils;
import org.dkpro.similarity.algorithms.api.SimilarityException;
import org.dkpro.similarity.algorithms.vsm.store.vectorindex.VectorIndexContract;
import org.junit.Ignore;
import org.netlib.util.doubleW;

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

	private Map<String, double[]> embeddings;
	private final File embeddingsFile; 
	private String lineSeperatorSequence; //will be used later
	private String valueSeperatorSequence;
	
	public enum EmbeddingType{
		GLOVE
	}
	
	public EmbeddingVectorReader(File path, EmbeddingType typeOfEmbedding) {
		this.embeddingsFile = path;
		if (typeOfEmbedding.equals(EmbeddingType.GLOVE))
		{
			lineSeperatorSequence = "\\n";
			valueSeperatorSequence = " ";
		}
	}
	
	@Override
	public Vector getVector(String term) throws SimilarityException {
		if (!embeddings.containsKey(term))
		{
			//ToDo: What happens if the term does not exist in the embedding
			System.err.printf("Term \"%s\" does not exist in embedding. Missing treatment!%n", term);
		}
		int vSize = getEmbeddings().get(term).length;
		int[] indices = IntStream.range(0, vSize).toArray();
		return new SparseVector(vSize, indices, getEmbeddings().get(term));
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
			BufferedReader in = null;
			String line;
			String[] partsOfLine;
			String term;
			double[] values;
			try {
				//propably here is a GZipInputStream missing
				in = new BufferedReader(new InputStreamReader(new FileInputStream(embeddingsFile), CONFIG_FILE_ENCODING));
				
				while((line  = in.readLine()) != null)
				{
					partsOfLine = line.split(valueSeperatorSequence);
					term = partsOfLine[0];
					values = new double[partsOfLine.length-1];
					for (int i = 1; i < partsOfLine.length; i++) {
						values[i-1] = Double.valueOf(partsOfLine[i]);
					}
					embeddings.put(term, values);
				}
			}
			catch (IOException e) {
				throw new SimilarityException(e);
			}
			finally {
				IOUtils.closeQuietly(in);
			}
		}
		
		return embeddings;
	}

}
