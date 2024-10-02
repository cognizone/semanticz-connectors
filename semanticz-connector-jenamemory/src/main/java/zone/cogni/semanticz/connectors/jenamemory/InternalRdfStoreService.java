package zone.cogni.semanticz.connectors.jenamemory;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.shared.Lock;
import org.apache.jena.update.UpdateAction;
import zone.cogni.sem.jena.template.JenaResultSetHandler;
import zone.cogni.semanticz.connectors.general.RdfStoreService;
import zone.cogni.semanticz.connectors.utils.JenaUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.function.Supplier;

@Slf4j
public class InternalRdfStoreService implements RdfStoreService {

  @Getter
  private final Model model;

  @Setter
  private String savePath;

  private File storeFile;
  private File tempStoreFile;

  public InternalRdfStoreService() {
    model = ModelFactory.createDefaultModel();
  }

  public InternalRdfStoreService(Model model) {
    this.model = model;
  }

  @PostConstruct
  private void init() {
    if (StringUtils.isNotBlank(savePath)) {
      storeFile = new File(savePath, "store.rdf");
      tempStoreFile = new File(savePath, "temp-store.rdf");
      storeFile.getParentFile().mkdirs();

      if (storeFile.isFile()) JenaUtils.readInto(storeFile, model);
    }
  }

  @Override
  public void addData(Model model) {
    executeInWriteLock(() -> this.model.add(model));
  }


  @Override
  public void addData(Model model, String graphUri) {
    throw new RuntimeException("Add data with graph not supported"); //or we add to default graph?
  }

  @Override
  public <R> R executeSelectQuery(Query query, QuerySolutionMap bindings, JenaResultSetHandler<R> resultSetHandler, String context) {
    return executeInReadLock(() -> {
      if (log.isTraceEnabled()) log.trace("Select {} - {} \n{}",
                                          context == null ? "" : "--- " + context + " --- ",
                                          bindings,
                                          query);

      try (QueryExecution queryExecution = QueryExecutionFactory.create(query, model, bindings)) {
        ResultSet resultSet = queryExecution.execSelect();
        return resultSetHandler.handle(resultSet);
      }
      catch (RuntimeException e) {
        log.error("Query failed: {}", query);
        throw e;
      }
    });
  }

  @Override
  public boolean executeAskQuery(Query query, QuerySolutionMap bindings) {
    return executeInReadLock(() -> {
      try (QueryExecution queryExecution = QueryExecutionFactory.create(query, model, bindings)) {
        return queryExecution.execAsk();
      }
      catch (RuntimeException e) {
        log.error("Query failed: {}", query);
        throw e;
      }
    });
  }

  @Override
  public Model executeConstructQuery(Query query, QuerySolutionMap bindings) {
    return executeInReadLock(() -> {
      try (QueryExecution queryExecution = QueryExecutionFactory.create(query, model, bindings)) {
        if (log.isTraceEnabled()) log.trace("Running construct query: \n{}", query);
        return queryExecution.execConstruct();
      }
      catch (RuntimeException e) {
        log.error("Query failed: {}", query);
        throw e;
      }
    });
  }

  @Override
  public void executeUpdateQuery(String updateQuery) {
    executeInWriteLock(() -> {
      try {
        UpdateAction.parseExecute(updateQuery, model);
        if (null != storeFile) {
          JenaUtils.write(model, tempStoreFile);
          storeFile.delete();
          tempStoreFile.renameTo(storeFile);
        }
      }
      catch (Exception e) {
        throw new RuntimeException("Update SPARQL failed.\n" + updateQuery, e);
      }
    });
  }

  @Override
  public void delete() {
    model.removeAll();
  }

  private void executeInWriteLock(Runnable executeInLock) {
    model.enterCriticalSection(Lock.WRITE);
    try {
      executeInLock.run();
    }
    finally {
      model.leaveCriticalSection();
    }
  }

  private <T> T executeInReadLock(Supplier<T> executeInLock) {
    model.enterCriticalSection(Lock.READ);
    try {
      return executeInLock.get();
    }
    finally {
      model.leaveCriticalSection();
    }
  }
}