/**
 * © 2016 AgNO3 Gmbh & Co. KG
 * All right reserved.
 * 
 * Created: 05.03.2016 by mbechler
 */
package ysoserial.payloads;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Callable;

import org.junit.Assert;

import com.google.common.io.Files;

import ysoserial.CustomTest;

/**
 * @author mbechler
 *
 */
public class FileUploadTest implements CustomTest {

    /**
     * 
     */
    private static final byte[] FDATA = new byte[] {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD, (byte) 0xEE, (byte) 0xFF };
    private File source;
    private File repo;
    
    
    /**
     * 
     */
    public FileUploadTest () {
        try {
            source = File.createTempFile("fileupload-test", ".source");
            source.deleteOnExit();
            repo = Files.createTempDir();
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
      * {@inheritDoc}
      *
      * @see ysoserial.CustomTest#run(java.util.concurrent.Callable)
      */
    public void run ( Callable<Object> payload ) throws Exception {
        try {
            Files.write(FDATA, this.source);
            payload.call();
            
            File found = null;
            for ( File f : this.repo.listFiles()) {
                found = f;
                break;
            }
            Assert.assertNotNull("File not copied", found);
            Assert.assertFalse("Source not deleted", this.source.exists());
            Assert.assertTrue("Contents not copied", Arrays.equals(FDATA, Files.toByteArray(found)));
        } finally {
            if ( this.repo.exists()) {
                for ( File f : this.repo.listFiles()) {
                    f.delete();
                }
                this.repo.delete();
            }
        }
    }

    /**
      * {@inheritDoc}
      *
      * @see ysoserial.CustomTest#getPayloadArgs()
      */
    public String getPayloadArgs () {
        return "copyAndDelete:" + this.source.getAbsolutePath() + ":" + this.repo.getAbsolutePath();
    }

}
