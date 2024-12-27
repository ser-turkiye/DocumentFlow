package ser;

import com.ser.blueline.IInformationObject;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.ITask;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.util.*;


public class DFlowLaunchReview extends UnifiedAgent {
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

            List<String> rvws = Utils.getReviewers(processInstance);
            if(rvws == null || rvws.size() == 0){throw new Exception("Reviewer(s) not found.");}

            processInstance.setDescriptorValues("_Reviewers", rvws);

            String pttl = "Process No:" + processInstance.getNumericID() ;
            if(Utils.hasDescriptor(document, "ObjectSubject")){
                pttl += document.getDescriptorValue("ObjectSubject", String.class);
               // pttl += (pttl == null ? "" : " / " + pttl);
            }
           // if(!pttl.isBlank()) {
                processInstance.setSubject(pttl);
            //}
            Utils.saveComment(processInstance,  task, "Launch-Review");
            Utils.linkedDocUpdate(processInstance, rvws);
            Utils.copyDescriptors(document, processInstance);
            processInstance.commit();

            JSONObject pcfg = Utils.getProcessConfig(document);
            //JSONObject bmks = Utils.getProcessBookmarks(task, processInstance, document);
            JSONObject bmks = Utils.getProcessBookmarks(task, processInstance, "processInstance", document, null);
            Utils.sendProcessMail(pcfg, bmks, "Finish.Done");

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