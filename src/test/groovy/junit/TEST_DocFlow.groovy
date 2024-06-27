package junit

import de.ser.doxis4.agentserver.AgentExecutionResult
import org.junit.*
import ser.DFlowComment
import ser.DFlowNext
import ser.DFlowReject
import ser.DFlowStart

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
        def agent = new DFlowReject()

        binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST09BPM_DFLOW244c66a921-d41e-48bd-8323-219ed4361e52182024-06-14T08:30:51.092Z018"

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
