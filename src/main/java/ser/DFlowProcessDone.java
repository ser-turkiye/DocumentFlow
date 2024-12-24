package ser;

import com.ser.blueline.IInformationObject;
import com.ser.blueline.ILink;
import com.ser.blueline.IOrgaElement;
import com.ser.blueline.bpm.IBpmService;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.ITask;
import com.ser.blueline.bpm.IWorkbasket;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class DFlowProcessDone extends UnifiedAgent {
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
        Utils.loadDirectory(Conf.Paths.MainPath);

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
            code = task.getCode();
            IBpmService bpmService = getSes().getBpmService();
            IProcessInstance newPI = bpmService.findProcessInstance(processInstance.getID());
            newPI.setDescriptorValue("ObjectStatus", task.getCode());
            newPI.commit();

            resetInfo(processInstance, task);

            JSONObject pcfg = Utils.getProcessConfig(document);
            //JSONObject bmks = Utils.getProcessBookmarks(task, processInstance, document);
            JSONObject bmks = Utils.getProcessBookmarks(task, processInstance, "processInstance", document, null);
            Utils.sendProcessMail(pcfg, bmks, "Finish." + code);

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
        return resultSuccess("Ended successfully [" + code + "]");
    }
    public static String resetInfo(IProcessInstance processInstance, ITask task){
        List<IInformationObject> list = new ArrayList<>();
        IInformationObject parent = processInstance.getMainInformationObject();
        if(parent != null && !list.stream().filter(o -> o.getID().equals(parent.getID())).findFirst().isPresent()){
            list.add(parent);
        }
        List<ILink> links = processInstance.getLoadedInformationObjectLinks().getLinks();
        for(ILink link : links){
            IInformationObject lino = link.getTargetInformationObject();
            if(lino != null && !list.stream().filter(o -> o.getID().equals(lino.getID())).findFirst().isPresent()){
                list.add(lino);
            }
        }
        for(IInformationObject item : list) {
            if(updInfObjTaskInfo(item, processInstance, task)){
                item.commit();
            }
        }
        return "";
    }
    public static boolean updInfObjTaskInfo(IInformationObject infObj, IProcessInstance proi, ITask task){
        if(!Utils.hasDescriptor(infObj, "ccmPrjDocWFProcessName")){
            return false;
        }
        if(!Utils.hasDescriptor(infObj, "ccmPrjDocWFTaskName")){
            return false;
        }
        if(!Utils.hasDescriptor(infObj, "ccmPrjDocWFTaskCreation")){
            return false;
        }
        if(!Utils.hasDescriptor(infObj, "ccmPrjDocWFTaskRecipients")){
            return false;
        }
        infObj.setDescriptorValue("ccmPrjDocWFProcessName",
                proi.getDisplayName()
        );
        infObj.setDescriptorValue("ccmPrjDocWFTaskName",
                //task.getName() + (task.getCode() != null ? " (" + task.getCode() + ")" : "")
                task.getName()
        );
        infObj.setDescriptorValue("ccmPrjDocWFTaskCreation",
                (task.getCreationDate() == null ? "" : (new SimpleDateFormat("yyyyMMdd")).format(task.getCreationDate()))
        );
        infObj.setDescriptorValue("ccmPrjDocWFTaskRecipients",
                ""
        );

        return true;
    }
}