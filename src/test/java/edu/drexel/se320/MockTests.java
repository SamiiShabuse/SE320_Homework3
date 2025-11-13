/**
 * SE320 Homework 3: Object Dependencies
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

public class MockTests {

    public MockTests() {}

    /**
     * Demonstrate a working mock from the Mockito documentation.
     * https://static.javadoc.io/org.mockito/mockito-core/latest/org/mockito/Mockito.html#1
     */
    @Test
    public void testMockDemo() {
         List<String> mockedList = (List<String>)mock(List.class);

         mockedList.add("one");
         mockedList.clear();

         verify(mockedList).add("one");
         verify(mockedList).clear();
    }

    @Test
    public void testServerConnectionFailureGivesNull() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        Client c = new Client(sc);
        when(sc.connectTo(anyString())).thenReturn(false);

        // If you change the code to pass the mock above to the client (based on your choice of
        // refactoring), this test should pass.  Until then, it will fail.
        assertNull(c.requestFile("DUMMY", "DUMMY"));
    }

    /**
     * test1
     * 
     * 1: Test that if the attempt to connectTo(...) the server fails, 
     * the client code calls no further methods on the connection.
     * 
     */
    @Test
    public void test1() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        when(sc.connectTo("s")).thenReturn(false);
        Client c = new Client(sc);

        String result = c.requestFile("s", "f");

        assertNull(result);
        verify(sc).connectTo("s");
        verify(sc, never()).requestFileContents(anyString());
        verify(sc, never()).moreBytes();
        verify(sc, never()).read();
        verify(sc, never()).closeConnection();
    }

    /**
     * test2
     * 
     * 2: Test that if the connection succeeds but there is no valid file of that name, 
     * the client code calls no further methods on the connection except closeConnection. 
     * That is, the client code is expected to call closeConnection exactly once, 
     * but should not call other methods after it is known the file name is invalid.
     * 
     */
    @Test
    public void test2() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        when(sc.connectTo("s")).thenReturn(true);
        when(sc.requestFileContents("missing.txt")).thenReturn(false);
        Client c = new Client(sc);

        String result = c.requestFile("s", "missing.txt");

        assertNull(result);
        verify(sc).connectTo("s");
        verify(sc).requestFileContents("missing.txt");
        verify(sc, never()).moreBytes();
        verify(sc, never()).read();
        verify(sc, times(1)).closeConnection(); 
    }

    /**
     * test3
     * 
     * 3: Test that if the connection succeeds and the file is valid and non-empty, 
     * that the connection asks for at least some part of the file. 
     * (We want you to check that the client makes the request; examining the return value is insufficient for this.)
     * 
     */
    @Test
    public void test3() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        when(sc.connectTo("server")).thenReturn(true);
        when(sc.requestFileContents("file.txt")).thenReturn(true);
        when(sc.moreBytes()).thenReturn(true, false);
        when(sc.read()).thenReturn("Hello");

        Client c = new Client(sc);
        String result = c.requestFile("server", "file.txt");

        verify(sc).connectTo("server");
        verify(sc).requestFileContents("file.txt");
        verify(sc).read();
        verify(sc).closeConnection();

        assertEquals("Hello", result);
    }

    /**
     * test4
     * 
     * 4: Test that if the connection succeeds and the file is valid but empty, the client returns an empty string.
     * 
     */
    @Test
    public void test4() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        when(sc.connectTo(anyString())).thenReturn(true);
        when(sc.requestFileContents(anyString())).thenReturn(true);
        when(sc.moreBytes()).thenReturn(false);

        Client c = new Client(sc);
        String result = c.requestFile("server", "empty.txt");

        verify(sc).closeConnection();
        assertEquals("", result);
    }

    /**
     * test5
     *
     * 5: Test that if the client successfully reads part of a file, and 
     * then an IOException occurs before the file is fully read (i.e., moreBytes() has not returned false), 
     * the client still returns null to indicate an error, rather than returning a partial result.
     * 
     */
    @Test
    public void test5() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        when(sc.connectTo("s")).thenReturn(true);
        when(sc.requestFileContents("f")).thenReturn(true);
        when(sc.moreBytes()).thenReturn(true); 
        when(sc.read()).thenThrow(new IOException()); 
        Client c = new Client(sc);

        assertNull(c.requestFile("s", "f"));
    }

    /**
     * test6
     * 
     * 6: Test that if the initial server connection succeeds, then if an IOException occurs while retrieving 
     * the file (requesting, or reading bytes, either one) the client still explicitly closes the server connection.
     * 
     */
    @Test
    public void test6() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        when(sc.connectTo("s")).thenReturn(true);
        when(sc.requestFileContents("f")).thenReturn(true);
        when(sc.moreBytes()).thenReturn(true);
        when(sc.read()).thenThrow(new IOException());
        Client c = new Client(sc);

        assertNull(c.requestFile("s", "f"));
        verify(sc).closeConnection(); 
    }

    /**
     * test7
     * 
     * 7: Test that if the initial connection succeeds and the file is valid and non-empty, 
     * the client calls moreBytes() before it calls read().
     * 
     */
    @Test
    public void test7() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        when(sc.connectTo("s")).thenReturn(true);
        when(sc.requestFileContents("f")).thenReturn(true);
        when(sc.moreBytes()).thenReturn(true, false);
        when(sc.read()).thenReturn("X");
        
        Client c = new Client(sc);

        c.requestFile("s", "f");

        InOrder inOrder = inOrder(sc);
        inOrder.verify(sc).connectTo("s");
        inOrder.verify(sc).requestFileContents("f");
        inOrder.verify(sc).moreBytes();
        inOrder.verify(sc).read();
        inOrder.verify(sc).moreBytes();
        inOrder.verify(sc).closeConnection();
    }

    /**
     * test8
     * 
     * 8: If the server returns the file in four pieces (i.e., four calls to read() must be executed), 
     * the client concatenates them in the correct order).
     * 
     */
    @Test
    public void test8() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        when(sc.connectTo("s")).thenReturn(true);
        when(sc.requestFileContents("f")).thenReturn(true);
        when(sc.moreBytes()).thenReturn(true, true, true, true, false);
        when(sc.read()).thenReturn("A", "B", "C", "D");
        Client c = new Client(sc);

        String result = c.requestFile("s", "f");
        assertEquals("ABCD", result);
    }

    /**
     * test9
     * 
     * 9: If read() ever returns null, the client treats this as the empty string. 
     * This stands in contrast to appending “null” to the file contents read thus far, 
     * which is the default if you simply append null. In Java, "asdf"+null evaluates to “asdfnull”.
     */
    @Test
    public void test9() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        when(sc.connectTo(anyString())).thenReturn(true);
        when(sc.requestFileContents(anyString())).thenReturn(true);
        when(sc.moreBytes()).thenReturn(true, true, false);
        when(sc.read()).thenReturn(null, "Data");

        Client c = new Client(sc);
        String result = c.requestFile("server", "file");

        assertEquals("Data", result);
    }

    /**
     * test10a
     * 
     * 10: Test that if any of the connection operations fails the first time it is executed with an IOException, 
     * the client returns null.
     */
    @Test
    public void test10a() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        when(sc.connectTo(anyString())).thenThrow(new IOException());

        Client c = new Client(sc);
        assertNull(c.requestFile("server", "file"));
    }

    /**
     * test10b
     * 
     * 10: Test that if any of the connection operations fails the first time it is executed with an IOException, 
     * the client returns null.
     */
    @Test
    public void test10b() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        when(sc.connectTo("s")).thenReturn(true);
        when(sc.requestFileContents("f")).thenThrow(new IOException());
        Client c = new Client(sc);

        assertNull(c.requestFile("s", "f"));
    }

    /**
     * test10c
     * 
     * 10: Test that if any of the connection operations fails the first time it is executed with an IOException, 
     * the client returns null.
     */
    @Test
    public void test10c() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        when(sc.connectTo("s")).thenReturn(true);
        when(sc.requestFileContents("f")).thenReturn(true);
        when(sc.moreBytes()).thenThrow(new IOException()); 
        Client c = new Client(sc);

        assertNull(c.requestFile("s", "f"));
    }

    /**
     * test10d
     * 
     * 10: Test that if any of the connection operations fails the first time it is executed with an IOException, 
     * the client returns null.
     */
    @Test
    public void test10d() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        when(sc.connectTo("s")).thenReturn(true);
        when(sc.requestFileContents("f")).thenReturn(true);
        when(sc.moreBytes()).thenReturn(true);
        when(sc.read()).thenThrow(new IOException());
        Client c = new Client(sc);

        assertNull(c.requestFile("s", "f"));
    }

    /**
     * test10e
     * 
     * 10: Test that if any of the connection operations fails the first time it is executed with an IOException, 
     * the client returns null.
     */
    @Test
    public void test10e() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        when(sc.connectTo("s")).thenReturn(true);
        when(sc.requestFileContents("f")).thenReturn(true);
        when(sc.moreBytes()).thenReturn(false); // no reads, still closes
        doThrow(new IOException()).when(sc).closeConnection();
        Client c = new Client(sc);

        assertNull(c.requestFile("s", "f")); // Client catches and returns null
    }
}