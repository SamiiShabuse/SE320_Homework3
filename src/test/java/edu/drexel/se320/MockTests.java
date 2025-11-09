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
}
