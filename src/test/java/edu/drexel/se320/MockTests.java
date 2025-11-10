package edu.drexel.se320;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import java.util.List;
import java.io.IOException;

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
        when(sc.connectTo(anyString())).thenReturn(false);

        Client c = new Client(sc);
        String result = c.requestFile("dummyServer", "dummyFile");

        verify(sc).connectTo("dummyServer");
        verify(sc, never()).requestFileContents(anyString());
        assertNull(result);
    }

    @Test
    public void testSuccessfulDownload() throws IOException {
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

    @Test
    public void testInvalidFileReturnsNull() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        when(sc.connectTo("server")).thenReturn(true);
        when(sc.requestFileContents("nofile.txt")).thenReturn(false);

        Client c = new Client(sc);
        String result = c.requestFile("server", "nofile.txt");

        verify(sc).closeConnection();
        assertNull(result);
    }

    @Test
    public void testMultipleReadsConcatenated() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        when(sc.connectTo(anyString())).thenReturn(true);
        when(sc.requestFileContents(anyString())).thenReturn(true);
        when(sc.moreBytes()).thenReturn(true, true, false);
        when(sc.read()).thenReturn("A", "B");

        Client c = new Client(sc);
        String result = c.requestFile("server", "file");

        verify(sc, times(2)).read();
        assertEquals("AB", result);
    }

    @Test
    public void testIOExceptionDuringConnect() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        when(sc.connectTo(anyString())).thenThrow(new IOException());

        Client c = new Client(sc);
        assertNull(c.requestFile("server", "file"));
    }

    @Test
    
}
