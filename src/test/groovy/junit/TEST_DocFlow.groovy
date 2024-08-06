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
        def agent = new DFlowFinish()

        binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST09BPM_DFLOW2407ab9881-b92b-4269-b3d9-8fe6ba5d6845182024-07-09T09:14:26.773Z0213"

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
