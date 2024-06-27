package ser;

import com.ser.blueline.*;
import com.ser.blueline.bpm.IBpmService;
import com.ser.blueline.bpm.IWorkbasket;

import java.io.File;
import java.util.*;
import java.util.function.Function;


public class XTRObjects {
    public static String uniqueId = UUID.randomUUID().toString();
    public static String classNameIInformationObject = "com.ser.internal.foldermanagerimpl.sedna.Folder";
    public static String classNameIDocument = "com.ser.sedna.client.bluelineimpl.document.Document";
    public static String classNameITask = "com.ser.sedna.client.bluelineimpl.bpm.Task";
    public static String classNameIProcessInstance = "com.ser.sedna.client.bluelineimpl.bpm.ProcessInstance";
    public static String patternDefaultDate = "dd.MM.yyyy";
    public static String patternDefaultTime = "HH:mm:ss";
    public static String patternDefaultDateTime = "dd.MM.yyyy HH:mm:ss";
    public static List<String> patternsDateTime = new ArrayList<>(Arrays.asList(
            "yyyyMMdd", "yyyy/MM/dd", "dd/MM/yyyy", "MM/dd/yyyy",
            "yyyy.MM.dd", "dd.MM.yyyy", "MM.dd.yyyy",
            "yyyy-MM-dd", "dd-MM-yyyy", "MM-dd-yyyy",
            "yyyy.MM.dd HH:mm:ss", "dd.MM.yyyy HH:mm:ss", "MM.dd.yyyy HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss", "dd-MM-yyyy HH:mm:ss", "MM-dd-yyyy HH:mm:ss",
            "yy","yyyy","M","MM","d","dd","H","HH","m","mm","s","ss"
    ));
    static ISession session;
    static IDocumentServer server;
    static IBpmService bpm;
    static String principalName;
    static String exportPath;

    public static void deleteDocument(IInformationObject docu) throws Exception {
        server.deleteDocument(session, (IDocument) docu);
    }
    public static void copyDescriptors(IInformationObject sour, IInformationObject targ) throws Exception {
        IValueDescriptor[] sdls = sour.getDescriptorList();
        for(IValueDescriptor sdvl : sdls){
            targ.setDescriptorValueTyped(sdvl.getDescriptor().getId(),
                sour.getDescriptorValue(sdvl.getName())
            );
        }
    }
    public static void createLink(IInformationObject o1, IInformationObject o2) throws Exception {
        ILink link = server.createLink(session, o1.getID(), null, o2.getID());
        link.commit();
    }
    public static IGroup createGroup(String gpnm) throws Exception {
        ISerClassFactory scf = server.getClassFactory();
        return scf.createGroupInstance(session, gpnm);
    }
    public static IWorkbasket createWorkbasket(IOrgaElement orga) throws Exception {
        return Utils.bpm.createWorkbasketObject(orga);
    }
    public static IWorkbasket getFirstWorkbasket(IOrgaElement orga) throws Exception {
        Set<IWorkbasket> wgrs = Utils.bpm.findAccessibleWorkbaskets(orga, true);
        Optional<IWorkbasket> fwbk = wgrs.stream().findFirst();
        if (fwbk.isPresent()) {
            return fwbk.get();
        }
        return null;
    }
    public static IGroup findGroup(String gpnm) throws Exception {
        if(gpnm.trim().equals("")){return null;}
        return server.getGroupByName(session, gpnm);
    }
    public static IUser getDocCreatorUser(IDocument document) throws Exception {
        if(document.getDocumentCreator() == null){return null;}
        return server.getUser(session, document.getDocumentCreator().getUserID());
    }

    public static boolean hasGroupMembers(IUser user, String grpName) throws Exception {
        String[] gIds = user.getGroupIDs();
        for(String gpId : gIds){
            IGroup mgrp = server.getGroup(session, gpId);
            if(mgrp == null){continue;}
            if(!mgrp.getName().equals(grpName)){continue;}
            return true;
        }
        return false;
    }

    public static void setBpm(IBpmService bpm) throws Exception {
        XTRObjects.bpm = bpm;
    }

    public static void setSession(ISession ses) throws Exception {
        XTRObjects.session = ses;
        XTRObjects.server = XTRObjects.session.getDocumentServer();
        XTRObjects.principalName = XTRObjects.server.getServicePrincipalName();
    }
    public static void setExportPath(String exportPath) throws Exception {
        XTRObjects.exportPath = (exportPath + "/" + XTRObjects.uniqueId).replace("//", "/");
            (new File(XTRObjects.exportPath)).mkdirs();
    }
    public static IDatabase getDatabase(String dbn) throws Exception {
        return XTRObjects.getDatabase((db) -> {
            if(db.getName().equals(dbn)){
                return db;
            }
            return null;
        });
    }
    public static IDatabase getDatabase(Function <IDatabase, IDatabase> func) throws Exception {
        IDatabase[] dbs = session.getDatabases();
        int c = 0;
        for(IDatabase db : dbs){
            if(func.apply(db) == null){continue;}
            return db;
        }
        return null;
    }
    public static IDescriptor getDescriptor(String dn) throws Exception {
        return server.getDescriptorForName(session, dn);
    }
    public static void callDatabases(Function <IDatabase, Boolean> func) throws Exception {
        IDatabase[] dbs = session.getDatabases();
        int c = 0;
        for(IDatabase db : dbs){
            if(!func.apply(db)){break;}
        }
    }
    public static ISession getSession(){
        return XTRObjects.session;
    }
    public static IDocumentServer getServer(){
        return XTRObjects.server;
    }
    public static String getPrincipalName(){
        return XTRObjects.principalName;
    }
    public static String getExportPath(){
        return XTRObjects.exportPath;
    }
    public static NumberRange numberRange() throws Exception{
        return NumberRange.init();
    }
    public static NumberRange numberRange(Object obj) throws Exception{
        return XTRObjects.numberRange().with(obj);
    }
    public static InfoObject infoObject(Object obj) throws Exception{
        return InfoObject.init(obj);
    }
    public static AutoText autoText() throws Exception{
        return AutoText.init();
    }
    public static AutoText autoText(Object obj) throws Exception{
        return XTRObjects.autoText().with(obj);
    }
    public static String autoText(Object obj, String atxt) throws Exception{
        return XTRObjects.autoText().with(obj).run(atxt);
    }

}
