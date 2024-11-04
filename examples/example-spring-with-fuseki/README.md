# Example - Spring Connector with Fuseki

You need Java 11+, `curl` and `gradle` to run this example. 

1. First, run a Fuseki server:
```bash
curl -o fuseki.jar https://repo1.maven.org/maven2/org/apache/jena/jena-fuseki-server/4.10.0/jena-fuseki-server-4.10.0.jar
mkdir -p fuseki-workspace-test 
java -jar fuseki.jar --port 3030 --localhost --loc=fuseki-workspace-test --auth=basic --update --ping -v --passwd=passwd /  
```

If you want to use your existing server instance, don't forget to adjust `src/main/resources/application.yml` accordingly. 

2. Run the application
```bash
./gradlew :bootRun
```

3. Use the application
- to add a new triple:
```bash
curl --location --request PATCH 'http://localhost:8080/data' \
--form 's="http://example.org/we"' \
--form 'p="love"' \
--form 'o="RDFX"' 
```

- to list all triples in JSON:
```bash
curl --location --request GET 'http://localhost:8080/data'
```