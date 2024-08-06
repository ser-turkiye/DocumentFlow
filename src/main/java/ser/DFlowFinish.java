package ser;

import com.ser.blueline.ICommentItem;
import com.ser.blueline.IInformationObject;
import com.ser.blueline.IUser;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.ITask;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.util.Arrays;


public class DFlowFinish extends UnifiedAgent {
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


            document = Utils.getProcessDocument(processInstance);
            if(document == null){throw new Exception("Process Document not found.");}

            processInstance.setDescriptorValue("ObjectStatus", task.getCode());

            JSONObject pcfg = Utils.getProcessConfig(document);

            Utils.saveComment(processInstance, task.getFinishedBy(), code);
            JSONObject bmks = Utils.getProcessBookmarks(task, processInstance, document);
            Utils.sendProcessMail(pcfg, bmks, "Finish." + code);
            processInstance.commit();

        } catch (Exception e) {
            //throw new RuntimeException(e);
            log.error("Exception       : " + e.getMessage());
            log.error("    Class       : " + e.getClass());
            log.error("    Stack-Trace : " + Arrays.toString(e.getStackTrace()));
            return resultError("Exception : " + e.getMessage());
        }

        log.info("Finished");
        return resultSuccess("Ended successfully [" + code + "]");
    }
}