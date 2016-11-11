/*Copyright (C) Reazon Systems, Inc.  All rights reserved.*/
/*
 * ERROR CODE DEFINITION
 * ---------------------
 * RZN9834953
 * 		Cannot authenticate with iRubric
 * RZN9832413
 * 		Invalid returned value for getallgrade purpose
 * RZN9834745
 * 		Invalid returned xToken from iRubric
 * RZN8345123
 * 		Error viewing the iRubric by student role
 * RZN9862813
 * 		Error when attaching an iRubric
 * RZN3534953
 * 		Invalid returned value from iRubricRZN3534953
 * RZN9831153
 * 		Empty Purpose parameter value
 */
package org.sakaiproject.irubric;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.irubric.model.GradableObjectRubric;
import org.sakaiproject.irubric.model.IRubricService;
import org.sakaiproject.portal.util.URLUtils;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.tool.gradebook.facades.Authn;
import org.sakaiproject.tool.gradebook.facades.Authz;
import org.sakaiproject.tool.gradebook.facades.ContextManagement;
import org.sakaiproject.util.ResourceLoader;

/**
 * A servlet to integrate with iRubric system
 * 
 * @author CD
 * 
 */
public class IRubricServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1720367954299139347L;

	private static Log LOG = LogFactory.getLog(IRubricServlet.class);

	private static final String XTOKEN = "xtoken";
	private static final String CMD = "p";
	private static final String CMD_VIEW = "v";
	private static final String CMD_VIEW_UNGRADED = "vu";
	private static final String CMD_ATTACH = "a";
	private static final String CMD_GRADE = "g";
	private static final String CMD_GRADE_ALL = "ga";
	private static final String CMD_GET_ATTACHED_RUBRIC = "getattachedrubric";
	private static final String CMD_GET_GRADE = "gg";
	private static final String CMD_GET_GRADES_BY_GRADEBOOK = "gag";
	private static final String CMD_GET_GRADES_BY_ROS = "gas";
	private static final String FIELD = "fieldToUpdate";
	private static final String ERR_IRUBRIC_UNAVAILABLE = "Sorry, cannot contact iRubric at this time. Please try again in a few minutes. <BR><BR>Should the problems persists, contact your system administrator.";
    private static final String P_RUB_ID = "rubricId";
    //DN 2012-06-04:variables for the redirect link in gradebook and gradebook2
    private static final String TOOL = "t";
    private static final String GB2 = "gb2";
    private static final String CMD_RUBRIC_AVAILALBE = "ra";

    private static final String ERR_MSG = "<br/><br/><div align=center>Error {errorcode}. Please contact your system administator.</div>";

    private static final String IRUBRIC_BUNDLE = "Messages";

	// iRubric bean class name
	private static final String IRUBRIC_SERVICE = IRubricService.class.getName();
	private IRubricService iRubricService;
	private Authz authzService;
	private Authn authnService;
    private ContextManagement contextMgm;
    private SessionManager sessionManager;
    private SecurityService securityService;
    private SiteService siteService;
    private GradebookService gradebookService;
    private ToolManager toolManager;
    
    private static ResourceLoader rl = new ResourceLoader(IRUBRIC_BUNDLE);


	/*
	 * Initialize for servlet (non-Javadoc)
	 * 
	 * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		iRubricService = (IRubricService) ComponentManager.get(IRUBRIC_SERVICE);
		authzService = (Authz) ComponentManager.get("org_sakaiproject_tool_gradebook_facades_Authz");
		authnService = (Authn) ComponentManager.get("org_sakaiproject_tool_gradebook_facades_Authn");
        contextMgm = (ContextManagement) ComponentManager.get("org_sakaiproject_tool_gradebook_facades_ContextManagement");
		sessionManager = (SessionManager) ComponentManager.get("org.sakaiproject.tool.api.SessionManager");
        toolManager = (ToolManager) ComponentManager.get("org.sakaiproject.tool.api.ToolManage");
        securityService = (SecurityService) ComponentManager.get("org.sakaiproject.authz.api.SecurityService");
        siteService = (SiteService) ComponentManager.get("org.sakaiproject.site.api.SiteService");
        gradebookService = (GradebookService) ComponentManager.get("org_sakaiproject_service_gradebook_GradebookService");
	}

    public String getCurrentContext(HttpServletRequest request) {
        return request.getParameter("siteId");
    }
    
    public String getCurrentSiteId() {
    	if (toolManager != null && toolManager.getCurrentPlacement() != null && toolManager.getCurrentPlacement().getContext() != null){
			return toolManager.getCurrentPlacement().getContext();

		} else {
			return null;
		}
    }

	/**
	 * doGet method
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}

    /**
     * doPost method
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        PrintWriter writer = response.getWriter();

        String cmd = "";
        try {
            cmd = request.getParameter(CMD).toLowerCase();
        } catch (NullPointerException ex) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Error while getting purpose name.", ex);
            }
            iRubricService
                    .renderErrorMessageByCmd(writer, cmd,
                            "Error while getting purpose name. Please contact your system administrator.");
        }

        if (cmd.equals("")) {
            return;
        }

        String gradebookUid = null;
        String siteId = null;
        if (request.getParameter("gradebookUid") != null && !"null".equals(request.getParameter("gradebookUid"))) {
            gradebookUid = request.getParameter("gradebookUid");
        }
        
        if (request.getParameter("siteId") != null && !"null".equals(request.getParameter("siteId"))) {
            siteId = request.getParameter("siteId");
        }

        if (!cmd.equals(CMD_GET_ATTACHED_RUBRIC)) {         
            if (gradebookUid == null) {
                authnService.setAuthnContext(request);
                gradebookUid = contextMgm.getGradebookUid(request);
            }

            String rosterStudentId = request.getParameter("rosterStudentId");
            if (!isAuthorized(cmd, gradebookUid, rosterStudentId)) {
                StringBuilder path = new StringBuilder(request.getContextPath());
                path.append("/noRole.jsp");
                response.sendRedirect(path.toString());
                return;
            }
        }

        // get the PURPOSE name
        if (cmd.equals(CMD_GET_ATTACHED_RUBRIC)) {
            String xToken = request.getParameter(XTOKEN);
            String responseData = getData(xToken, null, cmd);

            LOG.info("The attached rubric data: " + responseData);

            if (responseData != null) {
                switch (responseData.charAt(0)) {
                    case 'A':
                        // get the attached rubric data
                        String sTemp = responseData.trim().substring(1);
                        String[] datas = sTemp.split("\\|");
                        if (datas.length > 0) {
                            Long gradebookItemId = Long.parseLong(datas[0]);
                            String iRubricId = datas[1];
                            String iRubricTitle = datas[2];
                            try {
                                iRubricService.updateAssignmetByRubric(
                                        gradebookItemId, iRubricId, iRubricTitle);
                                if (LOG.isInfoEnabled())
                                    LOG.info("Update gradebook item is successful.");
                            } catch (Exception e) {
                                LOG.error("Error while updating gradebook item.", e);
                            }
                        }
                        break;
                    default:
                        LOG.error("The reponse data is in invalid format.");
                        break;
                }
            } else {
                LOG.error("Error while getting data from iRubric Server.");
            }
        } else {

            String dataPacket = null;
            // build data packet to send to iRubric system
            try {
                dataPacket = buildPostData(request, cmd);
                LOG.info("Data trasnfering - " + dataPacket);
            } catch (Exception ex) {
                LOG.error("Constructing data packet", ex);
                renderErrorMessageByCmd(
                                writer,
                                cmd,
                                "Error while constructing data packet. Please contact your system administrator.");
            }

            // authenticate with iRubric system

            try {

                String xToken = iRubricService.doiRubricAuthentication(dataPacket);
                if (StringUtils.isNotBlank(xToken)) {
                    switch (xToken.charAt(0)) {
                        case 'T':
                            //Gradebook currentGradebook = (Gradebook) iRubricService.getGradebookService().getGradebook(gradebookUid);
                            if (cmd.equals(CMD_ATTACH) || 
                                cmd.equals(CMD_VIEW) || 
                                cmd.equals(CMD_VIEW_UNGRADED) ||
                                cmd.equals(CMD_GRADE)) {
                                String redirecLink = URLUtils.addParameter(iRubricService
                                        .getIrubricRedirectUrl(), XTOKEN, xToken);
                                response.sendRedirect(redirecLink);

                                LOG.info("iRubric - " + redirecLink);

                            } else if (cmd.equals(CMD_GRADE_ALL)) {
                                String redirecLink = URLUtils.addParameter(iRubricService
                                        .getIrubricRedirectUrl(), XTOKEN, xToken);

                                //add use check, will use type redirect GB OR GB2
                                // String tool = request.getParameter(TOOL);
                                // if (tool != null && tool.toLowerCase().equals("gb2")) {//redirect link in gradebook2
                                //     //DN 2012-06-04: used for redirect link in GB2
                                //     response.sendRedirect(redirecLink);

                                // } else {
                                //     response.setContentType("text/plain");
                                //     writer.print(redirecLink);
                                // }
                                response.sendRedirect(redirecLink);

                            } else if (cmd.equals(CMD_GET_GRADE)) {
                                String sGrade = getData(xToken, writer, cmd);
                                LOG.info("grade array: - " + sGrade);
                                String newScore = null;
                                
                                if (sGrade != null) {
                                    switch (sGrade.charAt(0)) {
                                        case 'R':
                                            newScore = sGrade.substring(1).trim();
                                            LOG.debug("New Score from FC:  " + newScore);
                                            if (newScore != null) {
                                                response.setContentType("application/json");
                                                writer.print("{\n\"score\": "+ newScore + "}");
                                            }
                                            break;
                                        case 'N':
                                            LOG.debug("iRubric has not graded this item.");
                                            response.setContentType("application/json");
                                            writer.print("{\"message\": \""+ rl.getString("irubric.ungradedConfirm") + "\"}");
                                            break;
                                        case 'E':
                                            LOG.debug("iRubric does not have a rubric attached for this gradebook item.");
                                            response.setContentType("application/json");
                                            writer.print("{\"message\": \""+ rl.getString("irubric.noRubricAttached") + "\"}");
                                            break;
                                        default:
                                            LOG.error("Invalid returned value from iRubric system.  Returned value - " + sGrade);
                                            response.setContentType("application/json");
                                            writer.print("{\"message\": \""+ rl.getString("irubric.error") + "\"}");
                                            break;
                                    }
                                }

                            } else if (cmd.equals(CMD_GET_GRADES_BY_GRADEBOOK)
                                    || cmd.equals(CMD_GET_GRADES_BY_ROS)) {
                                String sGrade = getData(xToken, writer, cmd);

                                LOG.info("iRubric - Parsing scores ...");

                                //DN 2012-06-07:for GB2
                                //add use check, will use redirect type GB OR GB2
                                String tool = request.getParameter(TOOL);
                                if (tool != null && tool.toLowerCase().equals(GB2)) {//use in gradebook2
                                    if (sGrade != null) { //if have datapacket from IRubric system then proccess datapacket
                                        switch (sGrade.charAt(0)) {
                                            case 'A':
                                                String strGradebookItemId = request.getParameter("gradebookItemId");
                                                Long gradebookItemId = new Long(strGradebookItemId);
                                                //split value first(value condition)
                                                String strScoreStream = sGrade.substring(1).trim();

                                                //save grade from response IRubric system
                                                iRubricService.saveGradeFromGB2(gradebookUid, gradebookItemId, strScoreStream);
                                                writer.print('A');
                                                break;

                                            case 'N':
                                                // No student has been graded yet.
                                                writer.print('N');
                                                break;

                                            case 'E':
                                                //no attach irurbic
                                                writer.print('E');
                                                break;

                                            default:
                                                //invalid data return from Irubric system.
                                                writer.print('I');
                                                break;
                                        }

                                    } else {
                                        //Could n't received data from Irubric system.
                                        writer.print("C");
                                    }

                                } else {
                                    processAllGrades(writer, sGrade, cmd, gradebookUid, request, response);
                                }
                            } else if (cmd.equals(CMD_RUBRIC_AVAILALBE)) {
                                String gItemIdStr = request.getParameter("gradebookItemId");
                                String[] gItems = gItemIdStr.split(",");
                                response.setContentType("application/json");
                                writer.print("{");
                                for (int i = 0; i < gItems.length; i++) {

                                    boolean available = iRubricService.isIRubricAvailable(Long.parseLong(gItems[i]));
                                    if (available) {
                                        writer.print("\n\"" + gItems[i] + "\": true");
                                    } else {
                                        writer.print("\n\"" + gItems[i] + "\": false");
                                    }
                                    if (i != gItems.length - 1) {
                                        writer.print(",");
                                    }
                                }
                                writer.print("}");

                            } else { // pass the other CMD
                            	String redirecLink = URLUtils.addParameter(iRubricService
                            			.getIrubricRedirectUrl(), XTOKEN, xToken);
                            	response.sendRedirect(redirecLink);

                            	LOG.info("iRubric - " + redirecLink);
                            }
                            	
                            break;
                        case 'E':
                            dumpErrorMessage(writer, cmd, "RZN9834745", response);
                            LOG.debug("Error generating security token on iRubric system.");
                            break;
                        default:

                            break;
                    }
                } else {
                    LOG.error("Cannot authenticate with iRubric.");
                }

                writer.close();
            } catch (Exception e) {
                LOG.error(ERR_IRUBRIC_UNAVAILABLE, e);
                renderErrorMessageByCmd(writer, cmd, ERR_IRUBRIC_UNAVAILABLE);
            }
        }
    }



    /**
   	 * @param request
   	 * @param purpose
   	 * @return
   	 * @throws Exception
   	 */
   	public String buildPostData(HttpServletRequest request, String purpose)
   			throws Exception {
   		StringBuilder dataBuilder = new StringBuilder(iRubricService.buildDefaultPostData(request.getParameter("siteId")));
        String gradebookUid = getCurrentContext(request);
        
        // Translate the CMD_VIEW_UNGRADED into CMD_VIEW for sending to iRubric
        String iRubricPurpose = purpose;
        if (CMD_VIEW_UNGRADED.equals(purpose)) {
        	iRubricPurpose = CMD_VIEW;
        }
   		Helper.addUrlParam(dataBuilder, iRubricService.PURPOSE, iRubricPurpose);

   		if (purpose.equals(CMD_ATTACH)) {
               String urlParam = request.getParameter(iRubricService.P_GDB_ITEM_ID);
               Long gradebookItemId = new Long(urlParam);



            iRubricService.buildPostDataForAttach(dataBuilder, gradebookUid, gradebookItemId);
                              			//issue 53: DN 2012-08-08:add param rubid
			String rubId = getRubricId(gradebookItemId);
			//LOG.error("rubric id form table:"+ rubId);
			if(rubId != "") {
				Helper.addUrlParam(dataBuilder, P_RUB_ID, rubId);
			}


   		} else if (purpose.equals(CMD_GRADE) || 
   				purpose.equals(CMD_VIEW) || 
   				purpose.equals(CMD_GRADE_ALL) ||
   				purpose.equals(CMD_VIEW_UNGRADED)) {               /*String studentId = request.getParameter(iRubricService.P_ROS_ID);  */

               String urlParam = request.getParameter(IRubricService.P_GDB_ITEM_ID);


               String studentId = "";
               if (CMD_VIEW_UNGRADED.equals(purpose)) {
            	   // the call requires a studentId, so just send the current user
            	   // we won't be retrieving grade info for this student
            	   studentId = authnService.getUserUid();
               } else if (!purpose.equals(CMD_GRADE_ALL)) {
                   studentId = request.getParameter(IRubricService.P_ROS_ID);
               }

               Long gradebookItemId = new Long(urlParam);
//               iRubricService.buildPostDataForGrade(gradebookUid, gradebookItemId, studentId);
               //DN 2012-05-28:change get studentUids from database sakai(not pass param)
               //use for case gradeAll
               if (purpose.equals(CMD_GRADE_ALL) && gradebookItemId != 0) {

                   //get studentUids by assignmentid(gradebookItemid) from database sakai
                   studentId = iRubricService.getiRubricManager().getStudentUIdsByGradebookItemId(gradebookUid, gradebookItemId);
               }
               
               // do not display grade if not released in gradebook
               if (CMD_VIEW.equals(purpose) && gradebookItemId != 0) {
            	   boolean gradePublished = iRubricService.getiRubricManager().isGradebookItemReleased(gradebookUid, gradebookItemId);
            	   if (!gradePublished) {
            		   Helper.addUrlParam(dataBuilder, IRubricService.P_GRADE_PUBLISHED, "true");
            	   }
               }
               
               // do not display grade if using the view ungraded command
               if (CMD_VIEW_UNGRADED.equals(purpose)) {
            	   Helper.addUrlParam(dataBuilder, IRubricService.P_GRADE_PUBLISHED, "false");  
               }
               
               iRubricService.buildPostDataForGrade(gradebookUid, dataBuilder, gradebookItemId, studentId);

               //issue 53: DN 2012-08-08:add param rubid
               String rubId = getRubricId(gradebookItemId);
               //LOG.error("rubric id form table:"+ rubId);
               if (rubId != "") {
                   Helper.addUrlParam(dataBuilder, P_RUB_ID, rubId);
               }

   		} else if (purpose.equals(CMD_GET_GRADE)) {
   			String urlParam = request.getParameter(IRubricService.P_GDB_ITEM_ID);

   			Long gradebookItemId = new Long(urlParam);
   			iRubricService.addGradebookParams(gradebookUid, gradebookItemId, dataBuilder);

   			String studentId = request.getParameter(IRubricService.P_ROS_ID);
   			iRubricService.addRosterParams(studentId, dataBuilder, gradebookUid);

   		} else if (purpose.equals(IRubricService.CMD_GET_GRADES_BY_GDB) || 
   				IRubricService.CMD_SUMMARY_REPORT.equals(purpose)) {
   			String gradebookItemId = request.getParameter(IRubricService.P_GDB_ITEM_ID);
               Helper.addUrlParam(dataBuilder, IRubricService.P_GDB_ITEM_ID, URLUtils
                       .encodeUrl(gradebookItemId));

   			//Assignment gradebookItem = iRubricService.getGradebookService().getAssignment(gradebookUid, Long.parseLong(gradebookItemId));
   			//if (gradebookItem != null) {
   			//	setPointsPossible(gradebookItem.getPoints());
   		    //	}

   		} else if (purpose.equals(CMD_GET_GRADES_BY_ROS)) {
   			String rosterStudentId = request.getParameter(IRubricService.P_ROS_ID);
               Helper.addUrlParam(dataBuilder, IRubricService.P_ROS_ID, URLUtils
                       .encodeUrl(rosterStudentId));
   		}

   		return dataBuilder.toString();
   	}

    	//Issue 53:DN 2012-08-08: function get rubricid
    	private String getRubricId(long gradebookItemId) {
    		GradableObjectRubric rubric = iRubricService.getiRubricManager().getGradableObjectRubric(gradebookItemId);
    		if (rubric != null) {
    			return rubric.getiRubricId();
    		} else {
    			return "";

    		}
    	}


	/**
	 * @param secToken
	 * @param printWriter
	 * @param cmd
	 *            TODO
	 * @return
	 */
    private String getData(String secToken,
                           PrintWriter printWriter, String cmd) {
        try {
            return iRubricService.getData(secToken);

        } catch (Exception ex) {
            LOG.error(ex);
            if (printWriter != null) {
                renderErrorMessageByCmd(
                                printWriter,
                                cmd,
                                "iRubric Server is not available or the response data is in invalid format.  Please let the system administrator know should the problem persist.");
            }
        }

        return null;

    }


    /**
	 * @param printWriter
	 * @param sGrade
	 * @param cmd
	 */
	private void processAllGrades(
			PrintWriter printWriter, String sGrade, String cmd, String gradebookUid, HttpServletRequest request, HttpServletResponse response) {
		if (sGrade != null) {
			switch (sGrade.charAt(0)) {
			case 'A':
				// All scores were transferred correctly
				String strScoreStream = sGrade.substring(1).trim();
				LOG.debug("score Stream: " + strScoreStream);

                String[] gradeItems = strScoreStream.split("\\|");
                response.setContentType("application/json");
                printWriter.print("{\"gradeData\":");
                printWriter.print("[");
                for (int i = 0; i < gradeItems.length; i++) {
                	String[] stGradeData = gradeItems[i].split(",");
                	printWriter.print("\n{\"id\": \""+ stGradeData[0] + "\",");
                	printWriter.print("\n\"score\": \""+ stGradeData[1] + "\"}");
                    if (i != gradeItems.length - 1) {
                    	printWriter.print(",");
                    }
                }
                printWriter.print("]}");
					
				break;

			case 'N':
				// No student has been graded yet.
				response.setContentType("application/json");
                printWriter.print("{\"message\": \""+ rl.getString("irubric.alertNoScores") + "\"}");
				break;
			case 'E':
				if (cmd.equals(CMD_GET_GRADES_BY_GRADEBOOK)) {
					response.setContentType("application/json");
					printWriter.print("{\"message\": \""+ rl.getString("irubric.noRubricAttached") + "\"}");
					break;
				}
			default:
				response.setContentType("application/json");
				printWriter.print("{\"message\": \""+ rl.getString("irubric.error") + "\"}");
				LOG.info("Returned value: " + sGrade);
				break;
			}
		}
	}

    /**
     * render a error alert when page loaded
     *
     * @param errorMsg an error message
     * @return a string
     */
    public String renderJSErrorBox(String errorMsg) {
        StringBuilder builder = new StringBuilder(
                "<html><body onload=\"alert('");
        builder.append(errorMsg);
        builder.append("');\"></body></html>");

        return builder.toString();
    }

    /**
   	 * render a error message when page loaded
   	 *
   	 * @param errorMsg
   	 *            an error message
   	 * @return a string
   	 */
   	private String renderErrorMessageBox(String errorMsg) {
   		StringBuilder builder = new StringBuilder(
   				"<br/><br/><div align=center>");
   		builder.append(errorMsg);
   		builder.append("</div>");

   		return builder.toString();
   	}

    /**
   	 * render error message by command
   	 *
   	 * @param printWriter
   	 * @param cmd
   	 * @param errorMsg
   	 */
   public void renderErrorMessageByCmd(PrintWriter printWriter, String cmd,
                                           String errorMsg) {

   		if (cmd.equals(IRubricService.CMD_GET_GRADES_BY_GDB)
   				|| cmd.equals(IRubricService.CMD_GET_GRADES_BY_ROS)) {
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

   	/**
   	 * dump error message by purpose
   	 *
   	 * @param writer
   	 * @param cmd
   	 *            the purpose is get from iRubricLink.jsp
   	 * @param errorCode
   	 */
   public void dumpErrorMessage(PrintWriter writer, String cmd,
                                    String errorCode, HttpServletResponse response) {
   		if (cmd.equals(IRubricService.CMD_GET_GRADES_BY_GDB)
   				|| cmd.equals(IRubricService.CMD_GET_GRADES_BY_ROS)) {
   			response.setContentType("application/json");
   			writer.print("{\"message\": \""+ rl.getFormattedMessage("irubric.errorCode", new Object[] {errorCode}) + "\"}");
   			
   		} else if (cmd.equals(CMD_GET_GRADE)) {
   			response.setContentType("application/json");
   			writer.print("{\"message\": \""+ rl.getFormattedMessage("irubric.errorCode", new Object[] {errorCode}) + "\"}");
   			
   		} else {
   			writer.print(ERR_MSG.replace("{errorcode}", errorCode));
   		}
   	}


    /**
     * @param cmd
     * @param gradebookUid
     * @param rosterStudentId
     * @return
     */
    private boolean isAuthorized(String cmd, String gradebookUid, String rosterStudentId) {
        if (gradebookUid != null) {
            if (cmd.equals(CMD_GET_ATTACHED_RUBRIC)) {
                return true;
            }

            if (cmd.equals(CMD_VIEW)) {
                if (gradebookService.currentUserHasGradingPerm(gradebookUid)) {
                    return true;
                }
                if (gradebookService.currentUserHasViewOwnGradesPerm(gradebookUid) && rosterStudentId != null
                        && rosterStudentId.equals(sessionManager.getCurrentSessionUserId())) {
                    return true;
                }
            } else if (cmd.equals(CMD_RUBRIC_AVAILALBE)) {
                if (gradebookService.currentUserHasGradingPerm(gradebookUid) ||
                		gradebookService.currentUserHasViewOwnGradesPerm(gradebookUid)) {
                    return true;
                }
            } else if (cmd.equals(CMD_ATTACH)) {
                if (gradebookService.currentUserHasEditPerm(gradebookUid)) {
                    return true;
                }
            } else if (cmd.equals(CMD_GET_GRADE) || cmd.equals(CMD_GRADE)
                    || cmd.equals(CMD_GET_GRADES_BY_ROS)
                    || cmd.equals(CMD_GET_GRADES_BY_GRADEBOOK) 
                    || cmd.equals(CMD_GRADE_ALL)
                    || cmd.equals(IRubricService.CMD_SUMMARY_REPORT)) {
                if (gradebookService.currentUserHasGradingPerm(gradebookUid)) {
                    return true;
                }
            } else if (CMD_VIEW_UNGRADED.equals(cmd)) {
            	if (gradebookService.currentUserHasGradingPerm(gradebookUid) ||
            			gradebookService.currentUserHasViewOwnGradesPerm(gradebookUid)) {
                    return true;
                }
            } else if(cmd.equals(IRubricService.CMD_GALLERY) || 
            		cmd.equals(IRubricService.CMD_BUILD) || 
            		cmd.equals(IRubricService.CMD_ASSESSMENT)) {

            	String userid = sessionManager.getCurrentSessionUserId();

            	//get site reference
            	String siteRef = siteService.siteReference(gradebookUid);
            	String lock = "";

            	if (cmd.equals(IRubricService.CMD_GALLERY)) {
            		lock = "irubric.accessgallery";

            	} else if(cmd.equals(IRubricService.CMD_BUILD)) {
            		lock = "irubric.build";

            	} else if(cmd.equals(IRubricService.CMD_ASSESSMENT)) {
            		lock = "irubric.collaborativeassessments";
            	}

            	return securityService.unlock(userid, lock, siteRef);
            } //TODO add special handing for these commands?
            else if (IRubricService.CMD_IRUBRIC_HOME.equals(cmd) ||
            		IRubricService.CMD_REPORT_RUBRIC.equals(cmd) ||
            		IRubricService.CMD_MY_RUBRIC.equals(cmd) ||
            		IRubricService.CMD_MY_EPORT.equals(cmd) ||
            		IRubricService.CMD_BUILD_EPORT.equals(cmd) ||
            		IRubricService.CMD_IRUBRIC_HOME.equals(cmd) ||
            		IRubricService.CMD_ASS_MATRIX.equals(cmd)) {
            	return true;
        	}
            
            LOG.error("Unknown command: " + cmd + " IRubric Authorization failed.");
            
            return false;
        }
        return false;
    }
}
