<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ page language="java" isELIgnored="false" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>	

<%@ page import="edu.wustl.catissuecore.actionForm.InstitutionForm"%>

<link rel="stylesheet" type="text/css" href="css/catissue_suite.css" />	
<script src="jss/ajax.js" type="text/javascript"></script>
<script type="text/javascript">
function addCRG()
{
    var name = document.getElementById("name").value;
    var request = newXMLHTTPReq();
	if(request == null)
    {
		alert ("Your browser does not support AJAX!");
		return;
	}
	var handlerFunction = getReadyStateHandler(request,setCRGValues,true);
    request.onreadystatechange = handlerFunction;
	var param = "crgName="+name;
    var url = "AddCancerResearchGroup.do";
 	request.open("POST",url,true);
	
	request.setRequestHeader("Content-Type","application/x-www-form-urlencoded");	
	request.send(param);
}

function setCRGValues(response)
{
   var values = response.split("#@#");
   if(values.length == 1)
   {
        var divTag = document.getElementById("errorDiv");
		divTag.innerHTML = "<font size='3' color='red'>"+values[0]  +"</font>"; 
  }
  else
  {
	    //Setting ID
	    crgCtrl = window.parent.document.getElementById("cancerResearchGroupId");
		crgCtrl.value = values[0];
	
		//Setting the Cancer Research Group Name    
		displayCRGCtrl = window.parent.document.getElementById("displaycancerResearchGroupId");    
		displayCRGCtrl.value =values[1];

		//Closing the modal window
		parent.crgWindow.hide();
  }
 
}

function cancelWindow()
{
  parent.crgWindow.hide();
}

</script>

<!--begin content -->
<table width="100%" border="0" cellpadding="1" cellspacing="0" bgcolor="#FFFFFF">
		<tr>
          <td> <div id="errorDiv"> </div></td>
	    </tr>
		
		<tr>
    <td colspan="3" align="left"><table width="99%" border="0" cellpadding="1" cellspacing="0">
      <tr>
        <td><table width="100%" border="0" cellpadding="2" cellspacing="2" class="td_color_ffffff">
		<html:errors/> 
<html:messages id="messageKey" message="true" header="messages.header" footer="messages.footer">
	<%=messageKey%>
</html:messages>
   
	<html:form action='/CancerResearchGroupAdd.do'>  
	 <tr>
			<td>
				<html:hidden property="operation" />
				<html:hidden property="submittedFor"/>
			</td>
		</tr>
		
		<tr>
			<td><html:hidden property="id" /></td>
		</tr>
          <tr>
            <td class=" grey_ar_s">&nbsp;<img src="images/uIEnhancementImages/star.gif" alt="Mandatory" width="6" height="6" hspace="0"		vspace="0" />&nbsp; <bean:message key="commonRequiredField.message" />
			</td>

          </tr>
        </table></td>
      </tr>
    </table></td>
  </tr>
  <tr>
    <td height="25" align="left" class="tr_bg_blue1"><span class="blue_ar_b">&nbsp;					<bean:message key="user.cancerResearchGroup"/>
				<!--<logic:equal name="operation" value='${requestScope.operationAdd}'>
					<bean:message key="user.cancerResearchGroup"/>
				</logic:equal>
				<logic:equal name="operation" value='${requestScope.operationEdit}'>
					<bean:message key="institution.editTitle"/>
				</logic:equal> -->
			</span></td>
    <td align="right" class="tr_bg_blue1">&nbsp;</td>

  </tr>
  <tr>
    <td colspan="3" align="left"><div id="part_det" >
      <table width="100%" border="0" cellpadding="5" cellspacing="2">
                 
          <tr>
            <td width="2%" align="right" class="black_ar"><span class="blue_ar_b"><img src="images/uIEnhancementImages/star.gif" alt="Mandatory" width="6" height="6" hspace="0" vspace="0" /></span></td>
            <td width="14%" align="left" class="black_ar">
				<label for="name">
					<bean:message key="institution.name"/>
				</label>
			</td>

            <td width="80%" align="left"><label>
				<html:text styleClass="black_ar" maxlength="255"  size="50" styleId="name" property="name"/>
            </label></td>
            <td width="4%" align="left">&nbsp;</td>
          </tr>
       
      </table>
    </div></td>
  </tr>

  <tr>
    <td colspan="3">&nbsp;</td>
  </tr>
  <tr class="td_color_F7F7F7">
    <td height="35" colspan="3" class="buttonbg">&nbsp;
		<!--<html:submit styleClass="blue_ar_b">
			<bean:message  key="buttons.submit" />
		</html:submit> -->

      <html:button property="clickButton" styleClass="blue_ar_b" onclick="addCRG()" >
	      <bean:message  key="buttons.submit" /> 
      </html:button>
  
     
      &nbsp;| <span class="cancellink"><html:link href="#" styleClass="blue_ar_s_b" onclick= "cancelWindow();">
													<bean:message key="buttons.cancel" />
												</html:link></span></td>
  </tr>
 </html:form>

</table>
<!--end content -->
