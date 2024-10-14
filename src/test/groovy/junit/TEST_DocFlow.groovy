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
        def agent = new DFlowApprovalNext()

        binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST09BPM_DFLOW24dd3af39c-bc31-4d9f-9fc3-7ec6ce0dafa5182024-10-11T16:00:45.653Z019"

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
