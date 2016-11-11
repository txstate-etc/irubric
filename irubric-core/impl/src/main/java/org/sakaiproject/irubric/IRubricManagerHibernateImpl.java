package org.sakaiproject.irubric;

import java.util.*;
import java.util.Map.Entry;
import java.sql.SQLException;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;

import org.sakaiproject.irubric.model.GradableObjectRubric;
import org.sakaiproject.irubric.model.IRubricManager;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.service.gradebook.shared.GradebookNotFoundException;
import org.sakaiproject.service.gradebook.shared.GradebookService;



import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * Manages Rubric persistence via hibernate.
 */
public class IRubricManagerHibernateImpl extends HibernateDaoSupport
							implements IRubricManager {
	
	public static int MAX_NUMBER_OF_SQL_PARAMETERS_IN_LIST = 1000;
	
	GradebookService gradebookService;
	public void setGradebookService(GradebookService gradebookService) {
		this.gradebookService = gradebookService;
	}

    /**
     * Update a GradableObjectRubric object
     *
     * @param gradableObjectRubric
	 * @return void
     */
	public void updateGradableObjectRubric(final GradableObjectRubric gradableObjectRubric){
    	HibernateCallback hc = new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
            	session.evict(gradableObjectRubric);
                if (gradableObjectRubric.getiRubricId() == null) {
                    session.delete(gradableObjectRubric);
                }
                else {
        			session.saveOrUpdate(gradableObjectRubric);
                }
                return null;
            }
        };
        getHibernateTemplate().execute(hc);
    }
	
	/**
     * Get a GradableObjectRubric object by assignment
     *
     * @param assignmentId
	 * @return GradableObjectRubric
     */
    public GradableObjectRubric getGradableObjectRubric(Long gradableObjectId) {
    	//logger.info(gradableObjectId);
    	String hql = "from GradableObjectRubric as gr where gr.gradableObjectId=?";
    	if (getHibernateTemplate().find(hql, gradableObjectId).size()>0)
    		return (GradableObjectRubric)(getHibernateTemplate().find(hql, gradableObjectId)).get(0);
    	return null;
    }
    
    /**
     * Get a GradableObjectRubric objects by a list of assignment parameters
     *
     * @param gradableObjectIds
    * @return List of gradableObjectRubrics
     */
    public List getGradableObjectRubrics(final List<Long> gradableObjectIds) {
        HibernateCallback hc = new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException ,SQLException {
                List gradableObjectRubrics = new ArrayList();

                if (gradableObjectIds != null && !gradableObjectIds.isEmpty()) {
                    String hql = "from GradableObjectRubric as gr where (gr.gradableObjectId in (:gradableObjectIdList) and (gr.iRubricId <> null))";
                    Query query = session.createQuery(hql);

                    gradableObjectRubrics = queryWithParameterList(query, "gradableObjectIdList", gradableObjectIds);
                }

                return gradableObjectRubrics;
            }
        };
        return (List)getHibernateTemplate().execute(hc);

    }
    
    /**
     * 
     * @param query - your query with all other parameters already defined
     * @param queryParamName - the name of the list parameter referenced in the query
     * @param fullList - the list that you are using as a parameter
     * @return the resulting list from a query that takes in a list as a parameter;
     * this will cycle through with sublists if the size of the list exceeds the
     * allowed size for an sql query
     */
    private List queryWithParameterList(Query query, String queryParamName, List fullList) {
        // sql has a limit for the size of a parameter list, so we may need to cycle
        // through with sublists
        List queryResultList = new ArrayList();

        if (fullList.size() < MAX_NUMBER_OF_SQL_PARAMETERS_IN_LIST) {
            query.setParameterList(queryParamName, fullList);
            queryResultList = query.list();

        } else {
            // if there are more than MAX_NUMBER_OF_SQL_PARAMETERS_IN_LIST, we need to do multiple queries
            int begIndex = 0;
            int endIndex = 0;

            while (begIndex < fullList.size()) {
                endIndex = begIndex + MAX_NUMBER_OF_SQL_PARAMETERS_IN_LIST;
                if (endIndex > fullList.size()) {
                    endIndex = fullList.size();
                }
                List tempSubList = new ArrayList();
                tempSubList.addAll(fullList.subList(begIndex, endIndex));

                query.setParameterList(queryParamName, tempSubList);

                queryResultList.addAll(query.list());
                begIndex = endIndex;
            }
        }

        return queryResultList;
    }
    
    /**
     * DN 2012-05-28: defined function get studentUIds by gradebookItemId/assignemntId
     * @param gradebookItemId
     * @return String: studentUIds("studentUId1,studentUId2,...")
	 */
    public String getStudentUIdsByGradebookItemId(String gradebookUid, Long gradebookItemId){
    	String studentUids = "";
    	
    	// retrieve only the students the current user is allowed to grade
    	Map<String, String> studentIdFunctionMap = gradebookService.getViewableStudentsForItemForCurrentUser(gradebookUid, gradebookItemId);
    	
    	// We now need to iterate through the map to identify the students that
    	// the user may actually grade, not just view
    	for (Iterator iterator = studentIdFunctionMap.entrySet().iterator(); iterator.hasNext();) {
			Entry entry = (Entry) iterator.next();
			String studentUid = (String)entry.getKey();
			String viewOrGrade = (String)entry.getValue();
			if (GradebookService.gradePermission.equals(viewOrGrade)) {
				studentUids += studentUid + ",";
			}
    	}
    	
    	return studentUids;
    }
    
    //DN 2012-09-21:get gradeobject id by name assignment(gradeobject) and gradebookUid
    //use for assignment and site-manage(copy site)
	public Long getGradableObjectId(final String name,final String gradebookUid) {
		
		HibernateCallback hcbObj = new HibernateCallback() 
		{
			public Object doInHibernate(Session session) throws HibernateException {
		    	Query q = session.createQuery("select g.id from GradableObject as g where g.gradebook.uid=? and g.name = ? and g.removed=false");
		    	q.setParameter(0, gradebookUid, Hibernate.STRING);
		    	q.setParameter(1, name, Hibernate.STRING);
		    	return q.uniqueResult();
		    }
		};
		Long gradeObjectId = (Long) getHibernateTemplate().execute(hcbObj);	
		
		return gradeObjectId;
	}
	
	//DN 2012-09-25:get gradableobject id by externalId and gradebookUId
	public Long getGradableObjectIdByExternalId(final String name,final String gradebookUid) {
		
		HibernateCallback hcbObj = new HibernateCallback() 
		{
			public Object doInHibernate(Session session) throws HibernateException {
		    	Query q = session.createQuery("select g.id from GradableObject as g where g.gradebook.uid=? and g.externalId = ? and g.removed=false and g.externallyMaintained=true and g.externalAppName= ?");
		    	q.setParameter(0, gradebookUid, Hibernate.STRING);
		    	q.setParameter(1, name, Hibernate.STRING);
		    	q.setParameter(2, "Assignments", Hibernate.STRING);
		    	return q.uniqueResult();
		    }
		};
		Long gradeObjectId = (Long) getHibernateTemplate().execute(hcbObj);	
		
		return gradeObjectId;
	}
  
    /*
    *   check have attach irubric in gradebook item
    *   @param assignmentid - gradebookitem id (Long)
    *   return true if have attach irubric, false is otherwise
    */
    private boolean isHaveAttach(Long assignmentId) {

        if(getGradableObjectRubric(assignmentId) != null)
            return true;

        return false;
    }
    
    public List<GradableObjectRubric> getAllRubricsInGradebook(String gradebookUid) {
    	List<GradableObjectRubric> rubrics = new ArrayList<GradableObjectRubric>();
    	
    	// retrieve all of the gradebook items in the gradebook
    	List<Assignment> fromGbItems = gradebookService.getAssignments(gradebookUid);
        List<Long> fromGbItemIds = getGradebookItemIds(fromGbItems);

        if(fromGbItems != null && !fromGbItems.isEmpty()) {

            // retrieve the rubrics in the site we are importing, if they exist
            rubrics = getGradableObjectRubrics(fromGbItemIds);
        }
    	
    	return rubrics;
    }
    
    public void copyRubricAssociations(String fromGradebookUid, String toGradebookUid) {

            // get the gb items in the site you are importing
            List<Assignment> fromGbItems = gradebookService.getAssignments(fromGradebookUid);
            List<Long> fromGbItemIds = getGradebookItemIds(fromGbItems);

            if(fromGbItems != null && !fromGbItems.isEmpty()) {

                // retrieve the rubrics in the site we are importing, if they exist
                List<GradableObjectRubric> fromRubrics = getGradableObjectRubrics(fromGbItemIds);

                // if rubrics exist, we need to link the new gb items to the same rubrics
                if (fromRubrics != null && !fromRubrics.isEmpty()) {
                    // put the rubrics in a map: gb item id --> rubric
                    Map<Long, GradableObjectRubric> fromRubricMap = new HashMap<Long, GradableObjectRubric>();
                    for (GradableObjectRubric rubric : fromRubrics) {
                        fromRubricMap.put(rubric.getGradableObjectId(), rubric);
                    }

                    // get the gb items in the site we are importing into
                    List<Assignment> toGbItems = gradebookService.getAssignments(toGradebookUid);

                    if (toGbItems != null && !toGbItems.isEmpty()) {
                        // put the gb item name and id in a map for easy access
                        // later on
                        Map<String, Long> toGbItemMap = new HashMap<String, Long>();
                        for (Assignment toGbItem : toGbItems) {
                            toGbItemMap.put(toGbItem.getName(), toGbItem.getId());
                        }

                        for(Assignment fromGbItem : fromGbItems) {
                            // if the new site contains a gb item with the same name,
                            // check to see if there is a rubric attached in the site
                            // we are importing
                            if (toGbItemMap.containsKey(fromGbItem.getName()) &&
                                    fromRubricMap.containsKey(fromGbItem.getId())) {

                                GradableObjectRubric rubric = fromRubricMap.get(fromGbItem.getId());

                                if (rubric != null) {
                                    Long toGbItemId = toGbItemMap.get(fromGbItem.getName());
                                    GradableObjectRubric newobjRubric = new GradableObjectRubric(toGbItemId, rubric.getiRubricId(), rubric.getiRubricTitle());
                                    getHibernateTemplate().saveOrUpdate(newobjRubric);
                                }
                            }
                        }
                    }
                }
            }
        }
    
    /**
     * 
     * @param gbItems
     * @return a convenience method - 
     * given a list of gradebook items, returns a list of their ids
     */
    private List<Long> getGradebookItemIds(List<Assignment> gbItems) {
        List<Long> gbItemIds = new ArrayList<Long>();
        if(gbItems != null && !gbItems.isEmpty()) {
            for (Assignment gbItem : gbItems) {
                gbItemIds.add(gbItem.getId());
            }
        }

        return gbItemIds;
    } 
    
    public boolean isGradebookItemReleased(String gradebookUid, Long gbItemId) {
    	Assignment gbItem = gradebookService.getAssignment(gradebookUid, gbItemId);
    	return gbItem.isReleased();
    }
    
}

