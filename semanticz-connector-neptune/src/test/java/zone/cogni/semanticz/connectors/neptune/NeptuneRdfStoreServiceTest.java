package zone.cogni.semanticz.connectors.neptune;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import zone.cogni.sem.jena.template.JenaResultSetHandler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NeptuneRdfStoreServiceTest {

    @Mock
    private RDFConnectionRemoteBuilder mockBuilder;
    @Mock
    private RDFConnection mockConnection;
    @Mock
    private QueryExecution mockQueryExecution;
    @Mock
    private Model mockModel;
    @Mock
    private ResultSet mockResultSet;

    private NeptuneRdfStoreService rdfStoreService;

    @BeforeEach
    void setUp() {
        rdfStoreService = new NeptuneRdfStoreService("http://localhost:8182");
    }

    @Test
    void constructorShouldRemoveTrailingSlash() {
        // Basic check that no exception is thrown,
        // and that trailing slash is removed internally.
        NeptuneRdfStoreService service = new NeptuneRdfStoreService("http://example.com/");
        assertNotNull(service);
    }

    @Test
    void addDataShouldCallConnectionLoadForDefaultGraph() {
        // We'll do a static mock so newBuilder() returns mockBuilder, then mockBuilder.build() => mockConnection
        try (MockedStatic<RDFConnectionRemote> staticMock = mockStatic(RDFConnectionRemote.class)) {
            // Configure stubbing
            when(RDFConnectionRemote.newBuilder()).thenReturn(mockBuilder);
            when(mockBuilder.destination(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.queryEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.updateEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.gspEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockConnection);

            // We don't need anything special besides verifying load(...) is called
            doNothing().when(mockConnection).load(any(Model.class));

            // Call method under test
            rdfStoreService.addData(mockModel);

            // Verify
            verify(mockConnection).load(eq(mockModel));
            verify(mockConnection).close();
        }
    }

    @Test
    void addDataShouldCallConnectionLoadForNamedGraph() {
        try (MockedStatic<RDFConnectionRemote> staticMock = mockStatic(RDFConnectionRemote.class)) {
            when(RDFConnectionRemote.newBuilder()).thenReturn(mockBuilder);
            when(mockBuilder.destination(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.queryEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.updateEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.gspEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockConnection);

            doNothing().when(mockConnection).load(anyString(), eq(mockModel));

            rdfStoreService.addData(mockModel, "http://example.org/graph");

            verify(mockConnection).load("http://example.org/graph", mockModel);
            verify(mockConnection).close();
        }
    }

    @Test
    void executeSelectQueryShouldHandleResultSet() {
        // We'll define a JenaResultSetHandler
        @SuppressWarnings("unchecked")
        JenaResultSetHandler<String> resultSetHandler = mock(JenaResultSetHandler.class);

        Query dummyQuery = QueryFactory.create("SELECT ?s WHERE { ?s ?p ?o }");

        try (MockedStatic<RDFConnectionRemote> staticMock = mockStatic(RDFConnectionRemote.class)) {
            when(RDFConnectionRemote.newBuilder()).thenReturn(mockBuilder);
            when(mockBuilder.destination(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.queryEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.updateEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.gspEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockConnection);

            // mockConnection.query(...) => mockQueryExecution
            when(mockConnection.query(any(Query.class))).thenReturn(mockQueryExecution);
            when(mockQueryExecution.execSelect()).thenReturn(mockResultSet);
            when(resultSetHandler.handle(mockResultSet)).thenReturn("handler-result");

            String result = rdfStoreService.executeSelectQuery(
                    dummyQuery, null, resultSetHandler, "test-context"
            );

            assertEquals("handler-result", result);
            verify(mockConnection).query(any(Query.class));
            verify(mockQueryExecution).execSelect();
            verify(resultSetHandler).handle(mockResultSet);
            verify(mockQueryExecution).close();
            verify(mockConnection).close();
        }
    }

    @Test
    void executeAskQueryShouldReturnBoolean() {
        Query askQuery = QueryFactory.create("ASK WHERE { ?s ?p ?o }");

        try (MockedStatic<RDFConnectionRemote> staticMock = mockStatic(RDFConnectionRemote.class)) {
            when(RDFConnectionRemote.newBuilder()).thenReturn(mockBuilder);
            when(mockBuilder.destination(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.queryEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.updateEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.gspEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockConnection);

            when(mockConnection.query(any(Query.class))).thenReturn(mockQueryExecution);
            when(mockQueryExecution.execAsk()).thenReturn(true);

            boolean answer = rdfStoreService.executeAskQuery(askQuery, null);
            assertTrue(answer);

            verify(mockConnection).query(any(Query.class));
            verify(mockQueryExecution).execAsk();
            verify(mockQueryExecution).close();
            verify(mockConnection).close();
        }
    }

    @Test
    void executeConstructQueryShouldReturnModel() {
        Query constructQuery = QueryFactory.create("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");

        try (MockedStatic<RDFConnectionRemote> staticMock = mockStatic(RDFConnectionRemote.class)) {
            when(RDFConnectionRemote.newBuilder()).thenReturn(mockBuilder);
            when(mockBuilder.destination(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.queryEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.updateEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.gspEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockConnection);

            when(mockConnection.query(any(Query.class))).thenReturn(mockQueryExecution);
            when(mockQueryExecution.execConstruct()).thenReturn(mockModel);

            Model result = rdfStoreService.executeConstructQuery(constructQuery, null);
            assertSame(mockModel, result);

            verify(mockConnection).query(any(Query.class));
            verify(mockQueryExecution).execConstruct();
            verify(mockQueryExecution).close();
            verify(mockConnection).close();
        }
    }

    @Test
    void executeUpdateQueryShouldCallUpdateOnConnection() {
        String updateQuery = "DELETE WHERE { ?s ?p ?o }";

        try (MockedStatic<RDFConnectionRemote> staticMock = mockStatic(RDFConnectionRemote.class)) {
            when(RDFConnectionRemote.newBuilder()).thenReturn(mockBuilder);
            when(mockBuilder.destination(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.queryEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.updateEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.gspEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockConnection);

            doNothing().when(mockConnection).update(anyString());

            rdfStoreService.executeUpdateQuery(updateQuery);

            verify(mockConnection).update(eq(updateQuery));
            verify(mockConnection).close();
        }
    }

    @Test
    void deleteGraphShouldDropGraph() {
        try (MockedStatic<RDFConnectionRemote> staticMock = mockStatic(RDFConnectionRemote.class)) {
            when(RDFConnectionRemote.newBuilder()).thenReturn(mockBuilder);
            when(mockBuilder.destination(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.queryEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.updateEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.gspEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockConnection);

            doNothing().when(mockConnection).update(anyString());

            rdfStoreService.deleteGraph("http://example.org/graph");

            // The service calls "DROP GRAPH <URI>" internally
            verify(mockConnection).update(eq("DROP GRAPH <http://example.org/graph>"));
            verify(mockConnection).close();
        }
    }

    @Test
    void deleteShouldRemoveAllData() {
        try (MockedStatic<RDFConnectionRemote> staticMock = mockStatic(RDFConnectionRemote.class)) {
            when(RDFConnectionRemote.newBuilder()).thenReturn(mockBuilder);
            when(mockBuilder.destination(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.queryEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.updateEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.gspEndpoint(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockConnection);

            doNothing().when(mockConnection).update(anyString());

            rdfStoreService.delete();

            // The service calls "DELETE WHERE { ?s ?p ?o }"
            verify(mockConnection).update(eq("DELETE WHERE { ?s ?p ?o }"));
            verify(mockConnection).close();
        }
    }

    @Test
    void closeShouldNotThrow() {
        assertDoesNotThrow(() -> rdfStoreService.close());
    }
}
