package ser;

import com.ser.blueline.*;
import com.ser.blueline.bpm.*;
import com.ser.blueline.metaDataComponents.IArchiveClass;
import com.ser.blueline.metaDataComponents.IArchiveFolderClass;
import com.ser.blueline.metaDataComponents.IStringMatrix;
import com.ser.foldermanager.*;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Utils {
    static Logger log = LogManager.getLogger();
    static ISession session = null;
    static IDocumentServer server = null;
    static IBpmService bpm;
    static JSONObject processCfgs;
    static void loadDirectory(String path) {
        (new File(path)).mkdir();
    }


    static List<String> getReviewers(IProcessInstance proc){
        List<String> rtrn = new ArrayList<>();

        for(int i=1;i<99;i++){
            String ix = StringUtils.right("00" + i, 2);
            String fldx = "_Approver" + ix;
            if(!Utils.hasDescriptor(proc, fldx)){break;}

            String sval = proc.getDescriptorValue(fldx, String.class);
            if(sval == null || sval.isEmpty()){continue;}
            if(rtrn.contains(sval)){continue;}
            rtrn.add(sval);
        }

        if(Utils.hasDescriptor(proc, "_OtherReviewers")){
            List<String> orvs = proc.getDescriptorValues("_OtherReviewers", String.class);
            orvs = (orvs == null ? new ArrayList<>() : orvs);

            for(String orvw : orvs){
                if(orvw == null || orvw.isBlank()){continue;}
                if(rtrn.contains(orvw)){continue;}
                rtrn.add(orvw);
            }
        }

        return rtrn;
    }
    static List<String> getAbacOrgaReadAdds(IInformationObject docu){
        List<String> rtrn = new ArrayList<>();

        if(Utils.hasDescriptor(docu, "AbacOrgaReadAdd")){
            List<String> orvs = docu.getDescriptorValues("AbacOrgaReadAdd", String.class);
            orvs = (orvs == null ? new ArrayList<>() : orvs);

            for(String orvw : orvs){
                if(orvw == null || orvw.isBlank()){continue;}
                if(rtrn.contains(orvw)){continue;}
                rtrn.add(orvw);
            }
        }

        return rtrn;
    }
    static List<String> getApprovers(IProcessInstance proc){
        List<String> rtrn = new ArrayList<>();

        for(int i=1;i<99;i++){
            String ix = StringUtils.right("00" + i, 2);
            String fldx = "_Approver" + ix;
            if(!Utils.hasDescriptor(proc, fldx)){break;}

            String sval = proc.getDescriptorValue(fldx, String.class);
            if(sval == null || sval.isEmpty()){continue;}
            rtrn.add(sval);
        }

        return rtrn;
    }
    static IInformationObject getProcessDocument(IProcessInstance processInstance) throws Exception{
        IInformationObject rtrn = processInstance.getMainInformationObject();
        if(rtrn == null){
            List<ILink> aLnks = processInstance.getLoadedInformationObjectLinks().getLinks();
            rtrn = (!aLnks.isEmpty() ? aLnks.get(0).getTargetInformationObject() : rtrn);
            if(rtrn != null) {
                processInstance.setMainInformationObjectID(rtrn.getID());
            }
        }
        if(rtrn == null){
            throw new Exception("Document not found.");
        }
        return rtrn;
    }

    public static String updateLinksTaskInfo(IUser cusr, IProcessInstance processInstance, String taskName){
        String orgaName = (cusr != null ? cusr.getFullName() : "");

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
            if(updInfObjTaskInfo(item, processInstance, taskName, orgaName)){
                item.commit();
            }
        }
        return "";
    }
    public static boolean updInfObjTaskInfo(IInformationObject infObj, IProcessInstance proi, String taskName, String orgaName){
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
                taskName
        );
        infObj.setDescriptorValue("ccmPrjDocWFTaskCreation",
                (new SimpleDateFormat("yyyyMMdd")).format(new Date())
        );
        infObj.setDescriptorValue("ccmPrjDocWFTaskRecipients",
                orgaName
        );

        return true;
    }
    public static void linkedDocUpdate(IProcessInstance processInstance, List<String> wbcs){
        if(wbcs == null || wbcs.isEmpty()){return;}

        //List<String> lids = new ArrayList<>();
        List<IInformationObject> list = new ArrayList<>();
        IInformationObject parent = processInstance.getMainInformationObject();
        if(parent != null && !list.stream().filter(o -> o.getID().equals(parent.getID())).findFirst().isPresent()){
            list.add(parent);
            //lids.add(parent.getID());
        }
        List<ILink> links = processInstance.getLoadedInformationObjectLinks().getLinks();
        for(ILink link : links){
            IInformationObject lino = link.getTargetInformationObject();
            if(lino != null && !list.stream().filter(o -> o.getID().equals(lino.getID())).findFirst().isPresent()){
                list.add(lino);
                //lids.add(parent.getID());
            }
        }
        List<String> readers = new ArrayList<>();
        for(String wbc1 : wbcs){
            IWorkbasket wb = Utils.bpm.getWorkbasket(wbc1);
            if(wb.getAssociatedOrgaElement() == null){continue;}
            readers.add(wb.getAssociatedOrgaElement().getName());
        }
        if(readers == null || readers.isEmpty()){return;}

        for(IInformationObject item : list){
            boolean update = false;
            if(item.getClassID().equals(Conf.DocClasses.OrgUnitDocs)){
                if(Utils.hasDescriptor(item, "AbacOrgaReadAdd")) {
                    List<String> rds1 = item.getDescriptorValues("AbacOrgaReadAdd", String.class);
                    rds1 = (rds1 != null && !rds1.isEmpty() ? rds1 : new ArrayList<>());

                    rds1 = Stream.of(rds1, readers)
                            .flatMap(java.util.Collection::stream).collect(Collectors.toList());

                    Set<String> setRtrn = new HashSet<>(rds1);
                    rds1.clear();
                    rds1.addAll(setRtrn);

                    item.setDescriptorValues("AbacOrgaReadAdd", rds1);
                    update = true;
                }
            }
            if(update){
                item.commit();
            }
        }
    }
    public static boolean hasDescriptor(IInformationObject object, String descName){
        IDescriptor[] descs = session.getDocumentServer().getDescriptorByName(descName, session);
        List<String> checkList = new ArrayList<>();
        for(IDescriptor ddsc : descs){checkList.add(ddsc.getId());}

        String[] descIds = new String[0];
        if(object instanceof IFolder){
            String classID = object.getClassID();
            IArchiveFolderClass folderClass = session.getDocumentServer().getArchiveFolderClass(classID , session);
            descIds = folderClass.getAssignedDescriptorIDs();
        } else if(object instanceof IDocument){
            IArchiveClass documentClass = ((IDocument) object).getArchiveClass();
            descIds = documentClass.getAssignedDescriptorIDs();
        } else if(object instanceof ITask){
            IProcessType processType = ((ITask) object).getProcessType();
            descIds = processType.getAssignedDescriptorIDs();
        } else if(object instanceof IProcessInstance){
            IProcessType processType = ((IProcessInstance) object).getProcessType();
            descIds = processType.getAssignedDescriptorIDs();
        }

        List<String> descList = Arrays.asList(descIds);
        for(String dId : descList){if(checkList.contains(dId)){return true;}}
        return false;
    }
    public static boolean connectToFolder(IFolder folder, String rootName, String fold, IInformationObject pdoc) throws Exception {
        boolean add2Node = false;
        List<INode> nodes = folder.getNodesByName(rootName);
        if(nodes.isEmpty()){return false;}

        INodes root = (INodes) nodes.get(0).getChildNodes();
        INode fnod = root.getItemByName(fold);
        if(fnod == null) {return false;}

        boolean isExistElement = false;
        String pdocID = pdoc.getID();
        IElements nelements = fnod.getElements();
        for(int i=0;i<nelements.getCount2();i++) {
            IElement nelement = nelements.getItem2(i);
            String edocID = nelement.getLink();
            if(Objects.equals(pdocID, edocID)){
                isExistElement = true;
                break;
            }
        }
        if(isExistElement) {return false;}

        add2Node = folder.addInformationObjectToNode(pdoc.getID(), fnod.getID());

        folder.commit();
        return add2Node;
    }
    public static boolean disconnectToFolder(IFolder folder, String rootName, String fold, IInformationObject pdoc) throws Exception {
        boolean add2Node = false;
        List<INode> nodes = folder.getNodesByName(rootName);
        if(nodes.isEmpty()){return false;}

        INodes root = (INodes) nodes.get(0).getChildNodes();
        INode fnod = root.getItemByName(fold);
        if(fnod == null) {return false;}

        int rIndex = -1;
        String pdocID = pdoc.getID();
        IElements nelements = fnod.getElements();
        for(int i=0;i<nelements.getCount2();i++) {
            IElement nelement = nelements.getItem2(i);
            String edocID = nelement.getLink();
            if(!Objects.equals(pdocID, edocID)){
                continue;
            }
            nelements.remove(i);
            rIndex = i;
        }
        if(rIndex<0) {return false;}

        folder.commit();
        return add2Node;
    }
    public static void sendHTMLMail(JSONObject pars) throws Exception {
        JSONObject mcfg = Utils.getMailConfig();

        String host = mcfg.getString("host");
        String port = mcfg.getString("port");
        String protocol = mcfg.getString("protocol");
        String sender = mcfg.getString("sender");
        String subject = "";
        String mailTo = "";
        String mailCC = "";
        String attachments = "";

        if(pars.has("From")){
            sender = pars.getString("From");
        }
        if(pars.has("To")){
            mailTo = pars.getString("To");
        }
        if(pars.has("CC")){
            mailCC = pars.getString("CC");
        }
        if(pars.has("Subject")){
            subject = pars.getString("Subject");
        }
        if(pars.has("AttachmentPaths")){
            attachments = pars.getString("AttachmentPaths");
        }


        log.info("HtmlMail-Send.sender : " + sender);
        log.info("HtmlMail-Send.mailTo : " + mailTo);
        log.info("HtmlMail-Send.subject : " + subject);

        Properties props = new Properties();

        props.put("mail.debug","true");
        props.put("mail.smtp.debug", "true");
        props.put("mail.smtp.debug", "true");

        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        String start_tls = (mcfg.has("start_tls") ? mcfg.getString("start_tls") : "");
        if(start_tls.equals("true")) {
            props.put("mail.smtp.starttls.enable", start_tls);
        }

        String auth = mcfg.getString("auth");
        props.put("mail.smtp.auth", auth);
        Authenticator authenticator = null;
        if(!auth.equals("false")) {
            String auth_username = mcfg.getString("auth.username");
            String auth_password = mcfg.getString("auth.password");

            if (host.contains("gmail")) {
                props.put("mail.smtp.socketFactory.port", port);
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.fallback", "false");
            }
            if (protocol != null && protocol.contains("TLSv1.2"))  {
                props.put("mail.smtp.ssl.protocols", protocol);
                props.put("mail.smtp.ssl.trust", "*");
                props.put("mail.smtp.socketFactory.port", port);
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.fallback", "false");
            }
            authenticator = new Authenticator(){
                @Override
                protected PasswordAuthentication getPasswordAuthentication(){
                    return new PasswordAuthentication(auth_username, auth_password);
                }
            };
        }
        props.put("mail.mime.charset","UTF-8");
        Session session = (authenticator == null ? Session.getDefaultInstance(props) : Session.getDefaultInstance(props, authenticator));

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(sender.replace(";", ",")));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mailTo.replace(";", ",")));
        message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(mailCC.replace(";", ",")));
        message.setSubject(subject);

        Multipart multipart = new MimeMultipart("mixed");

        if(pars.has("BodyHTMLFile")) {
            BodyPart htmlBodyPart02 = new MimeBodyPart();
            htmlBodyPart02.setContent(getHTMLFileContent(pars.getString("BodyHTMLFile")), "text/html; charset=UTF-8"); //5
            multipart.addBodyPart(htmlBodyPart02);
        }
        if(pars.has("BodyHTMLText")) {
            BodyPart htmlBodyPart01 = new MimeBodyPart();
            htmlBodyPart01.setContent(getHTMLContentClear(pars.getString("BodyHTMLText")), "text/html; charset=UTF-8"); //5
            multipart.addBodyPart(htmlBodyPart01);
            log.info("Html-Mail..Content: " + pars.getString("BodyHTMLText"));
        }
        String[] atchs = attachments.split("\\;");
        for (String atch : atchs){
            if(atch.isEmpty()){continue;}
            BodyPart attachmentBodyPart = new MimeBodyPart();
            attachmentBodyPart.setDataHandler(new DataHandler((DataSource) new FileDataSource(atch)));

            String fnam = Paths.get(atch).getFileName().toString();
            if(pars.has("AttachmentName." + fnam)){
                fnam = pars.getString("AttachmentName." + fnam);
            }

            attachmentBodyPart.setFileName(fnam);
            multipart.addBodyPart(attachmentBodyPart);

        }

        message.setContent(multipart);
        Transport.send(message);

    }
    public static String getHTMLFileContent (String path) throws Exception {
        String rtrn = new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
        return getHTMLContentClear(rtrn);
    }
    public static String getHTMLContentClear (String strg) throws Exception {
        String rtrn = strg;
        rtrn = rtrn.replace("\uFEFF", "");
        rtrn = rtrn.replace("ï»¿", "");
        return rtrn;
    }
    public static void copyDescriptors(IInformationObject sour, IInformationObject targ) throws Exception {
        IValueDescriptor[] sdls = sour.getDescriptorList();
        for (IValueDescriptor sdvl : sdls) {
            if (!hasDescriptor(targ, sdvl.getDescriptor().getName())) {
                continue;
            }
            targ.setDescriptorValueTyped(sdvl.getDescriptor().getId(),
                    sour.getDescriptorValue(sdvl.getName())
            );
        }
    }
    public static ITask getLastFinishedTask(IProcessInstance processInstance) throws Exception {
        Collection<ITask> tasks = processInstance.findTasks(TaskStatus.COMPLETED);
        ITask rtrn = null;
        for(ITask task : tasks){
            if(task.getCurrentWorkbasket() == null){continue;}
            if(task.getFinishedDate() == null){continue;}
            if(task.getFinishedBy() == null){continue;}
            if(task.getFinishedBy().getLicenseType() == LicenseType.TECHNICAL_USER){continue;}
            if(rtrn != null && rtrn.getFinishedDate().before(task.getFinishedDate())){continue;}
            rtrn = task;
        }
        return rtrn;
    }
    public static ICommentItem saveComment(IProcessInstance processInstance, ITask ctask, String code) throws Exception {
        String cmmt = processInstance.getDescriptorValue("ObjectDescription", String.class);
        cmmt = (cmmt == null || cmmt.isBlank() ? "" :
                    (code != null && !code.isBlank() ? "(" + code + ") " : "") + cmmt);

        log.info("Save-Comment.01 : " + cmmt);
        if(cmmt.isBlank()) {return null;}

        IUser user = processInstance.getModificator();

        /*
        ITask task = getLastFinishedTask(processInstance);
        task = task == null ? ctask : task;
        IUser user = task.getFinishedBy();
        user = user == null || user.getLicenseType() == LicenseType.TECHNICAL_USER ? processInstance.getModificator() : user;
        if((user == null || user.getLicenseType() == LicenseType.TECHNICAL_USER) && task.getCurrentWorkbasket() != null){
            IWorkbasket wb = task.getCurrentWorkbasket();
            if(wb.getAssociatedOrgaElement() != null) {
                user = (IUser) wb.getAssociatedOrgaElement();
            }
        }
        */

        String xcmt = (user != null ? user.getFullName() + " ## " : "") + cmmt;
        log.info("Save-Comment.02 : " + xcmt);
        IAdditionalItems<ICommentItem> cmts = processInstance.getCommentItems();
        ICommentItem ncmt = server.createCommentItem(session,xcmt, "text/plain");
        cmts.addItem(ncmt);

        log.info("Save-Comment.99 : " + ncmt.hashCode());
        processInstance.setDescriptorValue("ObjectDescription", "");
        return ncmt;
    }
    public static String sendProcessMail(JSONObject pcfg, JSONObject bmks, String mnam) throws Exception {
        if(!pcfg.has("Mail." + mnam)){return "No-Config";}

        log.info("Send-ProcessMail : " + mnam);

        JSONObject mdef = pcfg.getJSONObject("Mail." + mnam);
        JSONObject ndef = new JSONObject();
        for(String keym : mdef.keySet()){
            String kval = mdef.getString(keym);
            for(String keyb : bmks.keySet()) {
                if (kval.contains("{{LINK@" + keyb + "}}")) {
                    kval = kval.replaceAll(Pattern.quote("{{LINK@" + keyb + "}}"),
                            bmks.getString(keyb));
                    //log.info("kval2[{{" + keyb + "}}]::" + kval);
                }
                if (kval.contains("{{LINK@" + keyb + ".html}}")) {
                    String klab = bmks.has(keyb + ".label") && bmks.getString(keyb + ".label") != null &&
                            !bmks.getString(keyb + ".label").isBlank() ? bmks.getString(keyb + ".label") : bmks.getString(keyb);

                    kval = kval.replaceAll(Pattern.quote("{{LINK@" + keyb + ".html}}"),
                            "<a href=\"" + bmks.getString(keyb) + "\">" + klab + "</a>");

                    //log.info("kval1[{{LINK@" + keyb + "}}]::" + kval);
                }
                if (kval.contains("{{" + keyb + "}}")) {
                    kval = kval.replaceAll(Pattern.quote("{{" + keyb + "}}"),
                            bmks.getString(keyb));
                    //log.info("kval2[{{" + keyb + "}}]::" + kval);
                }
            }

            ndef.put(keym, kval);
        }

        try {
            String spth = Conf.Paths.MainPath + "/" + UUID.randomUUID().toString() + "$" + mnam;
            BufferedWriter sjsn = Files.newBufferedWriter(Paths.get(spth + ".json"));
            sjsn.write(ndef.toString());
            sjsn.close();

            if(ndef.has("BodyHTMLText") && ndef.getString("BodyHTMLText") != null && !ndef.getString("BodyHTMLText").isBlank()){
                BufferedWriter shtm = Files.newBufferedWriter(Paths.get(spth + "BodyHTMLText.html"));
                shtm.write(ndef.getString("BodyHTMLText").toString());
                shtm.close();
            }

        } catch(Exception ex){
            log.error("** Save-Mail-Defn    : " + ex.getMessage());
            log.error("              Class       : " + ex.getClass());
            log.error("              Stack-Trace : " + Arrays.toString(ex.getStackTrace()));
        }

        try {
            Utils.sendHTMLMail(ndef);
        } catch(Exception ex){
            log.error("** Send-Mail-Exception    : " + ex.getMessage());
            log.error("              Class       : " + ex.getClass());
            log.error("              Stack-Trace : " + Arrays.toString(ex.getStackTrace()));
        }
        return "";
    }

    public static String getTaskURL(String taskID){
        StringBuilder webcubeUrl = new StringBuilder();
        webcubeUrl.append("?system=").append(session.getSystem().getName());
        webcubeUrl.append("&action=showtask&home=1&reusesession=1&id=").append(taskID);
        return webcubeUrl.toString();
    }
    public static JSONObject getProcessBookmarks(ITask task, IProcessInstance processInstance, String linkType, IInformationObject document, IUser currentApprover) throws Exception {
        JSONObject mcfg = Utils.getMailConfig();

        JSONObject rtrn = new JSONObject("{}");
        rtrn.put("ProcessOwner", "");
        rtrn.put("ProcessName", processInstance.getDisplayName());
        rtrn.put("Approveds", "");
        rtrn.put("Reviewers", "");
        rtrn.put("CurrentApprover", "");
        rtrn.put("Status", "");
        rtrn.put("Comments", "");
        String lnId = processInstance.getID();
        if(linkType.equals("task")){
            lnId = task.getID();
        }
        String tlnk = mcfg.getString("webBase") + getTaskURL(lnId);
        rtrn.put("DoxisLink", tlnk);
        rtrn.put("DoxisLink.address", tlnk);
        rtrn.put("DoxisLink.label", processInstance.getDisplayName());

        IUser processOwner = processInstance.getOwner();
        if(processOwner != null){
            rtrn.put("ProcessOwner", (processOwner.getEMailAddress() != null ? processOwner.getEMailAddress() : ""));
        }
        if(currentApprover != null){
            rtrn.put("CurrentApprover", (currentApprover.getEMailAddress() != null ? currentApprover.getEMailAddress() : ""));
        }

        List<String> apds = processInstance.getDescriptorValues("_Approves", String.class);
        apds = (apds == null ? new ArrayList<>() : apds);

        List<String> mpds = new ArrayList<>();
        if(apds != null && apds.size() > 0) {
            for(String apvd : apds){
                IWorkbasket uwbk = bpm.getWorkbasket(apvd);
                if(uwbk == null){continue;}
                if(uwbk.getAssociatedOrgaElement() == null){continue;}

                if(uwbk.getAssociatedOrgaElement().getOrgaElementType() == OrgaElementType.USER) {
                    IUser upvd = server.getUser(session, uwbk.getAssociatedOrgaElement().getID());
                    if (upvd == null) {
                        continue;
                    }
                    String mpvd = upvd.getEMailAddress();
                    mpvd = (mpvd == null ? "" : mpvd);
                    if (mpvd.isBlank()) {
                        continue;
                    }
                    if (mpds.contains(mpvd)) {
                        continue;
                    }
                    mpds.add(mpvd);
                }
            }
        }
        if(mpds != null && mpds.size() > 0){
            rtrn.put("Approveds", String.join(";", mpds));
        }
        List<String> rwds = processInstance.getDescriptorValues("_Reviewers", String.class);
        rwds = (rwds == null ? new ArrayList<>() : rwds);

        List<String> mrws = new ArrayList<>();
        if(rwds != null && rwds.size() > 0) {
            for(String rvwr : rwds){
                IWorkbasket urvw = bpm.getWorkbasket(rvwr);
                if(urvw == null){continue;}
                if(urvw.getAssociatedOrgaElement() == null){continue;}

                if(urvw.getAssociatedOrgaElement().getOrgaElementType() == OrgaElementType.USER) {
                    IUser usrv = server.getUser(session, urvw.getAssociatedOrgaElement().getID());
                    if (usrv == null) {
                        continue;
                    }
                    String msrv = usrv.getEMailAddress();
                    msrv = (msrv == null ? "" : msrv);
                    if (msrv.isBlank()) {
                        continue;
                    }
                    if (mpds.contains(msrv)) {
                        continue;
                    }
                    mrws.add(msrv);
                }
            }
        }
        if(mrws != null && mrws.size() > 0){
            rtrn.put("Reviewers", String.join(";", mrws));
        }

        if(task.getCode() != null && !task.getCode().isBlank()){
            rtrn.put("Status", task.getCode());
        }

        IAdditionalItems<ICommentItem> cmts = processInstance.getCommentItems();
        List<ICommentItem> lcms = cmts.getItems();

        List<String> mcms = new ArrayList<>();
        if(lcms != null && lcms.size() > 0) {
            for (ICommentItem cmmt : lcms) {
                Date dcm1 = (cmmt.getCreationDate() != null ? cmmt.getCreationDate() : new Date());
                mcms.add((dcm1!= null ? dateTimeToString(dcm1) : "**/**/**** **:**:**") + "\t " +
                        cmmt.getComment());
            }
        }
        if(mcms != null && mcms.size() > 0){
            Collections.reverse(mcms);
            rtrn.put("Comments", String.join("\n\r --- --- --- --- \n\r", mcms));
        }
        //log.info("RTRN..LINK:" + rtrn.get("DoxisLink"));
        return rtrn;
    }
    public static JSONObject getProcessConfig(IInformationObject docu) throws Exception {
        String db = docu.getDatabaseName();
        String cls = docu.getClassID();
        JSONObject pcfs = getAllProcessConfigs();
        if(pcfs.has(db + "@" + cls)){
            return pcfs.getJSONObject(db + "@" + cls);
        }
        if(pcfs.has("*@" + cls)){
            return pcfs.getJSONObject("*@" + cls);
        }
        if(pcfs.has(db + "@*")){
            return pcfs.getJSONObject(db + "@*");
        }
        if(pcfs.has( "*@*")){
            return pcfs.getJSONObject("*@*");
        }
        return new JSONObject();
    }
    public static JSONObject getAllProcessConfigs() throws Exception {
        if(processCfgs != null){return processCfgs;}

        IStringMatrix  mtrx = server.getStringMatrix("DOC_FLOW_PROCESS_CONFIGS", session);
        if(mtrx == null) throw new Exception("SystemConfig Global Value List not found");

        List<List<String>> rawTable = mtrx.getRawRows();

        JSONObject rtrn = new JSONObject();
        for(List<String> line : rawTable) {
            String name = line.get(0) + "";
            name = (name == null ? "" : name);
            if(name.isBlank()){continue;}

            String catg = line.get(1) + "";
            catg = (catg == null ? "" : catg);
            if(catg.isBlank()){continue;}


            String cnfg = line.get(2) + "";
            cnfg = (cnfg == null || cnfg.isBlank() ? "{}" : cnfg);

            JSONObject rctg = (rtrn.has(name) ? rtrn.getJSONObject(name) : new JSONObject());

            rctg.put(catg, new JSONObject(cnfg));
            rtrn.put(name, rctg);
        }
        processCfgs = rtrn;
        return rtrn;
    }
    public static JSONObject getMailConfig() throws Exception {
        IStringMatrix mtrx = server.getStringMatrix("DOC_FLOW_MAIL_CONFIGS", session);
        if(mtrx == null) throw new Exception("MailConfig Global Value List not found");
        List<List<String>> rawTable = mtrx.getRawRows();

        JSONObject rtrn = new JSONObject();
        for(List<String> line : rawTable) {
            rtrn.put(line.get(0), line.get(1));
        }
        return rtrn;
    }
    public static JSONObject getWorkbasket( String userID, IStringMatrix mtrx) throws Exception {
        if(mtrx == null){
            mtrx = server.getStringMatrixByID("Workbaskets", session);
        }
        if(mtrx == null) throw new Exception("Workbaskets Global Value List not found");
        List<List<String>> rawTable = mtrx.getRawRows();

        for(List<String> line : rawTable) {
            if(line.contains(userID)) {
                JSONObject rtrn = new JSONObject();
                rtrn.put("ID", line.get(0));
                rtrn.put("Name", line.get(1));
                rtrn.put("DisplayName", line.get(2));
                rtrn.put("Active", line.get(3));
                rtrn.put("Visible", line.get(4));
                rtrn.put("Type", line.get(5));
                rtrn.put("Organization", line.get(6));
                rtrn.put("Access", line.get(7));
                return rtrn;
            }
        }
        return null;
    }
    public static void copyFile(String spth, String tpth) throws Exception {
        FileUtils.copyFile(new File(spth), new File(tpth));
    }
    public static String dateToString(Date dval) throws Exception {
        if(dval == null) return "";
        return new SimpleDateFormat("dd/MM/yyyy").format(dval);
    }
    public static String dateTimeToString(Date dval) throws Exception {
        if(dval == null) return "";
        return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(dval);
    }
    public static String zipFiles(String zipPath, String pdfPath, List<String> expFilePaths) throws IOException {
        if(expFilePaths.size() == 0){return "";}

        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(new File(zipPath)));
        if(!pdfPath.isEmpty()) {
            //ZipEntry zltp = new ZipEntry("00." + Paths.get(tpltSavePath).getFileName().toString());
            ZipEntry zltp = new ZipEntry("_Correspondence." + FilenameUtils.getExtension(pdfPath));
            zout.putNextEntry(zltp);
            byte[] zdtp = Files.readAllBytes(Paths.get(pdfPath));
            zout.write(zdtp, 0, zdtp.length);
            zout.closeEntry();
        }

        for (String expFilePath : expFilePaths) {
            String fileName = Paths.get(expFilePath).getFileName().toString();
            fileName = fileName.replace("[@SLASH]", "/");
            ZipEntry zlin = new ZipEntry(fileName);

            zout.putNextEntry(zlin);
            byte[] zdln = Files.readAllBytes(Paths.get(expFilePath));
            zout.write(zdln, 0, zdln.length);
            zout.closeEntry();
        }
        zout.close();
        return zipPath;
    }
    public static String exportDocument(IDocument document, String exportPath, String fileName) throws IOException {
        String rtrn ="";
        IDocumentPart partDocument = document.getPartDocument(document.getDefaultRepresentation() , 0);
        String fName = (!fileName.isEmpty() ? fileName : partDocument.getFilename());
        fName = fName.replaceAll("[\\\\/:*?\"<>|]", "_");

        try (InputStream inputStream = partDocument.getRawDataAsStream()) {
            IFDE fde = partDocument.getFDE();
            if (fde.getFDEType() == IFDE.FILE) {
                rtrn = exportPath + "/" + fName + "." + ((IFileFDE) fde).getShortFormatDescription();

                try (FileOutputStream fileOutputStream = new FileOutputStream(rtrn)){
                    byte[] bytes = new byte[2048];
                    int length;
                    while ((length = inputStream.read(bytes)) > -1) {
                        fileOutputStream.write(bytes, 0, length);
                    }
                }
            }
        }
        return rtrn;
    }
    public static String exportRepresentation(IDocument document, int rinx, String exportPath, String fileName) throws IOException {
        String rtrn ="";
        IDocumentPart partDocument = document.getPartDocument(rinx , 0);
        String fName = (!fileName.isEmpty() ? fileName : partDocument.getFilename());
        fName = fName.replaceAll("[\\\\/:*?\"<>|]", "_");
        try (InputStream inputStream = partDocument.getRawDataAsStream()) {
            IFDE fde = partDocument.getFDE();
            if (fde.getFDEType() == IFDE.FILE) {
                rtrn = exportPath + "/" + fName + "." + ((IFileFDE) fde).getShortFormatDescription();

                try (FileOutputStream fileOutputStream = new FileOutputStream(rtrn)){
                    byte[] bytes = new byte[2048];
                    int length;
                    while ((length = inputStream.read(bytes)) > -1) {
                        fileOutputStream.write(bytes, 0, length);
                    }
                }
            }
        }
        return rtrn;
    }

}
