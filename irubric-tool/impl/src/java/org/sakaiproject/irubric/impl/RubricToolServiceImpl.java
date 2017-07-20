package org.sakaiproject.irubric.impl;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.authz.api.FunctionManager;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.irubric.api.RubricToolService;
import org.sakaiproject.scoringservice.api.ScoringAgent;
import org.sakaiproject.scoringservice.api.ScoringService;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.ToolManager;


public class RubricToolServiceImpl implements RubricToolService
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(RubricToolServiceImpl.class);
	
	private SecurityService securityService;
    private SiteService siteService;
    private ServerConfigurationService serverConfigurationService;
    private ScoringService scoringService;
    private ToolManager toolManager;
    private FunctionManager functionManager;
    private GradebookService gradebookService;
    
    private static final String SORTED_BY_TITLE = "title";
    private static final String SORTED_BY_DUEDATE = "duedate";
    private static final String SORTED_BY_SORT_ORDER = "sortOrder";
    private static final String EPORTFOLIO = "irubric.eportfolio";

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		
		M_log.info(this + " init()");

		//register permission
		// register functions
		functionManager.registerFunction(SECURE_IRUBRIC_HOME);
		functionManager.registerFunction(SECURE_MY_RUBRIC);
		functionManager.registerFunction(SECURE_ACCESS_GALLERY_IRUBRIC);
		functionManager.registerFunction(SECURE_BUILD_IRUBRIC);
		functionManager.registerFunction(SECURE_COLLABORATIVE_ASSESSMENTS_IRUBRIC);
		functionManager.registerFunction(SECURE_IRUBRIC_REPORT);
		functionManager.registerFunction(SECURE_GRADES); 		



		if (makePermission(EPORTFOLIO)) {
	 		// Added the element in the permission page
			functionManager.registerFunction(SECURE_EPORTFOLIOS);
			functionManager.registerFunction(SECURE_BUILD_EPORTFOLIOS);
			functionManager.registerFunction(SECURE_ASSIGNMENT_MATRICES);
		}

	} // init

	
	/**
	 * Define the permission 
	 */
	private boolean makePermission(String key) {
 		// Get the eportfolio value by key (the key same from sakai.properties)
		return serverConfigurationService.getBoolean(key, false);
	}


	/**
	 * Returns to uninitialized state.
	 */
	public void destroy() {
		M_log.info(this + " destroy()");
	}


	//check allow build irubric
	public boolean allowBuildiRubric(String userid, String resourceString) {
		
		// checking allow at the site
		if (unlockCheck(userid, SECURE_BUILD_IRUBRIC, resourceString)) 
			return true;
		return false;
	}

	//check allow access gallery
	public boolean allowAccessGalleryiRubric(String userid, String resourceString) {
		// checking allow at the site level
		if (unlockCheck(userid, SECURE_ACCESS_GALLERY_IRUBRIC, resourceString)) 
			return true;
		return false;
	}
	
	//check allow view report
	public boolean allowiRubricReports(String userid, String resourceString) {
		// checking allow at the site level
		if (unlockCheck(userid, SECURE_IRUBRIC_REPORT, resourceString)) 
			return true;
		return false;
	}
	
	//check allow access Assessment
	public boolean allowAssessmentiRubric(String userid, String resourceString) {
		// checking allow at the site level
		if (unlockCheck(userid, SECURE_COLLABORATIVE_ASSESSMENTS_IRUBRIC, resourceString)) 
			return true;
		return false;
	}

	//check allow my rubric
	public boolean allowMyRubrics(String userid, String resourceString) {
		// checking allow at the site level
		if (unlockCheck(userid, SECURE_MY_RUBRIC, resourceString)) 
			return true;
		return false;
	}

	//check allow grade all
	public boolean allowGrades(String userid, String resourceString) {
		// checking allow at the site level
		if (unlockCheck(userid, SECURE_GRADES, resourceString)) 
			return true;
		return false;
	}
	

	public boolean allowiRubricHome(String userid, String resourceString) {
		// checking allow at the site level
		if (unlockCheck(userid, SECURE_IRUBRIC_HOME, resourceString)) 
			return true;
		return false;
	}
	
	//check allow access ePorfolios
	public boolean allowMyePortfolios(String userid, String resourceString) {
		// checking allow at the site level
		if (unlockCheck(userid, SECURE_EPORTFOLIOS, resourceString)) 
			return makePermission(EPORTFOLIO);
		return false;
	}

	//check allow build ePortpolios
	public boolean allowBuildePortfolios(String userid, String resourceString) {
		// checking allow at the site level
		if (unlockCheck(userid, SECURE_BUILD_EPORTFOLIOS, resourceString)) 
			return makePermission(EPORTFOLIO);
		return false;
	}

	//check allow assignment matrices
	public boolean allowAssMatrices(String userid, String resourceString) {
		// checking allow at the site level
		if (unlockCheck(userid, SECURE_ASSIGNMENT_MATRICES, resourceString)) 
			return makePermission(EPORTFOLIO);
		return false;
	}


	/**
	 * Check security permission.
	 * @param userid - The user id string.
	 * @param lock - The lock id string.
	 * @param resource - The resource reference string, or null if no resource is involved.
	 * @return true if allowed, false if not
	 */
	private boolean unlockCheck(String userid, String lock, String resource)
	{	
		
		return securityService.unlock(userid, lock, resource);

	}// unlockCheck
	
	//DN 2013-09-11: if irubric.switch=2 then some course show icon grade, get grade, summary report(grading)
	public boolean allowShowiRubricLink(){
		return allowShowiRubricLink(getCurrentContextId());
	}
	
	public boolean allowShowiRubricLink(String siteId) {
		boolean enabled = false;
		ScoringAgent scoringAgent = scoringService.getDefaultScoringAgent();
		if (scoringAgent != null) {
			enabled = scoringAgent.isEnabled(siteId, null);
		}
		
		return enabled;
	}

	//DN 2013-09-12: able to grade
	public boolean ableToGrade(String gradebookUId){
		return gradebookService.currentUserHasGradingPerm(gradebookUId);
	}
	/*
	*	get list assignment(gradebook item) by gradebookUId
	*	@param gradebookUId - gradebookUId string
	*	@param	sort- type sort(title, duedate)
	*	@param	ascending - string "true" else "false"
	*	return list assignment
	*/
	public List getAssignmentsByGradebookUId(String gradebookUId, String sort, String ascending) {
		
		List<Assignment> assignments = gradebookService.getViewableAssignmentsForCurrentUser(gradebookUId);
        
        //sort list assignment
        try
		{
			Collections.sort(assignments, new AssignmentComparator(sort, ascending));		
		}
		catch (Exception e)
		{
			// log exception during sorting for helping debugging
			M_log.debug("have error sort list,sort=" + sort + " ascending=" + ascending, e);
		}

		return assignments;
	}

	/**
	 * the AssignmentComparator clas
	 */
	private class AssignmentComparator implements Comparator
	{
		Collator collator = Collator.getInstance();
		
		/**
		 * the criteria
		 */
		String m_criteria = null;

		/**
		 * the criteria
		 */
		String m_asc = null;


		/**
		 * constructor
		 *
		 * @param state
		 *        The state object
		 * @param criteria
		 *        The sort criteria string
		 * @param asc
		 *        The sort order string. TRUE_STRING if ascending; "false" otherwise.
		 */
		public AssignmentComparator(String criteria, String asc)
		{
			m_criteria = criteria;
			m_asc = asc;

		} // constructor

		/**
		 * implementing the compare function
		 *
		 * @param o1
		 *        The first object
		 * @param o2
		 *        The second object
		 * @return The compare result. 1 is o1 < o2; -1 otherwise
		 */
		public int compare(Object o1, Object o2)
		{
			int result = -1;
			
			if (m_criteria == null)
			{
				m_criteria = SORTED_BY_SORT_ORDER;
			}

			Assignment assignment1 = (Assignment) o1;
			Assignment assignment2 = (Assignment) o2;

			if (m_criteria.equals(SORTED_BY_SORT_ORDER))
			{
				Integer order1 = assignment1.getSortOrder();
				Integer order2 = assignment2.getSortOrder();
				if (order1 == null)
				{
					result = -1;
				}
				else if (order2 == null)
				{
					result = 1;
				}
				else if (order1.compareTo(order2) > 0)
				{
					result = -1;
				}
				else
				{
					result = 1;
				}
			}

			else if (m_criteria.equals(SORTED_BY_TITLE))
			{
				// sorted by the assignment title
				String s1 = assignment1.getName();
				String s2 = assignment2.getName();
				result = compareString(s1, s2);
			}
			
			else if (m_criteria.equals(SORTED_BY_DUEDATE))
			{
				// sorted by the assignment due date
				Date t1 = assignment1.getDueDate();
				Date t2 = assignment2.getDueDate();

				if (t1 == null)
				{
					result = -1;
				}
				else if (t2 == null)
				{
					result = 1;
				}
				else if (t1.before(t2))
				{
					result = -1;
				}
				else
				{
					result = 1;
				}
			}
			
			// sort ascending or descending
			if (!Boolean.valueOf(m_asc))
			{
				result = -result;
			}
			return result;
		}

		private int compareString(String s1, String s2) 
		{
			int result;
			if (s1 == null && s2 == null) {
				result = 0;
			} else if (s2 == null) {
				result = 1;
			} else if (s1 == null) {
				result = -1;
			} else {
				result = collator.compare(s1.toLowerCase(), s2.toLowerCase());
			}
			return result;
		}

	} // AssignmentComparator
	
	public String getCurrentContextId() {
		if (toolManager != null && toolManager.getCurrentPlacement() != null && toolManager.getCurrentPlacement().getContext() != null){
			return toolManager.getCurrentPlacement().getContext();

		} else {
			return null;
		}
	}
	
	public void setServerConfigurationService(
			ServerConfigurationService serverConfigurationService) {
		this.serverConfigurationService = serverConfigurationService;
	}
	
	public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setSiteService(SiteService siteService) {
        this.siteService = siteService;
    }
    
    public void setScoringService(ScoringService scoringService) {
    	this.scoringService = scoringService;
    }
    
    public void setGradebookService(GradebookService gradebookService) {
    	this.gradebookService = gradebookService;
    }
    
    public void setToolManager(ToolManager toolManager) {
    	this.toolManager = toolManager;
    }
    
    public void setFunctionManager(org.sakaiproject.authz.api.FunctionManager functionManager) {
    	this.functionManager = functionManager;
    }
}