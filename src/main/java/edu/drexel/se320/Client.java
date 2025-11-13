/**
 * SE320 HOmework 3: Object Dependencies
 * 
 * Refactoring Explanation:
 * 
 * In the original version of this class, the Client directly instantiated an
 * anonymous ServerConnection inside the requestFile() method. Which made the
 * Client tightly coupled to the ServerConnection implementation and prevented it from
 * being tested independently of an actual network connection, which caused problems for the client.
 *
 * To make the Client testable and remove this hard dependency, I refactored
 * the class to use **constructor injection**, also known as the
 * **Passing the Collaborator** pattern discussed in lecture slides. (also given by homework instructions)
 *
 * This approach exposes the dependency by requiring a ServerConnection
 * instance to be passed into the Client constructor. Then the Client calls
 * methods on ServerConnection: connectTo, requestFileContents, moreBytes,
 * read, and closeConnection. And this is done without knowing or controlling its concrete type.
 *
 * During the mock testing process, a mock implementation of ServerConnection is injected using
 * the Mockito framework (stated by the homework instructions). This allows all behaviors to be verified without 
 * actually calling an actual server or network dependency.
 *
 * I chose this approach instead of alternatives like a collaborator factory or
 * service locator because constructor injection keeps the design very simple,
 * explicit, and fully compatible with unit testing frameworks if there were any. 
 * It also makes the dependency requirements of Client clear and enforces proper separation
 * between Client and ServerConnection.
 *
 * Homework Requirement: I left the original function behavior of not closing
 * the connection when an invalid file is requested to ensure that my tests
 * can detect the same bugs described in the homework assignment requirements.
 */

package edu.drexel.se320;

import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.lang.StringBuilder;

public class Client {

    private String lastResult;
    StringBuilder sb = null;
    private ServerConnection conn = null;

    public Client(ServerConnection conn) {
        if (conn == null) {
            throw new IllegalArgumentException("ServerConnection cannot be null!");
        }
        this.conn = conn;
        this.lastResult = "";
    }

    public String requestFile(String server, String file) {
        if (server == null)
            throw new IllegalArgumentException("Null server address");
        if (file == null)
            throw new IllegalArgumentException("Null file");

	// We'll use a StringBuilder to construct large strings more efficiently
	// than repeated linear calls to string concatenation.
        sb = new StringBuilder();

        try {
            if (conn.connectTo(server)) {
                boolean validFile = conn.requestFileContents(file);
                if (validFile) {
                    while (conn.moreBytes()) {
                        String tmp = conn.read();
                        if (tmp != null) {
                            sb.append(tmp);
                        }
                    }
                    conn.closeConnection();
                } 
                // NOTE: preserving the original behavior leaving bug based on assignment instruction
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }

        String result = sb.toString();
        lastResult = result;
        return result;
    }

    // Getter for Testing
    public String getLastResult() {
        return lastResult;
    }
}

