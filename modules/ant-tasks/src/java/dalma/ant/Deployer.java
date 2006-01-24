package dalma.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Deploys a dar file to the webui.
 *
 * @author Kohsuke Kawaguchi
 */
public class Deployer extends Task {
    private String name;
    private URL dalmaUrl;
    private File darFile;

    public void setName(String name) {
        this.name = name;
    }
    public void setURL(URL url) {
        this.dalmaUrl = url;
    }
    public void setFile(File darFile) {
        this.darFile = darFile;
    }

    private static final String BOUNDARY = "---aXej3hgOjxE";
    private static final String NEWLINE = "\r\n";

    public void execute() throws BuildException {
        try {
            if(name==null)
                throw new BuildException("No application name is specified");
            if(dalmaUrl==null)
                throw new BuildException("URL is not specified");

            log("Deploying to "+dalmaUrl, Project.MSG_INFO);

            HttpURLConnection con = (HttpURLConnection)new URL(dalmaUrl,"createApp").openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type","multipart/form-data; boundary="+BOUNDARY);
            con.connect();
            PrintStream out = new PrintStream(con.getOutputStream());
            out.print("--"+BOUNDARY);
            out.print(NEWLINE);
            out.print("Content-Disposition: form-data; name=\"name\"");
            out.print(NEWLINE);
            out.print(NEWLINE);
            out.print(name);
            out.print(NEWLINE);
            out.print("--"+BOUNDARY);
            out.print(NEWLINE);
            out.print("Content-Disposition: form-data; name=\"file\"; filename=\"some.dar\"");
            out.print(NEWLINE);
            out.print("Content-Type: application/octet-stream");
            out.print(NEWLINE);
            out.print("Content-Length: "+darFile.length());
            out.print(NEWLINE);
            out.print(NEWLINE);

            InputStream in = new FileInputStream(darFile);
            copyStream(in, out);
            out.print(NEWLINE);

            out.print("--"+BOUNDARY+"--");
            out.print(NEWLINE);
            out.close();

            if(con.getResponseCode()>=300) {
                String msg = "Failed to deploy: " + con.getResponseMessage();
                log(msg,Project.MSG_ERR);
                in = con.getErrorStream();
                copyStream(in,System.out);
                throw new BuildException(msg);
            }
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    private void copyStream(InputStream in, PrintStream out) throws IOException {
        byte[] buf = new byte[8192];
        int len;
        while((len=in.read(buf))>=0) {
            out.write(buf,0,len);
        }
        in.close();
    }
}
