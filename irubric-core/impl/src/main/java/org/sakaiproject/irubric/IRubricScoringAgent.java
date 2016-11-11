package org.sakaiproject.irubric;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.irubric.model.GradableObjectRubric;
import org.sakaiproject.irubric.model.IRubricManager;
import org.sakaiproject.irubric.model.IRubricService;
import org.sakaiproject.scoringservice.api.AbstractScoringAgent;
import org.sakaiproject.scoringservice.api.ScoringComponent;
import org.sakaiproject.site.api.Site;


/**
 * Created with IntelliJ IDEA.
 * User: jbush
 * Date: 5/21/13
 * Time: 8:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class IRubricScoringAgent extends AbstractScoringAgent {
    private static Log LOG = LogFactory.getLog(IRubricScoringAgent.class);

    private IRubricService iRubricService;
    private IRubricManager iRubricManager;
    private ServerConfigurationService serverConfigurationService;

    public String getScore(String gradebookUid, String gradebookItemId, String studentId) {
        iRubricService.refreshGrades(gradebookUid, gradebookItemId);
        return "success";
    }

    public boolean hasScoringComponent() {
        return true;
    }

    public ScoringComponent getScoringComponent(String gradebookUid, String gradebookItemId) {
        GradableObjectRubric rubric= iRubricManager.getGradableObjectRubric(Long.valueOf(gradebookItemId));
        if (rubric != null) {
            ScoringComponent component =  new ScoringComponent();
            component.setId(rubric.getiRubricId());
            component.setName(rubric.getiRubricTitle());
            return component;

        }

        //TODO figure out how to call iRubric and check for a rubric
        // b/c we'll need to do that and the save it locally.

        // currently iRubric calls back to us.

        return null;
    }

    public String getScoringComponentLaunchUrl(String gradebookUid, String gradebookItemId) {
        return 	"/irubric-tool/IRubricServlet?p=a&gradebookItemId="+ gradebookItemId+
        									"&siteId=" + gradebookUid + "&gradebookUid=" + gradebookUid;
    }

    @Override
    public boolean isEnabled(String gradebookUid, String gradebookItemId) {
        //DN 2012-08-15: get irubric switch
		String irubricSwitch = serverConfigurationService.getString("irubric.switch");

        //check turn on/off rubric
        boolean isShowRubric = false;

		//if turn off rubric then it will not value IrubricId and siteId(use siteId check turn off rubric on UI)
		if(irubricSwitch != null) {

			if(irubricSwitch.equals("1")) {
				isShowRubric = true;
			} else if(irubricSwitch.equals("2")){// rurbicswitch =2 then check rubric site
                try {
                    Site site = iRubricService.getSiteService().getSite(gradebookUid);
                    //get irubric site
                    String iRubricSiteValue = site.getProperties().getProperty("iRubricSite");
                    //if is rubric site then is turn on rubric
                    if (iRubricSiteValue != null && "true".equalsIgnoreCase(iRubricSiteValue)) {
                        isShowRubric = true;
                    }
                } catch (IdUnusedException e) {
                    LOG.error(e.getMessage(), e);
                }
 			}
        }
        return isShowRubric;
    }

    public String getScoreLaunchUrl(String gradebookUid, String gradebookItemId) {
        return "/irubric-tool/IRubricServlet?gradebookItemId="+ gradebookItemId +
    				"&siteId=" + gradebookUid + "&gradebookUid=" + gradebookUid + "&p=ga";
    }

    public String getScoreLaunchUrl(String gradebookUid, String gradebookItemId, String studentId) {
         if (studentId == null) 
             return "/irubric-tool/IRubricServlet?p=g&gradebookItemId=" + gradebookItemId +
                 "&siteId=" + gradebookUid + "&gradebookUid=" + gradebookUid;

         return "/irubric-tool/IRubricServlet?p=g&gradebookItemId=" + gradebookItemId +
         "&rosterStudentId="+ studentId + "&siteId=" + gradebookUid + "&gradebookUid=" + gradebookUid;
    }

    public String getViewScoreLaunchUrl(String gradebookUid, String gradebookItemId, String studentId) {
    	if (studentId == null) {
    		return "/irubric-tool/IRubricServlet?p=vu&gradebookItemId=" + gradebookItemId +
    				"&siteId=" + gradebookUid + "&gradebookUid=" + gradebookUid;
    	} else {
    		return "/irubric-tool/IRubricServlet?p=v&gradebookItemId=" + gradebookItemId +
    				"&rosterStudentId="+ studentId + "&siteId=" + gradebookUid + "&gradebookUid=" + gradebookUid;
    	}
    }

    public void setServerConfigurationService(ServerConfigurationService serverConfigurationService) {
        this.serverConfigurationService = serverConfigurationService;
    }

    public void setiRubricService(IRubricServiceImpl iRubricService) {
        this.iRubricService = iRubricService;
    }

    public void setiRubricManager(IRubricManager iRubricManager) {
        this.iRubricManager = iRubricManager;
    }
    
    public String getImageReference() {
    	return "/../irubric-tool/images/rubric.gif";
    }
    
    public String getScoresUrl(String gradebookUid, String gradebookItemId) {
    	return "/irubric-tool/IRubricServlet?gradebookItemId="+ gradebookItemId +
                "&siteId=" + gradebookUid + "&gradebookUid=" + gradebookUid + "&p=gag";
    }
    
    public String getScoreUrl(String gradebookUid, String gradebookItemId, String studentUid) {
    	return "/irubric-tool/IRubricServlet?gradebookItemId="+ gradebookItemId +
    			"&rosterStudentId=" + studentUid + "&siteId=" + gradebookUid + "&gradebookUid=" + 
    			gradebookUid + "&p=gg";
    }
    
    public String getStudentScoresUrl(String gradebookUid, String studentUid) {
    	return "/irubric-tool/IRubricServlet?"+ 
    			"rosterStudentId=" + studentUid + "&siteId=" + gradebookUid + "&gradebookUid=" + 
    			gradebookUid + "&p=gas";
    }
    
    
    public void transferScoringComponentAssociations(String fromGradebookUid, String toGradebookUid) {
    	iRubricManager.copyRubricAssociations(fromGradebookUid, toGradebookUid);
    }
    
    public List<ScoringComponent> getScoringComponents(String gradebookUid) {
    	List<ScoringComponent> components = new ArrayList<ScoringComponent>();
    	List<GradableObjectRubric> allRubrics = iRubricManager.getAllRubricsInGradebook(gradebookUid);
    	if (allRubrics != null && !allRubrics.isEmpty()) {
    		for (GradableObjectRubric rubric : allRubrics) {
    			ScoringComponent component = new ScoringComponent();
    			component.setId(rubric.getiRubricId());
                component.setName(rubric.getiRubricTitle());
                components.add(component);
    		}
    	}
    	
    	return components;
    }
}
