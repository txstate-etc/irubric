/*Copyright (C) Reazon Systems, Inc.  All rights reserved.*/
package org.sakaiproject.irubric;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.coursemanagement.api.Enrollment;
import org.sakaiproject.coursemanagement.api.EnrollmentSet;
import org.sakaiproject.coursemanagement.api.Section;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.irubric.model.GradableObjectRubric;
import org.sakaiproject.irubric.model.IRubricManager;
import org.sakaiproject.irubric.model.IRubricService;
import org.sakaiproject.portal.util.URLUtils;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.service.gradebook.shared.GradeDefinition;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;

// import javax.servlet.http.HttpServletRequest;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * iRubric bean - a class working with iRubric server
 * 
 * @author CD
 */
public class IRubricServiceImpl implements Serializable, IRubricService {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3177647771610921706L;
	private static Log LOG = LogFactory.getLog(IRubricServiceImpl.class);
	/**
	 * Declare URL parameter names. Abbreviations: CMD - command, SCH - school,
	 * GDB - gradebook, ROS - roster student, USR - user, DEST - destination
	 */

	private int timeout;
	private boolean isSSL;
	private boolean isAnonymousStudents;
	
	private int irubricSwitch = 0;

	private String irubricRootUrl;
	private String irubricInitReqUrl;
	private String irubricRedirectUrl;
	private String certID;
	private String xtoken;
	private String termPropertyName;
	private String[] evaluator;
	private String[] evaluatee;

	private String academicSessionId;
	private String sslPort;

	private CourseManagementService courseManagementService;
	private UserDirectoryService userDirectoryService;
	private SiteService siteService;
	private GradebookService gradebookService;
	private ToolManager toolManager;
	private ServerConfigurationService serverConfigurationService;

    private IRubricManager iRubricManager;
	
	private Double pointsPossible;
	
	public static final String TYPE_SECTION = "section";
	public static final String TYPE_GROUP = "group";

	/**
	 * sets the serverConfigurationService this property is set by
	 * spring-beans.xml
	 * 
	 * @param serverConfigurationService
	 *            the serverConfigurationService to set
	 */

	public void setServerConfigurationService(
			ServerConfigurationService serverConfigurationService) {
		this.serverConfigurationService = serverConfigurationService;
	}

    public String getResponseData(HttpURLConnection connection) throws Exception {
        return Helper.getResponseData(connection);
    }

    /**
	 * gets session of the current user
	 * 
	 * @return the session
	 */
    public Session getSession() {
		return SessionManager.getCurrentSession();
	}

	/**
	 * sets the courseManagementService this property is set by spring-beans.xml
	 * 
	 * @param courseManagementService
	 *            the courseManagementService to set
	 */
	public void setCourseManagementService(
			CourseManagementService courseManagementService) {
		this.courseManagementService = courseManagementService;
	}

	/**
	 * sets the gradebookManager service this property is set by
	 * spring-beans.xml
	 * 
	 * @param gradebookService
	 */
	public void setGradebookService(GradebookService gradebookService) {
		this.gradebookService = gradebookService;
	}

	/**
	 * sets the site service property this property is set by spring-beans.xml
	 * 
	 * @param siteService
	 */
	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	}

	/**
	 * set the userDirectoryService property this property is set by
	 * spring-beans.xml
	 * 
	 * @param userDirectoryService
	 */
	public void setUserDirectoryService(
			UserDirectoryService userDirectoryService) {
		this.userDirectoryService = userDirectoryService;
	}

	/**
	 * set the toolManager property this property is set by spring-beans.xml
	 * 
	 * @param toolManager
	 */
	public void setToolManager(ToolManager toolManager) {
		this.toolManager = toolManager;
	}

	/**
	 * get the courseManagementService property
	 * 
	 * @return the courseManagementService
	 */
    public CourseManagementService getCourseManagementService() {
		return courseManagementService;
	}

	/**
	 * get the gradebookService property
	 * 
	 * @return the gradebookService
	 */
    public GradebookService getGradebookService() {
		return gradebookService;
	}

	/**
	 * get the userDirectoryService property
	 * 
	 * @return the userDirectoryService
	 */
    public UserDirectoryService getUserDirectoryService() {
		return userDirectoryService;
	}

	/**
	 * get the siteService property
	 * 
	 * @return the siteService
	 */
	@Override
    public SiteService getSiteService() {
		return siteService;
	}

	/**
	 * get the toolManager property
	 * 
	 * @return the toolManager
	 */
    public ToolManager getToolManager() {
		return toolManager;
	}
	
	/**
	 * get the timeout property
	 * 
	 * @return the timeout
	 */
	@Override
    public int getTimeout() {
		return timeout;
	}

	/**
	 * get the irubricRootUrl property
	 * 
	 * @return the irubricRootUrl
	 */
	@Override
    public String getIrubricRootUrl() {
		StringBuilder url = new StringBuilder();
		if (this.isSSL) {
			url.append("https://");
			url.append(irubricRootUrl);
			url.append(":");
			url.append(this.sslPort);
		} else {
			url.append("http://");
			url.append(irubricRootUrl);
		}
		return url.toString();
	}

	/**
	 * Retrieve the irubricInitReqUrl property which directs to a URL on iRubric
	 * system. iRubric will return an xToken with dispatch token (starts with
	 * "T") or error code (starts with "E") ...
	 * 
	 * @return the irubricInitReqUrl
	 */
	@Override
    public String getIrubricInitReqUrl() {
		return irubricInitReqUrl;
	}

	/**
	 * get the irubricRedirectUrl property
	 * 
	 * @return the irubricRedirectUrl
	 */
	@Override
    public String getIrubricRedirectUrl() {
		StringBuilder str = new StringBuilder(getIrubricRootUrl());
		str.append("/");
		str.append(this.irubricRedirectUrl);

		return str.toString();
	}
	
	/**
	 * set the irubricSwitch property
	 * 
	 * @param irubricSwitch
	 *            the irubricSwitch to set
	 */
	public void setIrubricSwitch(int irubricSwitch) {
		this.irubricSwitch = irubricSwitch;
	}

	/**
	 * get the irubricSwitch property
	 * 
	 * @return the irubricSwitch
	 */
    public int getIrubricSwitch() {
		return serverConfigurationService.getInt("irubric.switch", 0); 
	
	}

	/**
	 * Constructor of this class
	 */
	public IRubricServiceImpl() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sakaiproject.tool.gradebook.ui.InitializableBean#init()
	 */
	public void init() {
		this.irubricRootUrl = serverConfigurationService
				.getString("irubric.rootUrl");

		this.irubricRedirectUrl = serverConfigurationService
				.getString("irubric.redirectUrl");

		this.irubricInitReqUrl = serverConfigurationService
				.getString("irubric.initReqUrl");

		this.sslPort = serverConfigurationService.getString("irubric.sslPort");

		this.timeout = serverConfigurationService
				.getInt("irubric.timeout", 10000);

		this.certID = serverConfigurationService.getString("irubric.certID");

		this.isAnonymousStudents = serverConfigurationService.getBoolean("irubric.anonymousStudents", false);
		this.isSSL = serverConfigurationService.getBoolean("irubric.isSSL", false);

		this.termPropertyName = serverConfigurationService
				.getString("irubric.termPropertyName");

		this.evaluator = serverConfigurationService
				.getStrings("irubric.evaluator");
		this.evaluatee = serverConfigurationService
				.getStrings("irubric.evaluatee");

	}

	/**
	 * get AcademicSessionId by site id
	 * 
	 * @param siteId
	 * @return string
	 * @author CD
	 */
	private String getAcademicSessionId(String siteId) throws IdUnusedException {
			Site site = siteService.getSite(siteId);
			academicSessionId = site.getProperties().getProperty(
					this.termPropertyName);
			if ((academicSessionId == "") || (academicSessionId == null))
			{									
				academicSessionId = "OTHER";
			}
		return academicSessionId;
	}

	/**
	 * Get role of current user
	 * 
	 * @return string
	 * @author CD
	 */
	private String getCurrentUserRole(String siteId) throws IdUnusedException, Exception {
		String roleName = null;

		// Get the current user
		String userId = userDirectoryService.getCurrentUser().getId();

		Site site = siteService.getSite(siteId);

		// query teacher of this class
		Set<Member> members = site.getMembers();

		for (Member member : members) {
			if (member.getUserId().equals(userId)) {
				roleName = member.getRole().getId();
			}
		}

		if (roleName == null) {
			throw new Exception("Cannot retrieve the role of current user.");
		} else {
			return roleName;
		}
	}

	/**
	 * Retrieve student's enrollment data
	 * 
	 * @param userEid
	 * @return String
	 */
	public String[] getStudentEnrollment(String userEid, String siteId) throws Exception {
		String[] studentEnrollment = { NULL_STRING, NULL_STRING };

		Site site = siteService.getSite(siteId);
		if (site != null) {
			String sectionId = site.getProperties().getProperty(
					P_PROP_SITE_SECTION_EID);

			// RS: this condition is needed by ONC as sectionId can be null.
			if (sectionId != null) {
				Section section = courseManagementService.getSection(sectionId);
				if (section != null) {
					EnrollmentSet enrollmentSet = section.getEnrollmentSet();
					if (enrollmentSet != null) {
						Enrollment enr = courseManagementService
								.findEnrollment(userEid, enrollmentSet.getEid());
						// Only add the enrollment if it's not dropped and it
						// has an
						// enrollment role mapping
						if (enr != null && !enr.isDropped()) {
							studentEnrollment[0] = enrollmentSet.getEid();
							studentEnrollment[1] = enr.getEnrollmentStatus();
							return studentEnrollment;
						}
					}
				}
			}
		}
		return studentEnrollment;
	}

	/**
	 * Construct a string containing default post data
	 * 
	 * @return String
	 * @throws Exception
	 */
	public String buildDefaultPostData(String currentSiteId) throws Exception {
		StringBuilder dataBuilder = new StringBuilder();

		Helper.addUrlParam(dataBuilder, "postFile", URLUtils
                .encodeUrl(this.irubricInitReqUrl));

		Helper.addUrlParam(dataBuilder, P_CERTIFICATE_ID, URLUtils
				.encodeUrl(this.certID));


		String academicId = getAcademicSessionId(currentSiteId);
		Helper.addUrlParam(dataBuilder, P_ACADEMIC_ID, URLUtils
				.encodeUrl(academicId));

		Helper.addUrlParam(dataBuilder, P_SITE_ID, URLUtils
				.encodeUrl(currentSiteId));

		String siteTitle = null;
		Site currentSite = getSiteService().getSite(currentSiteId);
		if (currentSite != null)
			siteTitle = currentSite.getTitle();
		Helper.addUrlParam(dataBuilder, P_SITE_TITLE, URLUtils
				.encodeUrl(siteTitle));

		// add user parameter
		dataBuilder.append("&");
		dataBuilder.append(teacherParameters(currentSiteId));
		return dataBuilder.toString();
	}

    public HttpURLConnection createHttpURLGetConnection(String url, int timeout) throws IOException {
        return Helper.createHttpURLGetConnection(url, timeout);
    }

    public HttpURLConnection createHttpURLConnection(String url, int timeout) throws IOException {
        return Helper.createHttpURLConnection(url, timeout);
    }

    /**
	 * @param dataBuilder
	 */
	public void addGradebookParams(String gradebookUid, Long gradebookItemId,
			StringBuilder dataBuilder) {

		org.sakaiproject.service.gradebook.shared.Assignment gradebookItem = gradebookService
				.getAssignment(gradebookUid, gradebookItemId);

		if (gradebookItem != null) {
			Helper.addUrlParam(dataBuilder, P_GDB_ITEM_NAME, URLUtils
					.encodeUrl(gradebookItem.getName()));

			Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ID, URLUtils
					.encodeUrl(gradebookItemId.toString()));
			
			setPointsPossible(gradebookItem.getPoints());


            if (gradebookItem.getCategoryName() != null && gradebookItem.getWeight() != null)
                Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
                        .encodeUrl(CATEGORY_OPT_CAT_AND_WEIGHT));
            else if (gradebookItem.getCategoryName() != null)
                Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
                        .encodeUrl(CATEGORY_OPT_CAT_ONLY));
            else
                Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
                        .encodeUrl(CATEGORY_OPT_NONE));

            int gradeEntry = gradebookService.getGradeEntryType(gradebookUid);
            if (gradeEntry == GradebookService.GRADE_TYPE_PERCENTAGE)
                Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE, URLUtils
                        .encodeUrl(ENTRY_OPT_PERCENT));
            else if (gradeEntry == GradebookService.GRADE_TYPE_POINTS)
                Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE, URLUtils
                        .encodeUrl(ENTRY_OPT_POINTS));
            else
                Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE, URLUtils
                        .encodeUrl(ENTRY_OPT_LETTER));
		}
	}

	/**
	 * Add some pieces of a teacher profile to URL parameters string
	 * 
	 * @return string
	 */
	private String teacherParameters(String currentSiteId) throws Exception {
		// Create data with String builder
		StringBuilder dataBuilder = new StringBuilder();

		String userFirstName = null;
		String userLastName = null;
		String userDisplayName = null;
		String username = null;
		String userId = getSession().getUserId();

		// teacher is current user
		User teacher = getUserDirectoryService().getCurrentUser();
		if (teacher != null) {
			userFirstName = teacher.getFirstName();
			userLastName = teacher.getLastName();
			userDisplayName = teacher.getDisplayName();
			username = teacher.getDisplayId();
		}

		Helper.addUrlParam(dataBuilder, P_USR_ID, URLUtils.encodeUrl(userId));

		if (username != null) {
			Helper.addUrlParam(dataBuilder, P_USR_LOGIN_NAME, URLUtils.encodeUrl(username));
		}
		
		if ((userFirstName == null || "".equals(userFirstName))
				&& (userLastName == null || "".equals(userLastName))) {
			Helper.addUrlParam(dataBuilder, P_USR_FNAME, URLUtils
					.encodeUrl(userDisplayName));

		} else {
			Helper.addUrlParam(dataBuilder, P_USR_FNAME, URLUtils
					.encodeUrl(userFirstName));

			Helper.addUrlParam(dataBuilder, P_USR_LNAME, URLUtils
					.encodeUrl(userLastName));
		}

		String userRole = getCurrentUserRole(currentSiteId);
		Helper.addUrlParam(dataBuilder, P_USR_ROLE, URLUtils
				.encodeUrl(userRole));

		String userRoleType = getUserRoleType(userRole);
		Helper.addUrlParam(dataBuilder, P_ROLE_TYPE, URLUtils
				.encodeUrl(userRoleType));


		return dataBuilder.toString();
	}

	/**
	 * @param dataBuilder
	 * @throws Exception
	 */
	public void addRosterParams(String rosterStudentId,
			StringBuilder dataBuilder, String siteId) throws Exception {
		// get value of purpose in dataBuilder
		String data = dataBuilder.toString();				
		String[] params = data.split("&");		
		String purpose = Helper.EMPTY_STRING;		
		for (String param : params) {						
			if (param.split("=")[0].equals("purpose")) {
				purpose = param.split("=")[1];
				break;
			}			
		}
		if (purpose.equals("ga")) {
			String[] rosterStudentIds = rosterStudentId.split(",");
			StringBuilder studentData = new StringBuilder();
			studentData.append("<students>");
			for (String rStudentId: rosterStudentIds)
			{				
				CreateXmlStudent(rStudentId,studentData, siteId);
			}
			studentData.append("</students>");			
			Helper.addUrlParam(dataBuilder, DATA_STUDENTS,
					URLUtils.encodeUrl(studentData.toString()));
			
			// add section information
			String sectionData = createXmlSectionData(siteId, rosterStudentIds);
			if (sectionData != null && !sectionData.isEmpty()) {
				Helper.addUrlParam(dataBuilder, DATA_SECTIONS, URLUtils.encodeUrl(sectionData));
			}
		} else {
			Helper.addUrlParam(dataBuilder, P_ROS_ID, URLUtils
					.encodeUrl(rosterStudentId));
	
			Helper.addUrlParam(dataBuilder, P_EN_SET, URLUtils
					.encodeUrl(getStudentEnrollment(rosterStudentId, siteId)[0]));
			Helper.addUrlParam(dataBuilder, P_ENS, URLUtils
					.encodeUrl(getStudentEnrollment(rosterStudentId, siteId)[1]));
	
			if (!this.isAnonymousStudents) {
				User student = getUserDirectoryService().getUser(rosterStudentId);
	
				if (student != null) {
					String rosterStudentFirstName = Helper.EMPTY_STRING;
					if (student.getFirstName() != null) {
						rosterStudentFirstName = student.getFirstName();
					}
					String rosterStudentLastName = Helper.EMPTY_STRING;
					if (student.getLastName() != null) {
						rosterStudentLastName = student.getLastName();
					}
	
					Helper.addUrlParam(dataBuilder, P_ROS_FNAME, URLUtils
							.encodeUrl(rosterStudentFirstName));
	
					Helper.addUrlParam(dataBuilder, P_ROS_LNAME, URLUtils
							.encodeUrl(rosterStudentLastName));
					
					Helper.addUrlParam(dataBuilder, P_ROS_USERNAME, URLUtils
							.encodeUrl(student.getDisplayId()));
				}
			}
		}
	}
	
	/**
	 * Student's infors are appended to data package builder to send to Irubric
	 * @param rosStudentId String Student ID
	 * @param studentData string data package builder
	 * @throws Exception
	 */
	private void CreateXmlStudent(String rosStudentId,StringBuilder studentData, String siteId) throws Exception
	{
		studentData.append("<student ");
		studentData.append(P_ROS_ID + "=\"" + rosStudentId +"\"");
        studentData.append(" " + P_EN_SET + "=\"" + getStudentEnrollment(rosStudentId,siteId)[0] + "\"");
        studentData.append(" " + P_ENS + "=\"" + getStudentEnrollment(rosStudentId,siteId)[1] + "\"");
		User student = getUserDirectoryService().getUser(
				rosStudentId);

		if (student != null) {
			String rosterStudentFirstName = Helper.EMPTY_STRING;
			if (student.getFirstName() != null) {
				rosterStudentFirstName = student.getFirstName();
			}
			String rosterStudentLastName = Helper.EMPTY_STRING;
			if (student.getLastName() != null) {
				rosterStudentLastName = student.getLastName();
			}
			
			studentData.append(" " + P_ROS_FNAME + "=\"" + rosterStudentFirstName + "\"");
			studentData.append(" " + P_ROS_LNAME + "=\"" + rosterStudentLastName + "\"");	
			studentData.append(" " + P_ROS_USERNAME + "=\"" + student.getDisplayId() + "\"");
		}
		
		studentData.append(" />");			
	}
	
	/**
	 * 
	 * @param siteId
	 * @param students an array of students to filter the group memberships
	 * @return a String representation of the sections/groups that the current user is
	 * allowed to grade for the gb item
	 */
	private String createXmlSectionData(String siteId, String[] students) {
		StringBuilder sectionData = new StringBuilder();

		if (siteId == null) {
			LOG.warn("Null siteId passed to createXmlSectionData. No section data returned.");
			return sectionData.toString();
		}
		
		// We are going to filter the section memberships to only include the gradable students, 
		// so convert the array to an ArrayList for easier processing later on
		List<String> studentIds = new ArrayList<String>();
		if (students != null) {
			for (String student : students) {
				studentIds.add(student);
			}
		}

		// In the gradebook, the default TA grading privileges may be overridden via "grader permissions"
		// so we need to retrieve the "allowed sections" information from the gradebook
		Set<String> gradableSections = new HashSet<String>();

		Map<String,String> viewableSectionsMap = gradebookService.getViewableSectionUuidToNameMap(siteId);
		if (viewableSectionsMap != null) {
			for (String sectionUid : viewableSectionsMap.keySet()) {
				// extract the group id from the uid
				String groupId = sectionUid.substring(sectionUid.lastIndexOf("/") + 1);
				gradableSections.add(groupId);
			}
		}

		if (!gradableSections.isEmpty()) {
			Collection<Group> groups = getSiteGroups(siteId);
			if (groups != null && !groups.isEmpty()) {

				sectionData.append("<sections>");

				for (Group group : groups) {
					if (gradableSections.contains(group.getId())) {
						// determine if this is a section or group
						String type;
						if ("true".equalsIgnoreCase(group.getProperties().getProperty(Group.GROUP_PROP_WSETUP_CREATED))) {
							type=TYPE_GROUP;
						} else if (group.getProperties().getProperty("sections_category") != null) {
							type=TYPE_SECTION;
						} else {
							// default to group type but shouldn't get here
							type=TYPE_GROUP;
						}
						
						sectionData.append("<section");
						sectionData.append(" id=\"" + group.getId() + "\"");
						sectionData.append(" name=\"" + group.getTitle() + "\"");
						sectionData.append(" type=\"" + type + "\"");
						sectionData.append(">");

						// include the student membership information for this section/group
						Set<Member> members = group.getMembers();
						if (members != null) {
							sectionData.append("<members>");

							for (Member member : members) {
								// We only want to send the gradable students
								if (studentIds.contains(member.getUserId())) {
									sectionData.append("<member " + P_ROS_ID + "=\"" + member.getUserId() + "\" />");
								}
							}

							sectionData.append("</members>");
						}

						sectionData.append("</section>");				
					}
				}

				sectionData.append("</sections>");
			}
		}

		return sectionData.toString();
	}
	
	public Collection<Group> getSiteGroups(String contextId) {
        try {
            Site s = siteService.getSite(contextId);
            return s.getGroups();
        } catch (IdUnusedException e){
            LOG.warn("IdUnusedException attempting to find site with id: " + contextId);
            return new ArrayList<Group>();
        }
    }


	/**
	 * Get the URL on iRubric system to initialize a request from Sakai
	 * 
	 * @return A URL
	 */
	@Override
    public String getInitReqURL() {
		StringBuilder url = new StringBuilder();
		url.append(getIrubricRootUrl());
		url.append("/");
		url.append(getIrubricInitReqUrl());
		return url.toString();
	}

	/**
	 * Sync the attached rubric data from iRubric system
	 * 
	 * @param gradebookItemId
	 * @param iRubricId
	 * @param iRubricTitle
	 * 
	 * @return void
	 */
	@Override
    public void updateAssignmetByRubric(Long gradebookItemId, String iRubricId,
                                        String iRubricTitle) {

		GradableObjectRubric gradableObjectRubric = iRubricManager.getGradableObjectRubric(gradebookItemId);

		if (gradableObjectRubric == null) {
			gradableObjectRubric = new GradableObjectRubric();
			gradableObjectRubric.setGradableObjectId(gradebookItemId);
		}

		if (iRubricId.toLowerCase().equals(NULL_STRING)) {
			gradableObjectRubric.setiRubricId(null);
			gradableObjectRubric.setiRubricTitle(null);
		} else {
			gradableObjectRubric.setiRubricId(iRubricId);
			gradableObjectRubric.setiRubricTitle(iRubricTitle);
		}

        iRubricManager.updateGradableObjectRubric(gradableObjectRubric);
	}

	/**
	 * Get the role type name by user role
	 * 
	 * @param roleName
	 * @return
	 */
	@Override
    public String getUserRoleType(String roleName) {
		if (roleName == null) {
			return EMPTY_STRING;
		}

		for (int i = 0; i < evaluator.length; i++) {
			LOG.info(evaluator[i]);
			if (evaluator[i].toLowerCase()
					.equals(roleName.trim().toLowerCase())) {
				return ROLE_TYPE_EVALUATOR;
			}
		}

		for (int i = 0; i < evaluatee.length; i++) {
			LOG.info(evaluatee[i]);
			if (evaluatee[i].toLowerCase()
					.equals(roleName.trim().toLowerCase())) {
				return ROLE_TYPE_EVALUATEE;
			}
		}

		return EMPTY_STRING;
	}

    /**
   	 * build data packet for attach purpose
   	 *
   	 * @param gradebookItemId
   	 * @throws Exception
   	 */

       public void buildPostDataForAttach(StringBuilder dataBuilder, String gradebookUid, Long gradebookItemId) throws Exception {
   		org.sakaiproject.service.gradebook.shared.Assignment gradebookItem = gradebookService
   				.getAssignment(gradebookUid, gradebookItemId);
   		if (gradebookItem != null) {

   			Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ID, URLUtils
   					.encodeUrl(gradebookItemId.toString()));

   			String gradebookItemName = gradebookItem.getName();
   			Helper.addUrlParam(dataBuilder, P_GDB_ITEM_NAME, URLUtils
   					.encodeUrl(gradebookItemName));

   			// add current user's information
   			teacherParameters(gradebookUid);

   			if (gradebookItem.getCategoryName() !=  null && gradebookItem.getWeight() != null)
   				Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
   						.encodeUrl(CATEGORY_OPT_CAT_AND_WEIGHT));
   			else if (gradebookItem.getCategoryName() !=  null)
   				Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
   						.encodeUrl(CATEGORY_OPT_CAT_ONLY));
   			else
   				Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
   						.encodeUrl(CATEGORY_OPT_NONE));

   			int gradeEntry = gradebookService.getGradeEntryType(gradebookUid);
   			if (gradeEntry == GradebookService.GRADE_TYPE_PERCENTAGE)
   				Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE, URLUtils
   						.encodeUrl(ENTRY_OPT_PERCENT));
   			else if (gradeEntry == GradebookService.GRADE_TYPE_POINTS)
   				Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE, URLUtils
   						.encodeUrl(ENTRY_OPT_POINTS));
   			else
   				Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE, URLUtils
   						.encodeUrl(ENTRY_OPT_LETTER));

   			String pointsPossible = null;
   			if (gradebookItem.getPoints() == null) {
   				pointsPossible = Helper.EMPTY_STRING;
   			} else {
   				pointsPossible = gradebookItem.getPoints().toString();
   			}
   			Helper.addUrlParam(dataBuilder, P_POINTS_POSSIBLE, pointsPossible);
   		}

   	}

	/**
	 * build data packet for attach purpose
	 * 
	 * @param dataBuilder
	 * @param gradebookItemId
	 * @throws Exception
	 */
	private void buildPostDataForAttach(StringBuilder dataBuilder,
			String gradebookUid, Long gradebookItemId, String currentSiteId) throws Exception {

		org.sakaiproject.service.gradebook.shared.Assignment gradebookItem = gradebookService
				.getAssignment(gradebookUid, gradebookItemId);
		if (gradebookItem != null) {

			Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ID, URLUtils
					.encodeUrl(gradebookItemId.toString()));

			String gradebookItemName = gradebookItem.getName();
			Helper.addUrlParam(dataBuilder, P_GDB_ITEM_NAME, URLUtils
					.encodeUrl(gradebookItemName));

			// add current user's information
			teacherParameters(currentSiteId);

            if (gradebookItem.getCategoryName() != null && gradebookItem.getWeight() != null)
                Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
                        .encodeUrl(CATEGORY_OPT_CAT_AND_WEIGHT));
            else if (gradebookItem.getCategoryName() != null)
                Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
                        .encodeUrl(CATEGORY_OPT_CAT_ONLY));
            else
                Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
                        .encodeUrl(CATEGORY_OPT_NONE));

            int gradeEntry = gradebookService.getGradeEntryType(gradebookUid);
            if (gradeEntry == GradebookService.GRADE_TYPE_PERCENTAGE)
                Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE, URLUtils
                        .encodeUrl(ENTRY_OPT_PERCENT));
            else if (gradeEntry == GradebookService.GRADE_TYPE_POINTS)
                Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE, URLUtils
                        .encodeUrl(ENTRY_OPT_POINTS));
            else
                Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE, URLUtils
                        .encodeUrl(ENTRY_OPT_LETTER));

			String pointsPossible = null;
			if (gradebookItem.getPoints() == null) {
				pointsPossible = Helper.EMPTY_STRING;
			} else {
				pointsPossible = gradebookItem.getPoints().toString();
			}
			Helper.addUrlParam(dataBuilder, P_POINTS_POSSIBLE, pointsPossible);
		}
	}


    /**
   	 * build data packet for get grade purpose
   	 *
   	 * @throws Exception
   	 */
     public String buildPostDataForGrade(String gradebookUid, Long assignmentId, String rosterStudentId) throws Exception {
        StringBuilder dataBuilder = new StringBuilder();


   		// add current user's information
   		teacherParameters(gradebookUid);

   		org.sakaiproject.service.gradebook.shared.Assignment gradebookItem = gradebookService.getAssignment(gradebookUid, assignmentId);
   		if (gradebookItem != null) {
   			String gradebookItemId = gradebookItem.getId().toString();
   			Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ID, URLUtils
                       .encodeUrl(gradebookItemId));
   			String pointsPossible = null;
   			if (gradebookItem.getPoints() == null) {
   				pointsPossible = "";
   			} else {
   				pointsPossible = gradebookItem.getPoints().toString();
   			}
   			Helper.addUrlParam(dataBuilder, P_POINTS_POSSIBLE, pointsPossible);

   			Helper.addUrlParam(dataBuilder, P_GDB_ITEM_NAME, URLUtils
   					.encodeUrl(gradebookItem.getName()));
               if (gradebookItem.getCategoryName() != null && gradebookItem.getWeight() != null)
                   Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
                           .encodeUrl(CATEGORY_OPT_CAT_AND_WEIGHT));
               else if (gradebookItem.getCategoryName() != null)
                   Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
                           .encodeUrl(CATEGORY_OPT_CAT_ONLY));
               else
                   Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
                           .encodeUrl(CATEGORY_OPT_NONE));

               int gradeEntry = gradebookService.getGradeEntryType(gradebookUid);
               if (gradeEntry == GradebookService.GRADE_TYPE_PERCENTAGE)
                   Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE, URLUtils
                           .encodeUrl(ENTRY_OPT_PERCENT));
               else if (gradeEntry == GradebookService.GRADE_TYPE_POINTS)
                   Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE, URLUtils
                           .encodeUrl(ENTRY_OPT_POINTS));
               else
                   Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE, URLUtils
                           .encodeUrl(ENTRY_OPT_LETTER));
   		}

   		addRosterParams(rosterStudentId, dataBuilder, gradebookUid);
        return dataBuilder.toString();
   	}

    /**
   	 * Add a name-value pair to the URL parameter
   	 *
   	 * @return a string
   	 */
   	private void addUrlParam(StringBuilder builder, String paramName,
   			String paramValue) {
   		if (builder.toString().length() != 0) {
   			builder.append("&");
   		}
   		builder.append(paramName);
   		builder.append("=");
   		builder.append(paramValue);
   	}

    public String doiRubricAuthentication(String postData) {

   		boolean isInfoEnabled = LOG.isInfoEnabled();
        String result = null;

   		HttpURLConnection connection = null;
   		DataOutputStream dout = null;

   		if (isInfoEnabled) {
   			LOG.info("Init request URL: " + getInitReqURL());
   		}

   		try {
   			if (isInfoEnabled) {
   				LOG.info("Start connecting to iRubric system");
   			}
   			// connect to iRubric server
   			connection = createHttpURLConnection(
                       getInitReqURL(), getTimeout());
   		} catch (IOException ex) {
   			LOG.error("Cannot request to init to iRubric system.", ex);
   			return null;
   		}

   		try {
   			if (isInfoEnabled) {
   				LOG.info("Posting data to iRubric system");
   			}

   			dout = new DataOutputStream(connection.getOutputStream());
   			dout.writeBytes(postData);
   			dout.flush();
   			dout.close();

   			if (isInfoEnabled) {
   				LOG.info("Obtain return data from iRubric system");
   			}
   			// obtain the security token from iRubric server
   			result = getResponseData(connection);

   		} catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
   		} finally {
               if (connection != null) {
                   connection.disconnect();
               }
   			dout = null;
   			connection = null;
   		}

        return result;
   	}

    public String getData(String secToken) {

        String dataPacket = null;

        HttpURLConnection connection = null;
        try {
            // get grade from iRubric server
            connection = createHttpURLGetConnection(
                    URLUtils.addParameter(getIrubricRedirectUrl(), XTOKEN, secToken),
                    getTimeout());

            dataPacket = getResponseData(connection);

            connection.disconnect();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            // dispose the connection
            if (connection != null) {
                connection = null;
            }
        }

        return dataPacket;
    }

    public void refreshGrades(String gradebookUid, String gradebookItemIdStr) {
        String dataPacket = null;
        // build data packet to send to iRubric system
        Long gradebookItemId = Long.parseLong(gradebookItemIdStr);
        try {
            dataPacket = buildPostDataForGradeAll(gradebookUid, gradebookItemId, CMD_GET_GRADES_BY_GDB);
            LOG.info("Data transferring - " + dataPacket);

            String xToken =  doiRubricAuthentication(dataPacket);

            // authenticate with iRubric system
			if (StringUtils.isNotBlank(xToken)) {
				// authenticating completed
                String sGrade = getData( xToken );

                LOG.info("iRubric - Parsing scores ...");


                if (sGrade != null) { //if have datapacket from IRubric system then proccess datapacket

                    switch (sGrade.charAt(0)) {
                        case 'A':
                            //split value first(value condition)
                            String strScoreStream = sGrade.substring(1).trim();

                            //save grade from response IRubric system
                            saveGradeFromGB2(gradebookUid, gradebookItemId, strScoreStream);
                            break;

                        case 'N':
                            LOG.info("No student has been graded yet for gradebook: " + gradebookUid + " item: " + gradebookItemIdStr);
                            break;

                        case 'E':
                            LOG.info("No rubric attached for gradebook: " + gradebookUid + " item: " + gradebookItemIdStr);
                            break;

                        default:
                            LOG.error("invalid data returned from Irubric system for gradebook: " + gradebookUid + " item: " + gradebookItemIdStr);
                            break;
                    }

                } else {
                    LOG.error("Couldn't receive data from Irubric system for gradebook: " + gradebookUid + " item: " + gradebookItemIdStr);
                }

            }


        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public String buildPostDataForGradeAll(String gradebookUid, Long gradebookItemId, String purpose)
   			throws Exception {
   		StringBuilder dataBuilder = new StringBuilder(buildDefaultPostData(gradebookUid));
   		addUrlParam(dataBuilder, PURPOSE, purpose);

        String studentId = "";

        if (purpose.equals(CMD_GRADE_ALL) && gradebookItemId != 0) {

            //get studentUids by assignmentid(gradebookItemid) from database sakai
            studentId = getiRubricManager().getStudentUIdsByGradebookItemId(gradebookUid, gradebookItemId);

            buildPostDataForGrade(gradebookUid, dataBuilder, gradebookItemId, studentId);

            //issue 53: DN 2012-08-08:add param rubid
            String rubId = getRubricId(gradebookItemId);
            //LOG.error("rubric id form table:"+ rubId);
            if (rubId != null) {
                addUrlParam(dataBuilder, P_RUB_ID, rubId);
            }
        } else if (purpose.equals(CMD_GET_GRADES_BY_GDB)) {

            Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ID, URLUtils
                    .encodeUrl(gradebookItemId.toString()));

            Assignment gradebookItem = gradebookService.getAssignment(gradebookUid, gradebookItemId);
            if (gradebookItem != null) {
                setPointsPossible(gradebookItem.getPoints());
            }
        }


        return  dataBuilder.toString();
    }

    private String getRubricId(long gradebookItemId) {
   		GradableObjectRubric rubric = getiRubricManager().getGradableObjectRubric(gradebookItemId);
   		if (rubric != null) {
   			return rubric.getiRubricId();
   		}
        return null;
   }


	/**
	 * build data packet for get grade purpose
	 * 
	 * @param dataBuilder
	 * @throws Exception
	 */
	@Override
    public void buildPostDataForGrade(String gradebookUid, StringBuilder dataBuilder,
                                      Long assignmentId, String rosterStudentId) throws Exception {

		// add current user's information
		teacherParameters(gradebookUid);

		org.sakaiproject.service.gradebook.shared.Assignment gradebookItem = gradebookService.getAssignment(gradebookUid, assignmentId);
		if (gradebookItem != null) {
			String gradebookItemId = gradebookItem.getId().toString();
			Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ID, URLUtils
                    .encodeUrl(gradebookItemId));
			String pointsPossible = null;
			if (gradebookItem.getPoints() == null) {
				pointsPossible = "";
			} else {
				pointsPossible = gradebookItem.getPoints().toString();
			}
			Helper.addUrlParam(dataBuilder, P_POINTS_POSSIBLE, pointsPossible);
			
			Helper.addUrlParam(dataBuilder, P_GDB_ITEM_NAME, URLUtils
					.encodeUrl(gradebookItem.getName()));
            if (gradebookItem.getCategoryName() != null && gradebookItem.getWeight() != null)
                Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
                        .encodeUrl(CATEGORY_OPT_CAT_AND_WEIGHT));
            else if (gradebookItem.getCategoryName() != null)
                Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
                        .encodeUrl(CATEGORY_OPT_CAT_ONLY));
            else
                Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
                        .encodeUrl(CATEGORY_OPT_NONE));

            int gradeEntry = gradebookService.getGradeEntryType(gradebookUid);
            if (gradeEntry == GradebookService.GRADE_TYPE_PERCENTAGE)
                Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE, URLUtils
                        .encodeUrl(ENTRY_OPT_PERCENT));
            else if (gradeEntry == GradebookService.GRADE_TYPE_POINTS)
                Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE, URLUtils
                        .encodeUrl(ENTRY_OPT_POINTS));
            else
                Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE, URLUtils
                        .encodeUrl(ENTRY_OPT_LETTER));
		}

		addRosterParams(rosterStudentId, dataBuilder, gradebookUid);
	}

   	/**
   	 * Determine whether iRubric is available for a given gradebook item.
   	 *
   	 * @param gradebookItemId
   	 */
   	public boolean isIRubricAvailable(long gradebookItemId) {
   		boolean enabled = false;
   		// TODO We may also want to do the check to see if iRubric is available for
   		// the site, in the event that iRubric was turned off at a later date
   		// for the site.
   		GradableObjectRubric rubric = iRubricManager.getGradableObjectRubric(gradebookItemId);
   		if (rubric != null) {
   			enabled = true;
   		}
   		return enabled;
   	}



   	/**
   	 * DN 2012-06-07: function save grade form Irubric system(use for Gradebook2)
   	 *
   	 * @param gradebookItemId
     * @param strScoreStream(grade from irubric system)(datatype: studenUId1,score1|studenUId2,score2|...)
   	 */
       public void saveGradeFromGB2(String gradebookUuid, long gradebookItemId, String strScoreStream) {

           //check grade request from irubric
           LOG.info("Request Grade from irubric:" + strScoreStream);

           //get assignment by gradebookItemId
           Assignment assignment = gradebookService.getAssignment(gradebookUuid, gradebookItemId);

           //get grade type
           int gradeType = gradebookService.getGradeEntryType(gradebookUuid);

           String[] records = strScoreStream.split("\\|");
           int length = records.length;

           if (length > 0) {

               for (int i = 0; i < length; i++) {

                   //score(grade) when split
                   String score = records[i].split("\\,")[1].trim();
                   String studentUId = records[i].split("\\,")[0].trim();

				   //issue 313: check if user can grade assignment
				   if( gradebookService.isUserAbleToGradeItemForStudent(gradebookUuid, assignment.getId(), studentUId) ){

					   String oldScore = gradebookService.getAssignmentScoreString(gradebookUuid, assignment.getId(), studentUId);

					   //update grade if its new or its changed
					   if (oldScore == null || !score.equals(oldScore)) {
						   gradebookService.saveGradeAndCommentForStudent(gradebookUuid, new Long(gradebookItemId), studentUId, score.toString(), null);
					   }
		   }
               }

           }

       }


    /**
	 * @return the pointsPossible
	 */
	@Override
    public Double getPointsPossible() {
		return pointsPossible;
	}

	/**
	 * @param pointsPossible the pointsPossible to set
	 */
	public void setPointsPossible(Double pointsPossible) {
		this.pointsPossible = pointsPossible;
	}

    public IRubricManager getiRubricManager() {
        return iRubricManager;
    }

    public void setiRubricManager(IRubricManager iRubricManager) {
        this.iRubricManager = iRubricManager;
    }

	public void renderErrorMessageByCmd(PrintWriter printWriter, String cmd,
			String errorMsg) {

		if (cmd.equals(CMD_GET_GRADES_BY_GDB)
				|| cmd.equals(CMD_GET_GRADES_BY_ROS)) {
			printWriter
					.print("<html><body onload=\"window.parent.alertMsgByCmd('allgrades', '"
							+ errorMsg + "');\"></body></html>");
		} else if (cmd.equals(CMD_GET_GRADE)) {
			printWriter
					.print("<html><body onload=\"window.parent.alertMsgByCmd('getGradeFrame', '"
							+ errorMsg + "');\"></body></html>");
		} else {
			printWriter.print(renderErrorMessageBox(errorMsg));
		}
	}


	private String renderErrorMessageBox(String errorMsg) {
		StringBuilder builder = new StringBuilder(
				"<br/><br/><div align=center>");
		builder.append(errorMsg);
		builder.append("</div>");

		return builder.toString();
	}
}
