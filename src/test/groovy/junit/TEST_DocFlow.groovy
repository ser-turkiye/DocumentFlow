package junit

import de.ser.doxis4.agentserver.AgentExecutionResult
import org.junit.*
import ser.*

class TEST_DocFlow {

    Binding binding

    @BeforeClass
    static void initSessionPool() {
        AgentTester.initSessionPool()
    }

    @Before
    void retrieveBinding() {
        binding = AgentTester.retrieveBinding()
    }

    @Test
    void testForAgentResult() {
        def agent = new DFlowNewTask()

        binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST09BPM_DFLOW240142c8ab-4308-435b-98ed-bddf117cd9c3182024-12-23T15:37:05.604Z015"

        def result = (AgentExecutionResult)agent.execute(binding.variables)
        assert result.resultCode == 0
    }

    @Test
    void testForJavaAgentMethod() {
        //def agent = new JavaAgent()
        //agent.initializeGroovyBlueline(binding.variables)
        //assert agent.getServerVersion().contains("Linux")
    }

    @After
    void releaseBinding() {
        AgentTester.releaseBinding(binding)
    }

    @AfterClass
    static void closeSessionPool() {
        AgentTester.closeSessionPool()
    }
}
