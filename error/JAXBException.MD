# JAXBException 

  Cause: This error was happening in Intellij IDE, once changed java version, it fixed. 
  
  
[ERROR] Failed to execute goal org.jvnet.jaxb2.maven2:maven-jaxb2-plugin:0.8.1:generate (schema) on project platform-common-schema-ebpi-legacy: Execution schema of goal org.jvnet.jaxb2.maven2:maven-jaxb2-plugin:0.8.1:generate failed: A required class was missing while executing org.jvnet.jaxb2.maven2:maven-jaxb2-plugin:0.8.1:generate: javax/xml/bind/JAXBException
[ERROR] -----------------------------------------------------
[ERROR] realm =    plugin>org.jvnet.jaxb2.maven2:maven-jaxb2-plugin:0.8.1
[ERROR] strategy = org.codehaus.plexus.classworlds.strategy.SelfFirstStrategy
[ERROR] urls[0] = file:/Users/mpaulose/.m2/repository/org/jvnet/jaxb2/maven2/maven-jaxb2-plugin/0.8.1/maven-jaxb2-plugin-0.8.1.jar
[ERROR] urls[1] = file:/Users/mpaulose/.m2/repository/org/jvnet/jaxb2/maven2/maven-jaxb2-plugin-core/0.8.1/maven-jaxb2-plugin-core-0.8.1.jar
[ERROR] urls[2] = file:/Users/mpaulose/.m2/repository/com/sun/org/apache/xml/internal/resolver/20050927/resolver-20050927.jar
[ERROR] urls[3] = file:/Users/mpaulose/.m2/repository/org/sonatype/plexus/plexus-build-api/0.0.7/plexus-build-api-0.0.7.jar
[ERROR] urls[4] = file:/Users/mpaulose/.m2/repository/junit/junit/4.8.1/junit-4.8.1.jar
[ERROR] urls[5] = file:/Users/mpaulose/.m2/repository/org/jfrog/maven/annomojo/maven-plugin-anno/1.3.1/maven-plugin-anno-1.3.1.jar
[ERROR] urls[6] = file:/Users/mpaulose/.m2/repository/org/codehaus/plexus/plexus-utils/1.5.15/plexus-utils-1.5.15.jar
[ERROR] urls[7] = file:/Users/mpaulose/.m2/repository/org/jvnet/jaxb2/maven2/maven-jaxb22-plugin/0.8.1/maven-jaxb22-plugin-0.8.1.jar
[ERROR] urls[8] = file:/Users/mpaulose/.m2/repository/com/sun/xml/bind/jaxb-impl/2.2.5-b10/jaxb-impl-2.2.5-b10.jar
[ERROR] urls[9] = file:/Users/mpaulose/.m2/repository/com/sun/xml/bind/jaxb-xjc/2.2.5-b10/jaxb-xjc-2.2.5-b10.jar
[ERROR] Number of foreign imports: 1
[ERROR] import: Entry[import  from realm ClassRealm[maven.api, parent: null]]
[ERROR] 
[ERROR] -----------------------------------------------------
[ERROR] : javax.xml.bind.JAXBException
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/PluginContainerException
