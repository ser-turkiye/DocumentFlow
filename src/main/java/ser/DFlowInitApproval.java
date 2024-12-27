package ser;

import com.ser.blueline.IInformationObject;
import com.ser.blueline.ILink;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.ITask;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;


public class DFlowInitApproval extends UnifiedAgent {
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

            IProcessInstance newApproval = helper.buildNewProcessInstanceForID(Conf.ProcessInstances.Approval);
            //newApproval.setMainInformationObjectID(processInstance.getID());
            Utils.copyDescriptors(processInstance, newApproval);
            newApproval.getLoadedInformationObjectLinks().addInformationObject(document.getID());
            //newApproval.getLoadedInformationObjectLinks().addInformationObject(processInstance.getID());
            newApproval.setOwner(processInstance.getOwner());

            //List<String> rvws = Utils.getReviewers(processInstance);
            newApproval.commit();

            //processInstance.getLoadedInformationObjectLinks().addInformationObject(newApproval.getID());
            processInstance.commit();

            //Utils.server.createLink(Utils.session, task.getID(), null, newApproval.getID()).commit();
            //Utils.server.createLink(Utils.session, processInstance.getID(), null, newApproval.getID()).commit();
            //Utils.server.createLink(Utils.session, newApproval.getID(), null, processInstance.getID()).commit();

            log.info("Tested.");

        } catch (Exception e) {
            //processInstance.unlock();
            //throw new RuntimeException(e);
            log.error("Exception       : " + e.getMessage());
            log.error("    Class       : " + e.getClass());
            log.error("    Stack-Trace : " + Arrays.toString(e.getStackTrace()));
            return resultRestart("Exception : " + e.getMessage());
        }

        //processInstance.unlock();
        log.info("Finished");
        return resultSuccess("Ended successfully");
    }
}