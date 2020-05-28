package com.intuit.platform.services.ebpi.account.apis.acct.service;

import com.intuit.platform.schema.mdm.facade.organizationservice.siebel.xml.swiorganizationio.Account;
import com.intuit.platform.schema.mdm.synccustomer.account.DeleteContactInputDataType;
import com.intuit.platform.schema.mdm.synccustomer.account.DeleteContactInputType;
import com.intuit.platform.schema.mdm.synccustomer.synccustomeraccountservice.CustomerAccountException;
import com.intuit.platform.services.cas.account.sdk.service.CasContactService;
import com.intuit.platform.services.cas.account.v1.AuthorizeAccountResponse;
import com.intuit.platform.services.cas.account.v1.CanRealmMap;
import com.intuit.platform.services.cas.account.v1.CanRealmMaps;
import com.intuit.platform.services.common.constants.CommonConstants;
import com.intuit.platform.services.common.exceptions.rs.BadRequestException;
import com.intuit.platform.services.common.exceptions.rs.InternalServerException;
import com.intuit.platform.services.common.exceptions.rs.UnauthorizedException;
import com.intuit.platform.services.common.exceptions.rs.ServiceUnavailableException;
import com.intuit.platform.services.common.util.auth.AuthUtil;
import com.intuit.platform.services.common.util.mapper.Mapper;
import com.intuit.platform.services.ebpi.account.apis.acct.dto.GetV2AccountContext;
import com.intuit.platform.services.ebpi.account.apis.acct.exception.AccountsExceptionHandler;
import com.intuit.platform.services.ebpi.account.apis.acct.integration.*;
import com.intuit.platform.services.ebpi.account.apis.acct.util.AccountsUtil;
import com.intuit.platform.services.ebpi.account.apis.common.constants.AccountApiConstants;
import com.intuit.platform.services.ebpi.account.v2.AccountsResponse;
import com.intuit.platform.services.ebpi.account.v2.Contact;
import com.intuit.platform.services.ebpi.account.v2.RelatedAccount;
import com.intuit.platform.services.common.util.header.Headers;
import com.intuit.platform.services.ebpi.constants.EbpiCommonConstants;
import com.intuit.platform.services.ebpi.constants.EbpiError;
import com.intuit.platform.services.ebpi.constants.EbpiErrorDetails;
import com.intuit.platform.services.ebpi.entitlement.v1.EntitlementSummaryResponse;
import com.intuit.platform.services.ebpi.entitlement.v1.ItemSummary;
import com.intuit.platform.services.ebpi.util.v1.Application;
import com.intuit.platform.services.mdm.sdk.service.AccountMdmSdkService;
import com.intuit.platform.services.siebel.sdk.accountvalidation.ContactDeleteValidationInput;
import com.intuit.platform.services.siebel.sdk.accountvalidation.ContactDeleteValidationOutput;
import com.intuit.platform.services.siebel.sdk.service.SiebelAccountSdkService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class V2AccountsService {

    @Autowired
    private MDMIntegrationService getAccountByCustomerAccountNumber;

    @Autowired
    @Qualifier("acctSecurityIntegrationService")
    private SecurityIntegrationService securityIntegrationService;

    @Autowired
    private EntitledOfferingIntegrationService entitledOfferingIntegrationService;

    @Autowired
    private CasContactService casContactService;

    @Autowired
    private CasAccountIntegrationService casAccountIntegrationService;

    @Autowired
    private CasCanRealmMapIntegrationService casCanRealmMapIntegrationService;

    @Autowired
    private Mapper<Account, AccountsResponse> accountsResponseMapper;

    @Autowired
    private UtilApiIntegrationService utilApiIntegrationService;

    @Autowired
    private Mapper<CanRealmMap, RelatedAccount> canRealmMapRelatedAccountMapper;

    @Autowired
    private AccountsUtil accountsUtil;

    @Autowired
    private SiebelAccountSdkService siebelAccountSdkService;

    @Autowired
    private AccountMdmSdkService accountMdmSdkService;

    public AccountsResponse getAccountById(String accountId, String scheme, GetV2AccountContext context) {
        log.error("in get account by id, scheme={}", scheme);

        // the default flow is MDM if scheme is missing
        if (scheme == null || AccountApiConstants.MDM_SCHEME.equals(scheme)) {
            log.error("Processing the MDM flow");
            validateForSchemeCAN(accountId, context);

            Account accountByCustomerAccountNumber = getAccountByCustomerAccountNumber.getAccountByCustomerAccountNumber(accountId, context.getHeader());
            log.info("Got account from mdm for accountId={}", accountId);
            AccountsResponse accountsResponse = accountsResponseMapper.map(accountByCustomerAccountNumber);
            log.info("Converted MDM response to api response for accountId={}", accountId);

            CanRealmMaps canRealmMap = casCanRealmMapIntegrationService.getCanRealmMap(accountId, context);
            if (canRealmMap != null && !CollectionUtils.isEmpty(canRealmMap.getCanRealmMaps())) {
                List<CanRealmMap> canRealmMaps = canRealmMap.getCanRealmMaps();
                accountsResponse.setRelatedAccounts(canRealmMaps.stream().map(canRealmMapRelatedAccountMapper::map).collect(Collectors.toList()));
            }

            if (!context.getSecurityResponse().isUserContext()) {
                updateInfoForNonUserContext(accountsResponse);
            }
            return accountsResponse;
        } else {
            log.error("Invalid Scheme in V2 ACCOUNT API");
            List<Pair<String, String>> errorDetails = new ArrayList<>();
            errorDetails.add(Pair.of("Invalid SCHEME", null));
            throw new BadRequestException(EbpiError.EBPI_400_ACCT_002.getCode(), errorDetails);
        }

    }

    private void updateInfoForNonUserContext(AccountsResponse accountsResponse) {
        // filter information for third party access, we want to limit the PII given out by removing addresses or secondary contacts
        accountsResponse.setAddresses(null);
        List<Contact> primaryContacts = accountsResponse.getContacts().stream().filter(contact -> contact.getIsPrimary()).collect(Collectors.toList());
        accountsResponse.setContacts(primaryContacts);
    }

    private void validateForSchemeCAN(String accountId, GetV2AccountContext context) {
        log.info("Validating request in MDM flow for accountId={} isUserContext={} isCSRUser={} isSystemUser={}", accountId,
                context.getSecurityResponse().isUserContext(),
                context.getSecurityResponse().isCSRUser(),
                context.getSecurityResponse().isSystemUser());
        if (context.getSecurityResponse().isUserContext()
                && !context.getSecurityResponse().isCSRUser()
                && !context.getSecurityResponse().isSystemUser()) {
            log.info("Request is user context, checking if the user is authorized to access accountId={} userId={}", accountId, context.getSecurityResponse().getUserID());
            context.getHeader().setIntuitUserID(
                    AuthUtil.fetchValueFromAuth(context.getHeader().getAuthorization(), CommonConstants.INTUIT_USER_ID_KEY));
            AuthorizeAccountResponse authorizeAccountResponse =
                    casAccountIntegrationService.casAuthorizationByAccountNumber(accountId, context.getHeader());
            log.info("CAS authorizeAccount() response = " + authorizeAccountResponse);
            if (null == authorizeAccountResponse || null == authorizeAccountResponse.getStatus() || !authorizeAccountResponse.getStatus()
                    .equalsIgnoreCase(AccountApiConstants.CAS_AUTH_OK)) {
                throw new UnauthorizedException(EbpiError.EBPI_403_ACCT_001.getCode(),
                        EbpiErrorDetails.AUTHENTICATION_ERROR_DETAILS.getErrorDetails());
            }
        } else if (!context.getSecurityResponse().isUserContext()) {
            log.info("Request is not user context, checking if the app is authorized to access accountId={} userId={}", accountId, context.getSecurityResponse().getUserID());
            verifyIfAppHasRequiredPermission(accountId, context);
        }

    }

    private void verifyIfAppHasRequiredPermission(String accountId, GetV2AccountContext context) {
        String offlineTicket = securityIntegrationService.getOfflineTicketHeaderForSystemUser(context.getHeader().getIntuitTID());

        EntitlementSummaryResponse summary = entitledOfferingIntegrationService
                .fetchSummary(accountId, context.getHeader(), offlineTicket);

        if (summary != null && !CollectionUtils.isEmpty(summary.getItemSummary())) {
            log.info("Got product line summary, processing the response for accountId={}", accountId);
            List<String> itemIds = summary.getItemSummary().stream().map(ItemSummary::getItemNumber).collect(Collectors.toList());
            log.info("Got item ideas for accountId={} itemIds={}", accountId, itemIds);

            List<Application> applications = utilApiIntegrationService.getApplications(itemIds, context);

            if (CollectionUtils.isEmpty(applications)) {
                log.info("Applications API did not return any applications");
                unauthorised(accountId);
            } else {
                log.info("Received applications from api count={}", applications.size());
                log.info("Application intuit_assetalias={} is authorised to access CAN details", context.getSecurityResponse().getAssetAlias());
            }
        } else {
            unauthorised(accountId);
        }

    }

    private void unauthorised(String accountId) {
        log.error("Application is not authorized to fetch the v2 account information for the accountId={}", accountId);
        List<Pair<String, String>> errorDetails = new ArrayList<>(1);
        errorDetails.add(Pair.of("Application is not authorized to access the account details", null));
        throw new UnauthorizedException(EbpiError.EBPI_403_ACCT_001.getCode(), errorDetails);
    }

    public void deleteContact(String accountId, String contactId, Headers headers)
    {
        String authorization = headers.getAuthorization();
        String intuitRealmId = AuthUtil.fetchValueFromAuth(authorization, CommonConstants.INTUIT_REALM_ID_KEY);
        log.info("delete Contact service started for accountId={} contactId={} " , accountId,  contactId );
        if (intuitRealmId != null && intuitRealmId.equalsIgnoreCase(AccountApiConstants.IS_AGENT)) {
            log.info("deleteContact Agent flow");
            ContactDeleteValidationOutput contactDeleteValidationOutput = accountsUtil.initDeleteContactValidation(accountId, contactId, headers);
            String okToDelete = contactDeleteValidationOutput.getOkToDelete();
            String noMatchId = contactDeleteValidationOutput.getNoMatchID();
            String errorSpcMessage = contactDeleteValidationOutput.getErrorSpcMessage();
            log.info("okToDelete={} noMatchId={} errorSpcMessage={}" + okToDelete , noMatchId , errorSpcMessage);

            if (StringUtils.isNotEmpty(okToDelete) && okToDelete.equalsIgnoreCase(AccountApiConstants.OK_TO_DELETE)) {
                DeleteContactInputType deleteContactInputType = accountsUtil.initializeDeleteContact(accountId, contactId);
                List<Pair<String, String>> errorDetails = new ArrayList<>();
                if(deleteContactInputType == null) {
                    errorDetails.add(Pair.of(AccountApiConstants.ERROR_MDM_DATA_VALIDATION, null));
                    throw new BadRequestException(EbpiError.ACCT_401100.getCode(), errorDetails);
                }
                try {
                    log.info("deleteContact Call mdm sdk deleteContact");
                    accountMdmSdkService.deleteContact(deleteContactInputType, headers);
                } catch (CustomerAccountException e) {
                    log.error("CustomerAccountException during deleteContact at MDM, {}");
                    errorDetails.add(Pair.of(AccountApiConstants.ERROR_MDM_DATA_VALIDATION, null));
                    throw new BadRequestException(EbpiError.ACCT_401100.getCode(), errorDetails);
                } catch (ServiceUnavailableException e) {
                    log.error("ServiceUnavailableException during deleteContact at MDM, {}");
                    errorDetails.add(Pair.of(AccountApiConstants.ERROR_MDM_INVOCATION, null));
                    throw new BadRequestException(EbpiError.ACCT_401101.getCode(), errorDetails);
                } catch (Exception e) {
                    log.error("Exception during deleteContact at MDM, {}");
                    errorDetails.add(Pair.of(EbpiCommonConstants.INTERNAL_SERVER_ERROR, null));
                    throw new InternalServerException(EbpiError.ACCT_201040.getCode(), errorDetails);
                }
            } else if (StringUtils.isNotEmpty(okToDelete) && !(okToDelete.equalsIgnoreCase(
                    AccountApiConstants.OK_TO_DELETE)))
            {
                log.error("Exception during Seibel deleteContact Validation NotOkToDelete, {}");
                List<Pair<String, String>> errorDetails = new ArrayList<>();
                errorDetails.add(Pair.of(EbpiCommonConstants.RECORD_NOT_OK_TO_DELETE, null));
                throw new BadRequestException(EbpiError.ACCT_401120.getCode(), errorDetails);
            } else if (StringUtils.isNotEmpty(noMatchId) && noMatchId.equalsIgnoreCase(
                    AccountApiConstants.NO_MATCH_ID))
            {
                List<Pair<String, String>> errorDetails = new ArrayList<>();
                errorDetails.add(Pair.of(EbpiCommonConstants.RECORD_NOT_FOUND_IN_SIEBEL, null));
                throw new BadRequestException(EbpiError.ACCT_401122.getCode(), errorDetails);
            } else if (StringUtils.isNotEmpty(errorSpcMessage)) {
                log.error("Exception during deleteContact Siebel Validation and errorSpcMessage is not empty, {}");
                List<Pair<String, String>> errorDetails = new ArrayList<>();
                errorDetails.add(Pair.of(EbpiCommonConstants.SIEBEL_VALIDATION_FAILURE, null));
                throw new BadRequestException(EbpiError.ACCT_401123.getCode(), errorDetails);
            }
        } else {
            try {
                log.info("deleteContact Non agent, ebpi pass through flow");
                casContactService.deleteContact(accountId, contactId, headers);
            } catch (Exception e) {
                log.error("Error occurred  while calling deleteContact EbpiPassThrough Flow Error={}", e);
                AccountsExceptionHandler.handleException(e);
            }
        }
    }

}
