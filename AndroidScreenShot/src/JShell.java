import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;


public class JShell {

    Thread tIn; // handle input of child process
    Thread tOut;// handle output of child process
    Thread tErr;// handle error output of child process
    public JShell( ) {
        
    }

    public void execute(String shellCommand) {

        Process child = null; // child process
        try {
            child = Runtime.getRuntime().exec(shellCommand);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (child == null) {
            return;
        }

        final InputStream inputStream = child.getInputStream();
        final BufferedReader brOut = new BufferedReader(new InputStreamReader(
                inputStream));

        tOut = new Thread() { // initialize thread tOut
            String line;
            int lineNumber = 0;

            public void run() {
                try {
                    while ((line = brOut.readLine()) != null) {
                        System.out.println(lineNumber + ". " + line);
                        lineNumber++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        final InputStream errorStream = child.getErrorStream();
        final BufferedReader brErr = new BufferedReader(new InputStreamReader(
                errorStream));

        tErr = new Thread() { // initialize thread tErr
            String line;
            int lineNumber = 0;

            public void run() {
                try {
                    while ((line = brErr.readLine()) != null) {
                        System.out.println(lineNumber + ". " + line);
                        lineNumber++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        // read buffer of parent process' input stream
        final BufferedReader reader = new BufferedReader(new InputStreamReader(
                System.in));
        final OutputStream outputStream = child.getOutputStream();
        tIn = new Thread() {
            String line;

            public void run() {
                try {
                    while (true) {
                        outputStream.write((reader.readLine() + "\n")
                                .getBytes());
                        outputStream.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        tErr.start();
    }

    public void startIn() { // start thread tIn
        if (tIn != null) {
            tIn.start();
        }
    }

    public void startErr() { // start thread tErr
        if (tErr != null) {
            tErr.start();
        }
    }

    public void startOut() { // start thread tOut
        if (tOut != null) {
            tOut.start();
        }
    }

    public void interruptIn() { // interrupt thread tIn
        if (tIn != null) {
            tIn.interrupt();
        }
    }

    public void interruptErr() { // interrupt thread tErr
        if (tErr != null) {
            tErr.interrupt();
        }
    }

    public void interruptOut() { // interrupt thread tOut
        if (tOut != null) {
            tOut.interrupt();
        }
    }

}
