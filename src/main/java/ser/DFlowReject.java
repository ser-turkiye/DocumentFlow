package ser;

import com.ser.blueline.IDocument;
import com.ser.blueline.IInformationObject;
import com.ser.blueline.IUser;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.ITask;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class DFlowReject extends UnifiedAgent {
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
            IUser processOwner = processInstance.getOwner();
            processInstance.setDescriptorValue("ObjectStatus", "Rejected");
            processInstance.commit();

            log.info("REJECT COMMITTED");

            String uniqueId = UUID.randomUUID().toString();
            String mtpn = "PROCESS_REJECT_MAIL_TEMPLATE";
            JSONObject dbks = new JSONObject();
            this.helper = new ProcessHelper(Utils.session);
            JSONObject mcfg = Utils.getMailConfig();
            dbks.put("DoxisLink", mcfg.getString("webBase") + helper.getTaskURL(processInstance.getID()));
            dbks.put("Title", processInstance.getDisplayName());

            String tplMailPath = Conf.Paths.MainPath + "/" + mtpn + ".html";

            File htmlTemplateFile = new File(tplMailPath);
            String htmlString = FileUtils.readFileToString(htmlTemplateFile);
            htmlString = htmlString.replace("$ProcessName", processInstance.getDisplayName());
            htmlString = htmlString.replace("$DoxisLink", mcfg.getString("webBase") + helper.getTaskURL(processInstance.getID()));
            File newHtmlFile = new File(Conf.Paths.MainPath + "/" + mtpn + "[" + uniqueId + "].html");
            FileUtils.writeStringToFile(newHtmlFile, htmlString);

            String umail = processOwner.getEMailAddress();
            List<String> mails = new ArrayList<>();
            log.info("Mail To : " + String.join(";", mails));
            if (umail != null) {
                mails.add(umail);
                JSONObject mail = new JSONObject();
                mail.put("To", String.join(";", mails));
                mail.put("Subject", "Rejected Process");
                mail.put("BodyHTMLFile", Conf.Paths.MainPath + "/" + mtpn + "[" + uniqueId + "].html");
                Utils.sendHTMLMail(mail);
            } else {
                log.info("Mail adress is null :" + processOwner.getFullName());
            }

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