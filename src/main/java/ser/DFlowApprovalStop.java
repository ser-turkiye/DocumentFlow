package ser;

import com.ser.blueline.IInformationObject;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.ITask;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;


public class DFlowApprovalStop extends UnifiedAgent {
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

            processInstance.setDescriptorValue("ObjectStatus", "Send-Back");

            processInstance.setDescriptorValues("_Approves", Arrays.asList(""));
            processInstance.setDescriptorValue("_Approveds", "");
            Utils.updateLinksTaskInfo(null, processInstance, "Send-Back");
            Utils.saveComment(processInstance, task, "Send Back");
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