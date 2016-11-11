package org.sakaiproject.irubric.model;

import java.util.*;


import org.sakaiproject.service.gradebook.shared.ConflictingAssignmentNameException;
import org.sakaiproject.service.gradebook.shared.StaleObjectModificationException;
import org.sakaiproject.tool.gradebook.LetterGradePercentMapping;

public interface IRubricManager {
   
	/**
     * Updates a GradableObjectRubric object
	 * 
	 * @param gradableObjectRubric A GradableObjectRubric object
	 * @return void
     */
    public void updateGradableObjectRubric(GradableObjectRubric gradableObjectRubric)
    						throws ConflictingAssignmentNameException, StaleObjectModificationException;

    /**
     * Get a GradableObjectRubric object by assignment
     *
     * @param assignmentId The assignment ID
     * @return A GradableObjectRubric object
     */
    public GradableObjectRubric getGradableObjectRubric(Long gradableObjectId);
    
    /**
     * Get all GradableObjectRubrics object by a list of assignment parameters
     *
     * @param assignmentIds The assignment IDs
     * @return A List of GradableObjectRubric objects
     */
    public List getGradableObjectRubrics(List<Long> gradableObjectIds);
 
    /*
     * DN 2012-05-28: defined function get studentUIds by gradebookItemId/assignemntId
     * @param gradebookItemId
     * @return String: studentUIds
     */
    public String getStudentUIdsByGradebookItemId(String gradebookUid, Long gradebookItemId);
    

    /*
     * DN 2012-09-26: get gradebookitemId by externalId and gradebookUId
     * @param name: externalId of gradebook item
     * @param gradebookUid of gradebook
     * @return Long: gradebookItemId
     */
    public Long getGradableObjectIdByExternalId(final String name,final String gradebookUid) ;
    
    /**
     * Copy all of the rubric associations from one gradebook to another for gradebook items
     * with the same name
     * @param fromGradebookUid the gradebookUid of the gradebook rubric associations you would like to copy
     * @param toGradebookUid the gradebookUid of the gradebook you would like to copy the associations to
     */
    public void copyRubricAssociations(String fromGradebookUid, String toGradebookUid);

    /**
     * 
     * @param gradebookUid
     * @return a list of the {@link org.sakaiproject.irubric.model.GradableObjectRubric}s associated
     * with gradebook items in the given gradebook
     */
    public List<GradableObjectRubric> getAllRubricsInGradebook(String gradebookUid);
    
    /**
     * 
     * @param gradebookUid
     * @param gbItemId
     * @return true if the given gradebook item has been released to students
     */
    public boolean isGradebookItemReleased(String gradebookUid, Long gbItemId);
}

