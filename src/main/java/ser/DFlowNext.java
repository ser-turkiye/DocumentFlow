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
    String code;
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
        code = task.getCode();

        try {

            helper = new ProcessHelper(Utils.session);
            XTRObjects.setSession(Utils.session);

            processInstance = task.getProcessInstance();

            List<String> apds = processInstance.getDescriptorValues("_Approves", String.class);
            apds = (apds == null ? new ArrayList<>() : apds);

            String capd = processInstance.getDescriptorValue("_CurrentApprover", String.class);
            capd = (capd == null ? ""  : capd);
            if(!capd.isBlank()){apds.add(capd);}


            processInstance.setDescriptorValues("_Approves", Arrays.asList(""));
            processInstance.setDescriptorValue("_Approveds", "");

            if(apds != null && apds.size() > 0) {
                processInstance.setDescriptorValues("_Approves", apds);
                processInstance.setDescriptorValue("_Approveds", String.join(";", apds));
            }

            List<String> aprs = processInstance.getDescriptorValues("_Approvers", String.class);
            aprs = (aprs == null ? Arrays.asList("") : aprs);

            List<String> naps = new ArrayList<>();
            String capr = "";
            for(String appr : aprs){
                if(appr == null || appr.isBlank()){continue;}
                if(capr.isBlank()){
                    capr = appr;
                    continue;
                }
                naps.add(appr);
            }
            if(naps == null || naps.size() == 0){naps = Arrays.asList("");}

            if(!capr.isBlank()){
                Utils.saveComment(processInstance, task.getFinishedBy(), "Approval");
            }

            processInstance.setDescriptorValue("_CurrentApprover", capr);
            processInstance.setDescriptorValues("_Approvers", naps);
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