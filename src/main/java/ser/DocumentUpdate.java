package ser;

import com.ser.blueline.*;
import com.ser.blueline.bpm.*;
import com.ser.blueline.metaDataComponents.IArchiveClass;
import com.ser.blueline.metaDataComponents.IArchiveFolderClass;
import com.ser.foldermanager.IFolder;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class DocumentUpdate extends UnifiedAgent {
    Logger log = LogManager.getLogger(this.getClass().getName());
    ISession ses;
    IDocumentServer srv;
    IBpmService bpm;

    IGroup[] grps;
    private ProcessHelper helper;
    @Override
    protected Object execute() {
        if (getEventDocument() == null) {
            return resultError("Null Document object.");
        }
        ses = getSes();
        srv = ses.getDocumentServer();
        bpm = getBpm();

        _info("Started");

        try{
            IDocument document = getEventDocument();

            String confidentiality = "";
            if(hasDescriptor(document, "ccmConfidentiality")){
                String cfd1 = document.getDescriptorValue("ccmConfidentiality", String.class);
                confidentiality = (cfd1 == null || cfd1.isBlank() ? confidentiality : cfd1.trim());
            }
            String owner = "";
            if(!document.getOwnerID().isBlank()){
                IUser own1 = srv.getUser(ses, document.getOwnerID());
                owner = (own1 != null && !own1.getLogin().isBlank() ? own1.getLogin() : owner);
            }
            if(owner.isBlank()){throw new Exception("Owner not found.");}

            List<String> all = new ArrayList<>();
            List<String> shares = new ArrayList<>();

            if(hasDescriptor(document, "ccmPRJCard_code")){
                String prj1 = document.getDescriptorValue("ccmPRJCard_code", String.class);
                if(prj1 != null && !prj1.isBlank()){
                    all.add(prj1);
                    shares.add(prj1);
                }
            }

            if(hasDescriptor(document, "ccmPrjDocDepartment")){
                String dpt1 = document.getDescriptorValue("ccmPrjDocDepartment", String.class);
                if(dpt1 != null && !dpt1.isBlank()){
                    all.add(dpt1);
                }
            }

            IInformationObject parent = document.getPrimaryParent();
            if(parent != null && hasDescriptor(parent, "ObjectName")){
                String pnam = parent.getDescriptorValue("ObjectName", String.class);
                if(pnam != null && !pnam.isBlank()){
                    all.add(pnam);
                }
            }

            if(hasDescriptor(document, "ccmPrjDocShares")){
                List<String> shs1 = document.getDescriptorValues("ccmPrjDocShares", String.class);
                if(shs1 != null && !shs1.isEmpty()){
                    for(String shr1 : shs1){
                        if(all.contains(shr1)){continue;}
                        shares.add(shr1);
                        all.add(shr1);
                    }
                }
            }
            if(all.isEmpty()){throw new Exception("Shares-list is empty.");}

            boolean update = false;
            if(hasDescriptor(document, "AbacOrgaRead")) {
                document.setDescriptorValues("AbacOrgaRead",
                        readers(confidentiality, owner, all, shares));
                        //readers("Highest", owner, all, shares)
                update = true;
            }
            if(hasDescriptor(document, "AbacOrgaEdit")) {
                document.setDescriptorValues("AbacOrgaEdit",
                        editors(confidentiality, owner, all, shares));
                        //editors("Highest", owner, all, shares)
                update = true;
            }
            if(update) {
                document.commit();
            }
            _info("Tested.");

        } catch (Exception e) {
            //throw new RuntimeException(e);
            _error("Exception       : " + e.getMessage());
            _error("    Class       : " + e.getClass());
            _error("    Stack-Trace : " + e.getStackTrace() );
            return resultRestart("Exception : " + e.getMessage(),10);
        }

        _info("Finished");
        return resultSuccess("Ended successfully");
    }

    public List<String> editors(String cnfd, String owner, List<String> all, List<String> shares){
        List<String> rtrn = new ArrayList<>();

        for(String item : all){
            if(shares.contains(item)) {continue;}
            if(!rtrn.contains(item + "_DCC")) {
                rtrn.add(item + "_DCC");
            }
            if(cnfd.equals("Public") || cnfd.equals("Normal")){
                if(!rtrn.contains(item)) {
                    rtrn.add(item);
                }
                continue;
            }
            if(!rtrn.contains(item + "_Owner")) {
                rtrn.add(item + "_Owner");
            }
            if(!rtrn.contains(item + "_" + cnfd)) {
                rtrn.add(item + "_" + cnfd);
            }
        }

        if(!rtrn.contains(owner)) {
            rtrn.add(owner);
        }
        return rtrn;
    }
    public List<String> readers(String cnfd, String owner, List<String> all, List<String> shares){
        List<String> rtrn = new ArrayList<>();
        if(cnfd.equals("Public")){
            rtrn.add("_All_Users");
            return rtrn;
        }

        if(!rtrn.contains(owner)) {
            rtrn.add(owner);
        }
        for (String item : all) {
            if (cnfd.equals("Normal")) {
                if(!rtrn.contains(item)) {
                    rtrn.add(item);
                }
                continue;
            }
            if(!rtrn.contains(item + "_" + cnfd)) {
                rtrn.add(item + "_" + cnfd);
            }
        }
        return rtrn;
    }

    public boolean hasDescriptor(IInformationObject object, String descName){
        IDescriptor[] descs = ses.getDocumentServer().getDescriptorByName(descName, ses);
        List<String> checkList = new ArrayList<>();
        for(IDescriptor ddsc : descs){
            checkList.add(ddsc.getId());
        }

        String[] descIds = new String[0];
        if(object instanceof IFolder){
            String classID = object.getClassID();
            IArchiveFolderClass folderClass = ses.getDocumentServer().getArchiveFolderClass(classID , ses);
            descIds = folderClass.getAssignedDescriptorIDs();
        }else if(object instanceof IDocument){
            IArchiveClass documentClass = ((IDocument) object).getArchiveClass();
            descIds = documentClass.getAssignedDescriptorIDs();
        }else if(object instanceof ITask){
            IProcessType processType = ((ITask) object).getProcessType();
            descIds = processType.getAssignedDescriptorIDs();
        }else if(object instanceof IProcessInstance){
            IProcessType processType = ((IProcessInstance) object).getProcessType();
            descIds = processType.getAssignedDescriptorIDs();
        }

        List<String> descList = Arrays.asList(descIds);
        for(String dId : descList){
            if(checkList.contains(dId)){return true;}
        }
        return false;
    }
    public void _info(String txt){
        System.out.println("[INFO] " + txt);
        log.info(txt);
    }
    public void _warn(String txt){
        System.out.println("[WARN] " + txt);
        log.warn(txt);
    }
    public void _error(String txt){
        System.out.println("[ERROR] " + txt);
        log.error(txt);
    }

}