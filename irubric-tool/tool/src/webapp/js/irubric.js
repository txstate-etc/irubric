//DN 2012-09-24: open window irubric
function openWindowiRubric(purpose) {
    var heightResize = 400;
    
    //irubric link
    var urlPage = createLink(null);
    urlPage = urlPage + "&p=" + purpose;

     //decrease iframe when height iframe greater than 600
    if(document.body.clientHeight > heightResize) {
        resizeFrame(400);
        //clear iframe
        cleariFrame();
    }
    openWindow(urlPage);

}

function openWindow(urlPage){
    window.open(urlPage,'_blank',
     'width=800,height=800,top=20,left=100,menubar=yes,status=yes,location=no,toolbar=yes,scrollbars=yes,resizable=yes');
}

//resize frame
function resizeFrame (height){
    var clientH;
    if (top.location !== self.location) {
        var frame = parent.document.getElementById(window.name);
    }
    if (frame) {
       
        if(height == null) {
            //set default frame
            clientH = 300;
        }else {
            clientH = 650;

        }
        $(frame).height(clientH);
    }
    
}


//open iframe
function openiFrame(urlPage){
    
    var iframe = document.getElementById('iframeiRubric');
    showElement(iframe);
    iframe.src = urlPage;

}

function cleariFrame() {
    var iframe = document.getElementById('iframeiRubric');
    if(iframe != null) {
        //clear iframe
        iframe.src = '';
        //hide iframe
        hideElement(iframe);
    }
}
//hide iframe
function hideElement (element) {
    if (element.style) {
        element.style.display = 'none';
    }
}

//show iframe
function showElement (element) {
    if (element.style) {
        element.style.display = '';
    }
}


//use for template grade all
function openSiteiRubric(gradebookItemId, purpose){
    //irubric link
    var urlPage = createLink(gradebookItemId);
    //grade all
    if(purpose == 'gradeall') //grade all
        urlPage = urlPage + "&p=ga&t=gb2";
    
    else if(purpose == 'sumreport') //summary report
        urlPage = urlPage + "&p=srpt";

    else if(purpose == 'viewgrade'){  //view grade
        var studentId = $('#studentId').val();
        urlPage = urlPage + "&p=v&rosterStudentId="+ studentId;
    }
    else //attach irubric(select iRubric)
        urlPage = urlPage + "&p=a";
    //open window
    openWindow(urlPage);
}

//funtion get grade
function getGrades(gradebookItemId){
    //irubric link
    var urlPage = createLink(gradebookItemId);

    //add purpose
    urlPage = urlPage + "&p=gag&t=gb2";
    
    $.post(urlPage,'',
        function(data){
            
            if(data != null) {

                switch (data){
                    case 'A':
                        alert('Grades have been refreshed from iRubric.');
                        break;

                    case 'N':
                        alert('No student have been graded in iRubric.');
                        break;

                    case 'E':
                        alert('A rubric is not attached. Please select an iRubric first.');
                        break;

                    case 'I':
                        alert('Invalid returned value from iRubric. Please notify the system administrator, should the problem persist.');
                        break;

                    default:
                        alert('Could not receive data from iRubric.');

                }
            }else {
                console.log("Data response is null");
            }

        }, "text/string");
}

//create irubric link
function createLink(gradebookItemId) {
    var gradebookUId = $('#gradebookUId').val();
    
    var urlPage = "/irubric-tool/IRubricServlet?gradebookUid=" + gradebookUId + "&siteId=" + gradebookUId;

    if(gradebookItemId != null) 
        urlPage = urlPage + "&gradebookItemId=" + gradebookItemId;

    return urlPage;
}
