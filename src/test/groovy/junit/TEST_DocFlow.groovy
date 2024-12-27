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
        def agent = new DFlowInitApproval()

        binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST09BPM_DFLOW24cef61db4-a290-4379-8545-c344c771d74b182024-12-27T06:22:00.838Z016"

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
