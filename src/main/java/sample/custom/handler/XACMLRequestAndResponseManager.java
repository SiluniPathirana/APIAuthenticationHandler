/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
 *
 * This software is the property of WSO2 Inc. and its suppliers, if any.
 * Dissemination of any information or reproduction of any material contained
 * herein is strictly forbidden, unless permitted by WSO2 in accordance with
 * the WSO2 Commercial License available at http://wso2.com/licenses. For specific
 * language governing the permissions and limitations under this license,
 * please see the license as well as any agreement you’ve entered into with
 * WSO2 governing the purchase of this software and any associated services.
 */

package sample.custom.handler;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.wso2.balana.utils.exception.PolicyBuilderException;
import org.wso2.balana.utils.policy.PolicyBuilder;
import org.wso2.balana.utils.policy.dto.RequestElementDTO;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.entitlement.EntitlementException;
import org.wso2.carbon.identity.entitlement.EntitlementService;
import org.wso2.carbon.identity.entitlement.common.dto.RequestDTO;
import org.wso2.carbon.identity.entitlement.common.dto.RowDTO;
import org.wso2.carbon.identity.entitlement.common.util.PolicyCreatorUtil;
import org.wso2.carbon.identity.entitlement.pdp.EntitlementEngine;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;


public class XACMLRequestAndResponseManager {

    private static final Log log = LogFactory.getLog(XACMLRequestAndResponseManager.class);


    public String isAllowedToInvoke(String subscriber, String applicationName) {

        if (log.isDebugEnabled()) {
            log.debug("---------In policy provisioning flow...-----------------");
        }
        String decision = "";
        try {
            RequestDTO requestDTO = createRequestDTO(subscriber, applicationName);
            RequestElementDTO requestElementDTO = PolicyCreatorUtil.createRequestElementDTO(requestDTO);

            String requestString = PolicyBuilder.getInstance().buildRequest(requestElementDTO);
            log.debug("---------------------XACML request ------------------------------:\n" + requestString);

            log.debug("Tenant Id " + CarbonContext.getThreadLocalCarbonContext().getTenantId());


            String responseString = EntitlementEngine.getInstance().evaluate(requestString);
            if (log.isDebugEnabled()) {
                log.debug("XACML response :\n" + responseString);
            }
            decision = evaluateXACMLResponse(responseString);

        } catch (PolicyBuilderException e) {
            log.error("Policy Builder Exception occurred", e);
        } catch (EntitlementException e) {
            log.error("Entitlement Exception occurred", e);
        } catch (Exception e) {
            log.error("Evaluate Request Exception occured", e);
        }
        return decision;

    }


    private RequestDTO createRequestDTO(String subscriber, String applicationName) {
        List<RowDTO> rowDTOs = new ArrayList();


        RowDTO userDTO = createRowDTO(subscriber, APISecurityConstants.USER_ATTRIBUTE_DATATYPE,
                APISecurityConstants.USER_ATTRIBUTE_ID, APISecurityConstants.USER_ATTRIBUTE_CATEGORY);

        RowDTO requestApplicationNameDTO = createRowDTO(applicationName, APISecurityConstants.APPLICATION_ATTRIBUTE_DATATYPE,
                APISecurityConstants.APPLICATION_ATTRIBUTE_ID, APISecurityConstants.APPLICATION_ATTRIBUTE_CATEGORY);


        rowDTOs.add(userDTO);
        rowDTOs.add(requestApplicationNameDTO);


        RequestDTO requestDTO = new RequestDTO();
        requestDTO.setRowDTOs(rowDTOs);
        return requestDTO;
    }

    private RowDTO createRowDTO(String resourceName, String dataType, String attributeId, String categoryValue) {

        RowDTO rowDTOTenant = new RowDTO();
        rowDTOTenant.setAttributeValue(resourceName);
        rowDTOTenant.setAttributeDataType(dataType);
        rowDTOTenant.setAttributeId(attributeId);
        rowDTOTenant.setCategory(categoryValue);
        return rowDTOTenant;
    }


    private String evaluateXACMLResponse(String xacmlResponse) throws Exception {
        String decision = "";
        try {
            DocumentBuilderFactory documentBuilderFactory = IdentityUtil.getSecuredDocumentBuilderFactory();
            DocumentBuilder db = documentBuilderFactory.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xacmlResponse));
            Document doc = db.parse(is);


            NodeList decisionNode = doc.getDocumentElement().getElementsByTagName(
                    APISecurityConstants.XACML_RESPONSE_DECISION_NODE);
            if (decisionNode != null && decisionNode.item(0) != null) {
                decision = decisionNode.item(0).getTextContent();

            }

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        return decision;
    }
}
