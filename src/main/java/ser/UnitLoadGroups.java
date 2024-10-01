package ser;

import com.ser.blueline.*;
import com.ser.blueline.bpm.IBpmService;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.IProcessType;
import com.ser.blueline.bpm.ITask;
import com.ser.blueline.metaDataComponents.IArchiveClass;
import com.ser.blueline.metaDataComponents.IArchiveFolderClass;
import com.ser.foldermanager.IFolder;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.fasterxml.jackson.databind.type.LogicalType.Collection;


public class UnitLoadGroups extends UnifiedAgent {
    Logger log = LogManager.getLogger(this.getClass().getName());
    public static ISession ses;
    public static IDocumentServer srv;
    public static IBpmService bpm;
    @Override
    protected Object execute() {
        if (getEventFolder() == null) {
            return resultError("Null Document object.");
        }
        ses = getSes();
        srv = ses.getDocumentServer();
        bpm = getBpm();

        _info("Started");

        try{
            update(getEventFolder());
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
    public static void update(IInformationObject folder) throws Exception {

        String name = "";
        if(hasDescriptor(folder, "ObjectName")){
            String nam1 = folder.getDescriptorValue("ObjectName", String.class);
            name = (nam1 == null || nam1.isBlank() ? name : nam1.trim());
        }
        if(name.isBlank()){throw new Exception("Unit-name not found.");}

        List<String> groupCatgs = new ArrayList<>();

        List<String> owners = new ArrayList<>();
        if(hasDescriptor(folder, "AbacOwner")){
            String own1 = folder.getDescriptorValue("AbacOwner", String.class);
            owners = (own1 == null || own1.isEmpty() ? owners : Arrays.asList(own1));
            groupCatgs.add("Owner");
        }
        List<String> dccs = new ArrayList<>();
        if(hasDescriptor(folder, "ccmPrjCard_DccList")){
            List<String> dcs1 = folder.getDescriptorValues("ccmPrjCard_DccList", String.class);
            dccs = (dcs1 == null || dcs1.isEmpty() ? dccs : dcs1);
            groupCatgs.add("DCC");
        }

        List<String> moderators = new ArrayList<>();
        if(hasDescriptor(folder, "confidentialityModorate")){
            List<String> mds1 = folder.getDescriptorValues("confidentialityModorate", String.class);
            moderators = (mds1 == null || mds1.isEmpty() ? moderators : mds1);
            groupCatgs.add("Moderate");
        }

        List<String> highs = new ArrayList<>();
        if(hasDescriptor(folder, "confidentialityHigh")){
            List<String> hgs1 = folder.getDescriptorValues("confidentialityHigh", String.class);
            highs = (hgs1 == null || hgs1.isEmpty() ? highs : hgs1);
            groupCatgs.add("High");
        }

        List<String> highests = new ArrayList<>();
        if(hasDescriptor(folder, "confidentialityVeryHigh")){
            List<String> hst1 = folder.getDescriptorValues("confidentialityVeryHigh", String.class);
            highests = (hst1 == null || hst1.isEmpty() ? highests : hst1);
            groupCatgs.add("Highest");
        }

        ISerClassFactory classFactory = srv.getClassFactory();
        for(String grpCatg : groupCatgs){
            String groupName = name + "_" + grpCatg;
            IGroup group = srv.getGroupByName(ses, groupName);
            if(group != null){
                srv.deleteGroup(ses, group);
            }

            group = classFactory.createGroupInstance(ses, groupName);
            group.commit();

            List<String> members = new ArrayList<>();
            if(grpCatg.equals("Owner")){
                members = owners;
            }
            if(grpCatg.equals("DCC")){
                members = dccs;
            }
            if(grpCatg.equals("Moderate")){
                members = Stream.of(moderators, highs, highests)
                        .flatMap(java.util.Collection::stream).collect(Collectors.toList());
            }
            if(grpCatg.equals("High")){
                members = Stream.of(highs, highests)
                        .flatMap(java.util.Collection::stream).collect(Collectors.toList());
            }
            if(grpCatg.equals("Highest")){
                members = highests;
            }

            for(String memberId : members){
                IUser musr = srv.getUser(ses, memberId);
                if(musr != null){
                    IUser xusr = musr.getModifiableCopy(ses);
                    xusr.setGroupIDs(ArrayUtils.add(musr.getGroupIDs(), group.getID()));
                    xusr.commit();
                    continue;
                }
                IRole mrol = srv.getRoleByName(ses, memberId);
                if(mrol != null){
                    IRole xrol = mrol.getModifiableCopy(ses);
                    xrol.setGroupIDs(ArrayUtils.add(mrol.getGroupIDs(), group.getID()));
                    xrol.commit();
                    continue;
                }
                IUnit munt = srv.getUnitByName(ses, memberId);
                if(munt != null){
                    IUnit xunt = munt.getModifiableCopy(ses);
                    xunt.setGroupIDs(ArrayUtils.add(munt.getGroupIDs(), group.getID()));
                    xunt.commit();
                    continue;
                }
            }
        }
    }
    public static boolean hasDescriptor(IInformationObject object, String descName){
        IDescriptor[] descs = srv.getDescriptorByName(descName, ses);
        List<String> checkList = new ArrayList<>();
        for(IDescriptor ddsc : descs){
            checkList.add(ddsc.getId());
        }

        String[] descIds = new String[0];
        if(object instanceof IFolder){
            IArchiveFolderClass folderClass = srv.getArchiveFolderClass(object.getClassID(), ses);
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