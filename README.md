TPP Signup Workflow - Read Me

Introduction
- This module will introduce customizations of TPP Signup Workflow.
- It includes email notification sending, Open Banking role assignment, and including signup information into description on BPS process instance.



Configurations
1. Set the email server configurations in the <API-M_HOME>/repository/conf/output-event-adapters.xml file under the <adapterConfig type="email"> section.

<adapterConfig type="email">
       <!-- Comment mail.smtp.user and mail.smtp.password properties to support connecting SMTP servers which use trust
       based authentication rather username/password authentication -->
       <property key="mail.smtp.from">abcd@gmail.com</property>
       <property key="mail.smtp.user">abcd</property>
       <property key="mail.smtp.password">xxxx</property>
       <property key="mail.smtp.host">smtp.gmail.com</property>
       <property key="mail.smtp.port">587</property>
       <property key="mail.smtp.starttls.enable">true</property>
       <property key="mail.smtp.auth">true</property>
       <!-- Thread Pool Related Properties -->
       <property key="minThread">8</property>
       <property key="maxThread">100</property>
       <property key="keepAliveTimeInMillis">20000</property>
       <property key="jobQueueSize">10000</property>
   </adapterConfig>


    mail.smtp.from - The email address you use to send emails
    mail.smtp.user - The email address used to authenticate the mail server. This can be same as mail.smtp.from
    mail.smtp.password - Password used to authenticate the mail server.

    NOTE : If you are using gmail as SMTP server, you may need to import gmail public certificate in to Trust store of APIM.
        wget https://secure.globalsign.net/cacert/Root-R2.crt
        keytool -import -trustcacerts -alias globalsign -file Root-R2.crt -keystore client-truststore.jks


2. In APIM, update following configurations on /_system/config/apimgt/applicationdata/tenant-conf.json

  "Notifications":[
  {
    "Type":"new_signup",
    "Notifiers" :
    [{
      "Class":"SignUpWorkflowEmailNotifier",
      "ClaimsRetrieverImplClass":"org.wso2.carbon.apimgt.impl.token.DefaultClaimsRetriever",
      "Title": "New Signup Request from $2",
      "ApproverRole": "admin",
      "Template": " <html> <body> <h3 style=\"color:Black;\">New TPP registration request received from $1userName, behalf of $10legalEntityName .</h3><h3/><h3 style=\"color:Black;\">User details as follows.</h3><h4 style=\"color:Black;\">Username : $1userName</h4><h4 style=\"color:Black;\">First Name : $7firstName</h4><h4 style=\"color:Black;\">Last Name : $8lastName</h4><h4 style=\"color:Black;\">Email Address : $9email</h4><h4 style=\"color:Black;\">Requested Open Banking Roles : $6openBankingRoles</h4><h3/><h3 style=\"color:Black;\">Company Registration details as follows.</h3><h4 style=\"color:Black;\">Legal Entity Name : $10legalEntityName</h4><h4 style=\"color:Black;\">Country Of Registration : $11countryOfRegistration</h4><h4 style=\"color:Black;\">Company Register : $12companyRegister</h4><h4 style=\"color:Black;\">Company Registration Number : $13companyRegistrationNumber</h4><h3/><h3 style=\"color:Black;\">Competent Authority Registration details as follows.</h3><h4 style=\"color:Black;\">Competent Authority : $3competentAuthority</h4><h4 style=\"color:Black;\">Competent Authority Country : $2competentAuthorityCountry</h4><h4 style=\"color:Black;\">Competent Authority Registration Number : $4competentAuthorityRegNumber</h4><h4 style=\"color:Black;\">Competent Authority Register Page URL : $5competentAuthorityURL</h4><a href=\"https://localhost:9443/admin\">Click here to login WSO2 APIM Admin Dashboard</a><h6 style=\"color:Black;\">(Please do not respond to this automated e-mail)</h6></body></html>",
      "AckTitle": "We received your signup request - $1",
      "AckTemplate": " <html> <body> <h3 style=\"color:Black;\">We received your ($1) signup request, and we are processing it. You will get a notification email when the decision is made.</h3><h6 style=\"color:Black;\">(Please do not respond to this automated e-mail)</h6></body></html>"
    }]
  },
  {
    "Type":"new_signup_complete",
    "Notifiers" :
    [{
      "Class":"SignUpWorkflowCompleteEmailNotifier",
      "ClaimsRetrieverImplClass":"org.wso2.carbon.apimgt.impl.token.DefaultClaimsRetriever",
      "Title": "Signup Request from $2 has been $1",
      "Template": " <html> <body> <h3 style=\"color:Black;\">Your signup request has been $1.</h3><h3 style=\"color:Black;\">Details :  $3.</h3><a href=\"https://localhost:9443/store\">Click here to navigate to WSO2 API Store</a><h6 style=\"color:Black;\">(Please do not respond to this automated e-mail)</h6></body></html>"
     }]
  }
  ],


    ApproverRole - Role which get Signup Request Nortifications
    Title - Email Subject
    Template - Email Content


3. Configure User Signup Workflow as in https://docs.wso2.com/display/AM210/Adding+a+User+Signup+Workflow

4. In APIM, edit the  /_system/governance/apimgt/applicationdata/workflow-extensions.xml resource, update UserSignUp section,  set executor="TPPSignUpWorkFlow".
    <UserSignUp executor="TPPSignUpWorkFlow">
         <Property name="serviceEndpoint">http://localhost:9765/services/UserSignupProcess/</Property>
         <Property name="username">admin@wso2ob.com@carbon.super</Property>
         <Property name="password">wso2ob123</Property>
         <Property name="callbackURL">https://localhost:8243/services/WorkflowCallbackService</Property>
    </UserSignUp>

NOTE : update the 'serviceEndpoint' as per BPS hostname and port. update the 'callbackURL' as per APIM host name and port.

