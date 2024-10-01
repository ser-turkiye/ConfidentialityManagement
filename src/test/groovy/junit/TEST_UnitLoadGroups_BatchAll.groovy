package junit

import de.ser.doxis4.agentserver.AgentExecutionResult
import org.junit.*
import ser.UnitLoadGroups
import ser.UnitLoadGroups_BatchAll

class TEST_UnitLoadGroups_BatchAll {

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
        def agent = new UnitLoadGroups_BatchAll();

        binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SR04BPWS24b61b3dbb-7021-40e6-ae51-362c0ea0a50b182024-09-26T09:25:14.488Z011"

        def result = (AgentExecutionResult) agent.execute(binding.variables)
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
