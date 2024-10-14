package ser;

import com.ser.blueline.IInformationObject;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.ITask;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Arrays;
import java.util.List;

public class DFlowApprovalStart extends UnifiedAgent {
    Logger log = LogManager.getLogger();
    IProcessInstance processInstance;
    ProcessHelper helper;
    ITask task;
    IInformationObject document;
    String code;
    @Override
    protected Object execute() {
        if (getEventTask() == null)
            return resultError("Null Document object");

        if(getEventTask().getProcessInstance().findLockInfo().getOwnerID() != null){
            return resultRestart("Locked process instance ...");
        }
        //Utils.loadDirectory(Conf.Paths.MainPath);

        Utils.session = getSes();
        Utils.bpm = getBpm();
        Utils.server = Utils.session.getDocumentServer();

        task = getEventTask();
        processInstance = task.getProcessInstance();
        //processInstance.lock(task);

        try {

            helper = new ProcessHelper(Utils.session);
            XTRObjects.setSession(Utils.session);

            document = Utils.getProcessDocument(processInstance);
            if(document == null){throw new Exception("Process Document not found.");}

            List<String> aprs = Utils.getApprovers(processInstance);
            if(aprs == null || aprs.isEmpty()){throw new Exception("Approver(s) not found.");}

            processInstance.setDescriptorValue("ObjectStatus", "Approval");
            processInstance.setDescriptorValues("_Approvers", aprs);
            processInstance.setDescriptorValues("_Approves", Arrays.asList(""));
            processInstance.setDescriptorValue("_Approveds", "");

            String pttl = "";
            if(Utils.hasDescriptor(document, "ObjectSubject")){
                pttl = document.getDescriptorValue("ObjectSubject", String.class);
                pttl = (pttl == null ? "" : pttl);
            }
            if(!pttl.isBlank()) {
                processInstance.setSubject(pttl);
            }
            Utils.saveComment(processInstance, task, "Start-Approval");
            Utils.updateLinksTaskInfo(null, processInstance, "Start-Approval");
            processInstance.commit();

            log.info("Tested.");

        } catch (Exception e) {
            //processInstance.unlock();
            //throw new RuntimeException(e);
            log.error("Exception       : " + e.getMessage());
            log.error("    Class       : " + e.getClass());
            log.error("    Stack-Trace : " + Arrays.toString(e.getStackTrace()));
            return resultError("Exception : " + e.getMessage());
        }

        //processInstance.unlock();
        log.info("Finished");
        return resultSuccess("Ended successfully");
    }
}