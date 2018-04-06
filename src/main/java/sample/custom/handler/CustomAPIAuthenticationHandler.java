/*
* Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package sample.custom.handler;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.Constants;
import org.apache.http.HttpStatus;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.rest.AbstractHandler;
import org.apache.synapse.transport.passthru.util.RelayUtils;
import org.wso2.carbon.apimgt.gateway.handlers.Utils;
import org.wso2.carbon.apimgt.gateway.handlers.security.APISecurityUtils;
import org.wso2.carbon.apimgt.gateway.handlers.security.AuthenticationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;


public class CustomAPIAuthenticationHandler extends AbstractHandler {

    private static Log log = LogFactory.getLog(CustomAPIAuthenticationHandler.class);


    public boolean handleRequest(MessageContext messageContext) {


        AuthenticationContext authContext = APISecurityUtils.getAuthenticationContext(messageContext);
        XACMLRequestAndResponseManager xacmlRequestAndResponseManger = new XACMLRequestAndResponseManager();
        String result = xacmlRequestAndResponseManger.isAllowedToInvoke(MultitenantUtils.getTenantAwareUsername(authContext.getUsername()),
                authContext.getApplicationName());
        if (authenticate(result,messageContext)) {
            return true;
        }
        return false;
    }

    public boolean handleResponse(MessageContext messageContext) {


        return true;
    }


    public boolean authenticate(String result,MessageContext messageContext) {

        if (result.equals("Permit")) {

            if (log.isDebugEnabled()) {
                log.debug("##########  Permit");
            }

            return true;
        }
        if (result.equals("NotApplicable")) {

            if (log.isDebugEnabled()) {
                log.debug("##########  NotApplicable");
            }

            return true;
        }
        if (result.equals("Deny")) {

            if (log.isDebugEnabled()) {
                log.debug("##########  Deny");
            }

            Exception ea = new Exception();
            messageContext.setFaultResponse(true);
            customHandleAuthFailure(messageContext, ea);
            return false;
        }
        if (result.equals("Indeterminate")) {

            if (log.isDebugEnabled()) {
                log.debug("##########  Indeterminate");
            }
            Exception ea = new Exception();
            messageContext.setFaultResponse(true);
            customHandleAuthFailure(messageContext, ea);
            return false;
        }
        return true;
    }
    private void customHandleAuthFailure(MessageContext messageContext,
                                         Exception e) {
        org.apache.axis2.context.MessageContext axis2MC = ((Axis2MessageContext) messageContext)
                .getAxis2MessageContext();
        axis2MC.setProperty(Constants.Configuration.MESSAGE_TYPE,
                "application/soap+xml");

        int status;
        status = HttpStatus.SC_UNAUTHORIZED;

        if (messageContext.isDoingPOX() || messageContext.isDoingGET()) {

            axis2MC.setProperty("ContentType", "application/json");
            // Set the payload to be sent back
            try {
                RelayUtils.buildMessage(axis2MC);
                Utils.setFaultPayload(messageContext, getFaultPayload(e));
            } catch (IOException e1) {
                e1.printStackTrace();

            } catch (XMLStreamException e1) {
                e1.printStackTrace();
            }

        }
        Utils.sendFault(messageContext, status);
    }

    private OMElement getFaultPayload(Exception e) {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace ns = fac.createOMNamespace(
                APISecurityConstants.API_SECURITY_NS,
                APISecurityConstants.API_SECURITY_NS_PREFIX);
        OMElement payload = fac.createOMElement("fault", ns);

        OMElement errorCode = fac.createOMElement("code", ns);
        errorCode.setText(String.valueOf(401));
        OMElement errorMessage = fac.createOMElement("message", ns);
        errorMessage.setText("Not Authorized");
        OMElement errorDetail = fac.createOMElement("description", ns);
        errorDetail
                .setText("The user's role does not allow access to the requested service");

        payload.addChild(errorCode);
        payload.addChild(errorMessage);
        payload.addChild(errorDetail);
        return payload;
    }

}
