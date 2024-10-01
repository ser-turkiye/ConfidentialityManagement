package junit

import de.ser.doxis4.agentserver.AgentExecutionResult
import org.junit.*
import ser.DocumentUpdate
import ser.UnitLoadGroups

class TEST_UnitLoadGroups {

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
        def agent = new UnitLoadGroups();

        binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SR04BPWS244b2c678a-47ab-4eff-b9b9-a4767742b5db182024-10-01T08:12:27.778Z011"

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
