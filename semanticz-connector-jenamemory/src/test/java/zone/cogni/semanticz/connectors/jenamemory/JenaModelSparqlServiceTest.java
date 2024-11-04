package zone.cogni.semanticz.connectors.jenamemory;

import zone.cogni.semanticz.connectors.utils.AbstractSparqlServiceTest;

public class JenaModelSparqlServiceTest extends AbstractSparqlServiceTest<JenaModelSparqlService> {

  @Override
  protected JenaModelSparqlService createSUT() {
    return new JenaModelSparqlService();
  }

  @Override
  protected void disposeSUT(JenaModelSparqlService sparqlService) {
    // nothing to do
  }
}