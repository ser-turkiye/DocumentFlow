package ser;

import com.ser.blueline.IInformationObject;
import com.ser.blueline.ILink;
import com.ser.blueline.IUser;
import com.ser.blueline.OrgaElementType;
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
import java.util.Date;
import java.util.List;


public class DFlowApprovalNext extends UnifiedAgent {
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

            List<String> apds = processInstance.getDescriptorValues("_Approves", String.class);
            apds = (apds == null ? new ArrayList<>() : apds);

            String capd = processInstance.getDescriptorValue("_CurrentApprover", String.class);
            capd = (capd == null ? ""  : capd);
            if(!capd.isBlank()){apds.add(capd);}

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

            IUser cusr = null;
            String cmad = "";
            if(!capr.isBlank()) {
                Utils.linkedDocUpdate(processInstance, Arrays.asList(capr));
                IWorkbasket cwb = Utils.bpm.getWorkbasket(capr);
                if(cwb != null && cwb.getAssociatedOrgaElement() != null
                && cwb.getAssociatedOrgaElement().getOrgaElementType() == OrgaElementType.USER){
                    cusr = (IUser) cwb.getAssociatedOrgaElement();
                }
            }

            //ITask ctask = Utils.getLastFinishedTask(processInstance);
            Utils.updateLinksTaskInfo(cusr, processInstance, "Waiting For Approval");

            processInstance.setDescriptorValues("_Approves", Arrays.asList(""));
            processInstance.setDescriptorValue("_Approveds", "");

            if(apds != null && apds.size() > 0) {
                processInstance.setDescriptorValues("_Approves", apds);
                processInstance.setDescriptorValue("_Approveds", String.join(";", apds));
            }

            processInstance.setDescriptorValue("_CurrentApprover", capr);
            processInstance.setDescriptorValues("_Approvers", naps);

            Utils.saveComment(processInstance, task, "Approval");
            processInstance.commit();

            String pttl = "";
            if(Utils.hasDescriptor(document, "ObjectSubject")){
                pttl = document.getDescriptorValue("ObjectSubject", String.class);
                pttl = (pttl == null ? "" : pttl);
            }
            task.setDescriptorValue("ObjectSubject",
                    pttl
            );
            task.setSubject(pttl);
            task.commit();

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