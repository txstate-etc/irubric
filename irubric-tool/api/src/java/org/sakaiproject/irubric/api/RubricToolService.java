package org.sakaiproject.irubric.api;

import java.util.List;
import java.lang.String;
import org.sakaiproject.component.api.ServerConfigurationService;
//import org.sakaiproject.entity.api.EntityProducer;

/**
 * <p>
 * RubricToolService is the service that handles irubric.
 * </p>
 * 
 */
public interface RubricToolService
{
	/** Security lock for irubric home. */
	public static final String SECURE_IRUBRIC_HOME = "irubric.irubrichome";

	/** Security lock for my rubric*/
	public static final String SECURE_MY_RUBRIC = "irubric.myrubrics";

	/** Security lock for gallery rurbic. */
	public static final String SECURE_ACCESS_GALLERY_IRUBRIC = "irubric.accessgallery";

	/** Security lock for build irubric. */
	public static final String SECURE_BUILD_IRUBRIC = "irubric.build";

	/** Security lock for assessment. */
	public static final String SECURE_COLLABORATIVE_ASSESSMENTS_IRUBRIC = "irubric.collaborativeassessments";

	/** Security lock for iRubric report*/
	public static final String SECURE_IRUBRIC_REPORT = "irubric.reports";
	
	/** Security lock for Grades*/
	public static final String SECURE_GRADES = "irubric.grades";

	/** Security lock for irubric eportfolios */
	public static final String SECURE_EPORTFOLIOS = "irubric.eportfolios";

	/** Security lock for build eportfolios */
	public static final String SECURE_BUILD_EPORTFOLIOS = "irubric.buildeportfolios";

	/** Security lock for assignment matrices */
	public static final String SECURE_ASSIGNMENT_MATRICES = "irubric.assmatrices";

	//check permission build rubric
	public boolean allowiRubricHome(String userid, String resourceString);

	//check permission my rubric
	public boolean allowMyRubrics(String userid, String resourceString);

	//check permission access gallery irubric
	public boolean allowAccessGalleryiRubric(String userid, String resourceString);

	//check permission build rubric
	public boolean allowBuildiRubric(String userid, String resourceString);

	//check permission access Assessment
	public boolean allowAssessmentiRubric(String userid, String resourceString);

	//check permission iRubric report 
	public boolean allowiRubricReports(String userid, String resourceString); 

	//check permission grades
	public boolean allowGrades(String userid, String resourceString); 

	//check permission My ePortfolios
	public boolean allowMyePortfolios(String userid, String resourceString); 

	//check permission build ePortfolios
	public boolean allowBuildePortfolios(String userid, String resourceString); 

	//check permission assignment matrices
	public boolean allowAssMatrices(String userid, String resourceString); 

	//get list assignment (gradebook item)
	public List getAssignmentsByGradebookUId(String gradebookUId, String sort, String ascending);
	
	//DN 2013-09-11: if irubric.switch=2 then some course show icon grade, get grade, summary report(grading)
	public boolean allowShowiRubricLink();
	
	public boolean allowShowiRubricLink(String siteId);

	//DN 2013-09-12: able to grade
	public boolean ableToGrade(String gradebookUId);

}