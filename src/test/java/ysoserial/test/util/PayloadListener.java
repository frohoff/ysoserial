package ysoserial.test.util;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

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
        System.out.println(getPayload(description.getDisplayName()) + ": STARTED");

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
        System.out.println(getPayload(description.getDisplayName()) + ": " + status);
        if (status == Status.FAILURE) System.err.println(outs.get(description).toString());
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
        return displayName.replaceAll(".*\\[\\S+: class (\\w+\\.)+(\\w+)\\].*", "$2");
    }
}
