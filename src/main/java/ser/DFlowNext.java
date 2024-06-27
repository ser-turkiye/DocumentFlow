package ser;

import com.ser.blueline.IInformationObject;
import com.ser.blueline.ILink;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.ITask;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class DFlowNext extends UnifiedAgent {
    Logger log = LogManager.getLogger();
    IProcessInstance processInstance;
    IInformationObject qaInfObj;
    ProcessHelper helper;
    ITask task;
    IInformationObject document;
    String compCode;
    String reqId;
    @Override
    protected Object execute() {
        if (getEventTask() == null)
            return resultError("Null Document object");

        if(getEventTask().getProcessInstance().findLockInfo().getOwnerID() != null){
            return resultRestart("Restarting Agent");
        }

        Utils.session = getSes();
        Utils.bpm = getBpm();
        Utils.server = Utils.session.getDocumentServer();
        Utils.loadDirectory(Conf.Paths.MainPath);
        
        task = getEventTask();

        try {

            helper = new ProcessHelper(Utils.session);
            XTRObjects.setSession(Utils.session);

            processInstance = task.getProcessInstance();

            /*
            document = Utils.getProcessDocument(processInstance);
            */

            List<String> aprs = processInstance.getDescriptorValues("_Approvers", String.class);
            aprs = (aprs == null ? new ArrayList<>() : aprs);
            List<String> naps = new ArrayList<>();
            String capr = "";
            for(String appr : aprs){
                if(appr == null || appr.isEmpty()){continue;}
                if(capr.isEmpty()){
                    capr = appr;
                    continue;
                }
                naps.add(appr);
            }
            processInstance.setDescriptorValue("_CurrentApprover", capr);
            processInstance.setDescriptorValue("_Approvers", "");
            if(naps.size() > 0) {
                processInstance.setDescriptorValues("_Approvers", naps);
            }
            processInstance.commit();

            log.info("Tested.");

        } catch (Exception e) {
            //throw new RuntimeException(e);
            log.error("Exception       : " + e.getMessage());
            log.error("    Class       : " + e.getClass());
            log.error("    Stack-Trace : " + Arrays.toString(e.getStackTrace()));
            return resultError("Exception : " + e.getMessage());
        }

        log.info("Finished");
        return resultSuccess("Ended successfully");
    }
}