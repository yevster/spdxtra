package org.quackware.spdxtra;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;

/**
 * An auto-closable transaction that, if closed prior to comitting will abort.
 * If closed after a commit, has no effect.
 * Not thread-safe.
 * @author yevster
 *
 */
public class DatasetAutoAbortTransaction implements AutoCloseable{
	private boolean alreadyEnded = false;
	
	private Dataset dataset;
	private DatasetAutoAbortTransaction(Dataset dataset){
		this.dataset = dataset;
	}

	public static DatasetAutoAbortTransaction begin(Dataset dataset, ReadWrite readWrite){
		dataset.begin(readWrite);
		return new DatasetAutoAbortTransaction(dataset);
	}
	
	public void commit(){
		alreadyEnded = true;
		dataset.commit();
	}
	
	public void abort(){
		alreadyEnded = true;
		dataset.abort();
		
	}
	
	@Override
	public void close(){
		if (!alreadyEnded){
			dataset.abort();
		}
	}
}
