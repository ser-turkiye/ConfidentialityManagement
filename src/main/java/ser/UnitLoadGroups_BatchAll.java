package ser;

import com.ser.blueline.IInformationObject;
import com.ser.foldermanager.IFolder;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;


public class UnitLoadGroups_BatchAll extends UnifiedAgent {
    Logger log = LogManager.getLogger();
    ProcessHelper processHelper;
    @Override
    protected Object execute() {
        UnitLoadGroups.ses = getSes();
        UnitLoadGroups.bpm = getBpm();
        UnitLoadGroups.srv = UnitLoadGroups.ses.getDocumentServer();

        try {
            processHelper = new ProcessHelper(UnitLoadGroups.ses);
            IInformationObject[] list = getAllOrgUnits();
            for(IInformationObject item : list){
                System.out.println("****** : " + item.getDisplayName());
                UnitLoadGroups.update((IFolder) item);
            }

            log.info("Tested.");
        } catch (Exception e) {
            //throw new RuntimeException(e);
            log.error("Exception       : " + e.getMessage());
            log.error("    Class       : " + e.getClass());
            log.error("    Stack-Trace : " + e.getStackTrace().toString());
            return resultRestart("Exception : " + e.getMessage(),10);
        }

        log.info("Finished");
        return resultSuccess("Ended successfully");

    }

    private IInformationObject[] getAllOrgUnits() {
        String whereClause = "TYPE = '" + Conf.ClassIDs.OrgUnit + "'";
        return processHelper.createQuery(new String[]{Conf.Databases.Main}, whereClause, "", 0, true);
    }
}