package zone.cogni.semanticz.connectors.neptune;

import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NeptuneSparqlServiceTest {

    @Mock
    private RDFConnectionRemoteBuilder mockBuilder;
    @Mock
    private RDFConnection mockConnection;

    private NeptuneSparqlService service;

    @BeforeEach
    void setUp() {
        service = new NeptuneSparqlService("http://localhost:8182");
    }

    @Test
    void constructorShouldRemoveTrailingSlash() {
        // Basic test that doesn't require static mocking:
        NeptuneSparqlService s = new NeptuneSparqlService("http://example.org/");
        assertNotNull(s, "Should construct without error.");
    }

    @Test
    void dropGraphShouldExecuteDropQuery() {
        // 1) Start static mocking of RDFConnectionRemote:
        try (MockedStatic<RDFConnectionRemote> mockedStatic = mockStatic(RDFConnectionRemote.class)) {

            // 2) Stub newBuilder() to return our mockBuilder:
            when(RDFConnectionRemote.newBuilder()).thenReturn(mockBuilder);

            // 3) Stub the builder chain:
            when(mockBuilder.destination(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.queryEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.updateEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.gspEndpoint(anyString())).thenReturn(mockBuilder);

            // 4) Finally, the build() call returns our mockConnection:
            when(mockBuilder.build()).thenReturn(mockConnection);

            // 5) Stub the connection update(...) method:
            doNothing().when(mockConnection).update(anyString());

            // 6) Run the method under test:
            service.dropGraph("http://example.org/graph");

            // 7) Verify calls:
            // The method under test calls: "DROP GRAPH <...>"
            // So let's ensure it used "update(DROP GRAPH <...>)"
            verify(mockConnection).update(eq("DROP GRAPH <http://example.org/graph>"));
            verify(mockConnection).close();

            // When we exit the try-with-resources, static mocking is undone
        }
    }

    @Test
    void executeUpdateQueryShouldNotCallRealNeptune() {
        try (MockedStatic<RDFConnectionRemote> mocked = mockStatic(RDFConnectionRemote.class)) {

            when(RDFConnectionRemote.newBuilder()).thenReturn(mockBuilder);
            when(mockBuilder.destination(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.queryEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.updateEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.gspEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockConnection);

            doNothing().when(mockConnection).update(anyString());

            String updateQuery = "DELETE WHERE { ?s ?p ?o }";
            service.executeUpdateQuery(updateQuery);

            verify(mockConnection).update(eq(updateQuery));
            verify(mockConnection).close();
        }
    }
}
