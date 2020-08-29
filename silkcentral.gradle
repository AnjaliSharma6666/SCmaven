import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.xml.MarkupBuilder

//Specify startConfig here or pass as properties when launching the script
//ext.sc_executionNodeIds = '22431,22432,22433'
//ext.sc_host = 'http://localhost:19120'
//ext.sc_token = '80827e02-cfda-4d2d-b0aa-2d5205eb6ea9'
//ext.sc_sourceControlBranch
//ext.sc_buildName
//ext.sc_sinceBuild
//ext.sc_StartOption
//ext.sc_collectResults
//ext.sc_collectResultFiles
//ext.sc_startDelay

task silkCentralLaunch {
  def sc_propertyNames = ['sc_executionNodeIds', 'sc_host', 'sc_token', 'sc_sourceControlBranch', 'sc_buildName', 'sc_sinceBuild', 'sc_StartOption', 'sc_collectResults', 'sc_startDelay']
  verifyProperties()
  doLast {
    def parameters = project.ext.properties.findAll { !sc_propertyNames.contains(it.key) && !it.key.startsWith('teamcity')}
    def startConfig = new StartConfig()
    parameters.each{startConfig.parameters.add([name:it.key, value:it.value])}
    startConfig.sourceControlBranch = findProperty('sc_sourceControlBranch')
    startConfig.buildName = findProperty('sc_buildName')
    startConfig.sinceBuild = findProperty('sc_sinceBuild')
    startConfig.startOption = findProperty('sc_StartOption') ? findProperty('sc_StartOption').trim() : null
    
    println 'Starting execution plan with following startConfig:'
    println JsonOutput.prettyPrint(JsonOutput.toJson(startConfig))
    def startedEdrs = []
    sc_executionNodeIds.split(',').each { executionNodeId ->
        def startRun = getURLConnection("/Services1.0/execution/executionplanruns?nodeId=${executionNodeId}")
        startRun.setRequestMethod("POST")
        startRun.getOutputStream().write(JsonOutput.toJson(startConfig).getBytes("UTF-8"));
        if (startRun.getResponseCode() == 200) {
          def edrs = new JsonSlurper().parseText(startRun.getInputStream().getText())
          edrs.each { edr ->
            println "Started run with id ${edr.executionPlanRunId} for execution plan '${edr.executionPlanName}' "
            startedEdrs.add(edr)
            sleep((findProperty('sc_startDelay') ?: '0').toInteger() * 1000)
          }
        } else {
          throw new GradleException ("Failed to start execution plan '${executionNodeId}': " +  startRun.getErrorStream().getText())
        }
    }
    
    if ((findProperty('sc_collectResults') ?: 'true') != 'false') {
      def i = 0;
      def finishedEdrMap = [:]
      while (startedEdrs.size() > finishedEdrMap.size()) {
        startedEdrs.findAll { !finishedEdrMap.containsKey(it) }.each { 
          try {
            def getRun = getURLConnection("/Services1.0/execution/executionplanruns/${it.executionPlanRunId}")
            def edr = new JsonSlurper().parseText(getRun.getInputStream().getText())
            println "Run with id ${edr.executionPlanRunId} for execution plan '${edr.executionPlanName}' has status '${edr.status}'"
            if (edr.status != 'PENDING') {
              finishedEdrMap.put(edr, null);
            } else {
               pauseResultFetching(++i)
            }
          } catch (Exception e) {
            throw new GradleException ("Failed to get run for execution plan '${it.executionPlanName}': " + e.getMessage(), e)
          }
        }
      }
      
      finishedEdrMap.each{ edr, v -> 
        try {
          def getTestRuns = getURLConnection("/Services1.0/execution/testruns?executionPlanRunId=${edr.executionPlanRunId}")
          def testRuns = new JsonSlurper().parseText(getTestRuns.getInputStream().getText())
          finishedEdrMap.put(edr, testRuns)
        } catch (Exception e) {
          throw new GradleException ("Failed to get test runs for execution plan '${edr.executionPlanName}': " + e.getMessage(), e)
        }
      }
      writeResults(finishedEdrMap)
    } else {
      println 'Skip collecting results as sc_collectResults property is set to false'
    }
  }  
}

def verifyProperties() {
  def requiredProperties = ['sc_executionNodeIds', 'sc_host', 'sc_token']
  requiredProperties.each {
    if (!project.hasProperty(it)) {
      throw new InvalidUserDataException("Please specify property ${it}")
    }
  }
}

def getURLConnection(restUrl) {
    def requestHeaders = [
      "accept": "application/json;charset=UTF-8",
      "SC-SESSION-ID": "${sc_token}",
      "Content-Type": "application/json"
    ]
    def urlConnection = new URL(sc_host + restUrl).openConnection();
    urlConnection.setDoOutput(true)
    requestHeaders.each{ k, v -> urlConnection.setRequestProperty(k, v) }
    return urlConnection
}

def writeResults(finishedEdrMap) {
  def resultFolder = new File('sc_results')
  resultFolder.deleteDir()
  resultFolder.mkdir()
  finishedEdrMap.each{ edr, testRuns ->
    def writer = new StringWriter()
    def xmlMarkup = new MarkupBuilder(writer)
    xmlMarkup.mkp.xmlDeclaration(version: "1.0", encoding: "utf-8") 
    xmlMarkup
    .'testsuite' (id: edr.executionPlanRunId, name: edr.executionPlanName.replaceAll('\\.','_'), time: edr.duration/1000.0, errors: testRuns.sum{testRun -> testRun.status != 'PASSED' ? 1 : 0}) {
        testRuns.each { testRun -> 
            'testcase'(id: testRun.testRunId, name: testRun.testName.replaceAll('\\.','_'), time: testRun.duration/1000.0) {
              if (testRun.status != 'PASSED') {
                'failure'(message:getTestLink(testRun))
              }
            }
        }
    }
    new File(resultFolder, "junit${edr.executionPlanRunId}.xml").write writer.toString()
  }
  if ((findProperty('sc_collectResultFiles') ?: 'false') != 'false') {
    finishedEdrMap.each{ edr, testRuns ->
        testRuns.each { testRun ->
        def resultFiles = new JsonSlurper().parseText(getURLConnection("/Services1.0/execution/resultfiles?testRunId=${testRun.testRunId}").getInputStream().getText())
        resultFiles.each{resultFile -> 
          File folder = new File(resultFolder, testRun.testName)
          folder.mkdir()
          def fileName = resultFile.fileName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_") + '.' + resultFile.fileType
          println "Downloading " + testRun.testName + "/" + fileName
          def downloadStream = getURLConnection("/Services1.0/execution/resultfiles/${resultFile.fileId}").getInputStream();
          BufferedInputStream bis = new BufferedInputStream(downloadStream);
          FileOutputStream fis = new FileOutputStream(new File(folder, fileName));
          byte[] buffer = new byte[1024];
          int count=0;
          while((count = bis.read(buffer,0,1024)) != -1) {
            fis.write(buffer, 0, count);
          }
          fis.close();
          bis.close();
        }
      }
    }
  }
}

def getTestLink(testRun) {
  return "${sc_host}/silk/DEF/TM/Execution?pId=${testRun.projectId}&testRunId=${testRun.testRunId}&execView=execDetails&etab=4"
}

def pauseResultFetching(i) {
  if (i > 36) {
    sleep(20000)
  } else if (i > 12) {
    sleep(10000)
  } else {
    sleep(5000)
  }
}

class StartConfig {
  String startOption
  String buildName
  List parameters = new ArrayList()
  String sinceBuild
  String sourceControlBranch
}
