package edu.drexel.se320;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.mockito.InOrder;

import java.util.List;
import java.io.IO;
import java.io.IOException;
import java.security.KeyStore.SecretKeyEntry;

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
    public void testIOExceptionDuringRead() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        when(sc.connectTo(anyString())).thenReturn(true);
        when(sc.requestFileContents(anyString())).thenReturn(true);
        when(sc.moreBytes()).thenReturn(true);
        when(sc.read()).thenThrow(new IOException());


        Client c = new Client(sc);
        assertNull(c.requestFile("server", "file"));
    }
    
    @Test
    public void testNullServerThrowsException() {
        ServerConnection sc = mock(ServerConnection.class);
        Client c  = new Client(sc);

        assertThrows(IllegalArgumentException.class, () -> c.requestFile(null, "file"));
    }

    @Test
    public void testNullFileThrowsException() {
        ServerConnection sc = mock(ServerConnection.class);
        Client c = new Client(sc);

        assertThrows(IllegalArgumentException.class, () -> c.requestFile("server", null));
    }

    @Test
    public void testCloseConnectionAlwaysCalledOnValidFIle() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        when(sc.connectTo(anyString())).thenReturn(true);
        when(sc.requestFileContents(anyString())).thenReturn(true);
        when(sc.moreBytes()).thenReturn(false);

        Client c = new Client(sc);
        c.requestFile("server", "file");

        verify(sc).closeConnection();
    }

    @Test
    public void testnullReadSkipped() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        when(sc.connectTo(anyString())).thenReturn(true);
        when(sc.requestFileContents(anyString())).thenReturn(true);
        when(sc.moreBytes()).thenReturn(true, true, false);
        when(sc.read()).thenReturn(null, "Data");


        Client c = new Client(sc);
        String result = c.requestFile("server", "file");

        assertEquals("Data", result);
    }

    @Test
    public void testEmptyFileStillClosesConnection() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        when(sc.connectTo(anyString())).thenReturn(true);
        when(sc.requestFileContents(anyString())).thenReturn(true);
        when(sc.moreBytes()).thenReturn(false);

        Client c = new Client(sc);
        String result = c.requestFile("server", "empty.txt");

        verify(sc).closeConnection();
        assertEquals("", result);
    }

    @Test
    public void testIOExceptionDuringClose() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        when(sc.connectTo(anyString())).thenReturn(true);
        when(sc.requestFileContents(anyString())).thenReturn(true);
        when(sc.moreBytes()).thenReturn(true, false);
        when(sc.read()).thenReturn("OK");
        doThrow(new IOException()).when(sc).closeConnection();

        Client c = new Client(sc);
        String result = c.requestFile("server", "file");

        // This works because result shouldn't change after read
        assertEquals("OK", result);
    }

    @Test
    public void testNoReadOnInvalidFile() throws IOException {
        ServerConnection sc = mock(ServerConnection.class);
        when(sc.connectTo(anyString())).thenReturn(true);
        when(sc.requestFileContents(anyString())).thenReturn(false);

        Client c = new Client(sc);
        c.requestFile("server", "badfile");
        verify(sc, never()).read();
    }

    @Test
    public void testInteractionOrder() throws IOException {
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

    @Test
    public void testNoReadsOrCloseWhenConnectFails() throws IOException {
        
    }
}
