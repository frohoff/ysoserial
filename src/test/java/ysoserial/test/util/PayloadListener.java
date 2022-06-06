package ysoserial.test.util;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import ysoserial.Strings;

import java.io.ByteArrayOutputStream;
import java.util.*;

public class PayloadListener extends RunListener {
    public enum Status {
        SUCCESS,
        FAILURE,
        IGNORE,
        ASSUMPTION_FAILURE
    }

    private Map<Description, ByteArrayOutputStream> outs = new HashMap<Description, ByteArrayOutputStream>();
    private Map<Description, ByteArrayOutputStream> errs = new HashMap<Description, ByteArrayOutputStream>();

    private Map<Description, Status> statuses = new HashMap<Description, Status>();

    private Map<Description, Failure> failures = new HashMap<Description, Failure>();

    @Override
    public void testStarted(Description description) throws Exception {
//        System.out.println(getPayload(description.getDisplayName()) + ": STARTED");

        statuses.put(description, Status.SUCCESS);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
//            ByteArrayOutputStream err = new ByteArrayOutputStream();

        outs.put(description, out);
//            errs.put(description, err);

        StdIoRedirection.setStreams(out, out);
    }

    @Override
    public void testFinished(Description description) throws Exception {
        outs.get(description).close();
        //errs.get(description).close();

        StdIoRedirection.restoreStreams();

        Status status = statuses.get(description);
        String payload = getPayload(description.getDisplayName());
        String out = outs.get(description).toString().trim();

        Map<String,String> props = new HashMap<String, String>();
        props.put("payload", payload);
        props.put("status", status.toString());
        props.put("out", out);
        for (String k : Arrays.asList("java.version", "java.vendor", "java.vm.version", "java.runtime.version", "os.arch", "os.name", "os.version")) {
            props.put(k, System.getProperty(k));
        }

        List<String> pairs = new ArrayList<String>();
        for (Map.Entry<String, String> e : props.entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append("\"")
                .append(e.getKey().replace("\\", "\\\\").replace("\"", "\\\""))
                .append("\"")
                .append(": ")
                .append("\"")
                .append(e.getValue().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t").replace("\b", "\\b"))
                .append("\"");
            pairs.add(sb.toString());
        }

        String obj = "{" + Strings.join(pairs, ", ", "", "") + "}";

        System.out.println(obj);
//        System.out.println(payload + ": " + status);
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        statuses.put(failure.getDescription(), Status.FAILURE);
        failures.put(failure.getDescription(), failure);
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        statuses.put(failure.getDescription(), Status.ASSUMPTION_FAILURE);
        failures.put(failure.getDescription(), failure);
    }

    // testPayload[payloadClass: class ysoserial.payloads.JavassistWeld1](ysoserial.test.payloads.PayloadsTest)
    public static String getPayload(String displayName) {
        return displayName.replaceAll(".*\\[\\S+: class (\\w+[.$])+(\\w+)\\].*", "$2");
    }
}
