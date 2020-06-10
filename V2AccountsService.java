package com.intuit.platform.services.ebpi.account.apis.acct.service;

import com.intuit.platform.schema.mdm.synccustomer.account.CustomerAccountHeaderType;
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
import com.intuit.platform.services.common.exceptions.rs.ServiceUnavailableException;
import com.intuit.platform.services.common.exceptions.rs.UnauthorizedException;
import com.intuit.platform.services.common.security.bean.SecurityResponse;
import com.intuit.platform.services.common.util.auth.AuthUtil;
import com.intuit.platform.services.common.util.header.Headers;
import com.intuit.platform.services.common.util.mapper.Mapper;
import com.intuit.platform.services.ebpi.account.apis.acct.dto.GetV2AccountContext;
import com.intuit.platform.services.ebpi.account.apis.acct.exception.AccountsExceptionHandler;
import com.intuit.platform.services.ebpi.account.apis.acct.integration.*;
import com.intuit.platform.services.ebpi.account.apis.acct.util.AccountsUtil;
import com.intuit.platform.services.ebpi.account.apis.common.constants.AccountApiConstants;
import com.intuit.platform.services.ebpi.common.v2.Account;
import com.intuit.platform.services.ebpi.common.v2.Contact;
import com.intuit.platform.services.ebpi.common.v2.RelatedAccount;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class V2AccountsService {

    @Context
    private HttpServletRequest request;

    @Autowired
    private MDMIntegrationService getAccountByCustomerAccountNumber;

    @Autowired
    private MDMIntegrationService mdmIntegrationService;

    @Autowired
    @Qualifier("acctSecurityIntegrationService")
    private SecurityIntegrationService securityIntegrationService;

    @Autowired
    private EntitledOfferingIntegrationService entitledOfferingIntegrationService;

    @Autowired
    private CasAccountIntegrationService casAccountIntegrationService;

    @Autowired
    private CasCanRealmMapIntegrationService casCanRealmMapIntegrationService;

    @Autowired
    private AccountsUtil accountsUtil;

    @Autowired
    private SiebelAccountSdkService siebelAccountSdkService;

    @Autowired
    private AccountMdmSdkService accountMdmSdkService;

    @Autowired
    private CasContactService casContactService;

    @Autowired
    private Mapper<com.intuit.platform.schema.mdm.facade.organizationservice.siebel.xml.swiorganizationio.Account, Account> accountsResponseMapper;

    @Autowired
    private UtilApiIntegrationService utilApiIntegrationService;

    @Autowired
    private Mapper<CanRealmMap, RelatedAccount> canRealmMapRelatedAccountMapper;

    public Account getAccountById(String accountId, String scheme, GetV2AccountContext context) {
        log.error("in get account by id, scheme={}", scheme);

        // the default flow is MDM if scheme is missing
        if (scheme == null || AccountApiConstants.MDM_SCHEME.equals(scheme)) {
            log.error("Processing the MDM flow");
            validateForSchemeCAN(accountId, context);

            com.intuit.platform.schema.mdm.facade.organizationservice.siebel.xml.swiorganizationio.Account accountByCustomerAccountNumber = getAccountByCustomerAccountNumber.getAccountByCustomerAccountNumber(accountId, context.getHeader());
            log.info("Got account from mdm for accountId={}", accountId);
            Account accountsResponse = accountsResponseMapper.map(accountByCustomerAccountNumber);
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

    private void updateInfoForNonUserContext(Account accountsResponse) {
        // filter information for third party access, we want to limit the PII given out by removing addresses or secondary contacts
        accountsResponse.setAddresses(null);
        List<Contact> primaryContacts = accountsResponse.getContacts().stream().filter(contact -> contact.getIsPrimary()!=null&&contact.getIsPrimary()).collect(Collectors.toList());
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
        String offlineTicket = securityIntegrationService.getOfflineTicketHeaderForSystemUser(accountId, context);

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


    private void handleBadRequestException(String errorMsg, String detail, String code) {
        log.info(errorMsg);
        List<Pair<String, String>> errorDetails = new ArrayList<>();
        errorDetails.add(Pair.of(detail, null));
        throw new BadRequestException(code, errorDetails);
    }

    private void validateAccountForAddressContacts(Account account) {
        if (account != null) {
            if (StringUtils.isBlank(account.getIdentification().getId())) {
                handleBadRequestException("Exception during deleteContact account validation - account ID is invalid",
                        AccountApiConstants.ERROR_ACCOUNT_ID_IS_INVALID, EbpiError.EBPI_403_ACCT_001.getCode());
            }
            if (StringUtils.isBlank(account.getAccountName())) {
                handleBadRequestException("Exception during deleteContact account validation - account name is invalid",
                        AccountApiConstants.ERROR_ACCOUNT_NAME_IS_INVALID, EbpiError.EBPI_403_ACCT_001.getCode());
            }
            if ((account.getAddresses() == null)) {
                handleBadRequestException("Exception during deleteContact account validation - addresses associated with the account is empty",
                        AccountApiConstants.ERROR_EMPTY_ADDRESS, EbpiError.EBPI_404_ACCT_003.getCode());
            }
            if ((account.getContacts() == null)) {
                handleBadRequestException("Exception during deleteContact account validation - contacts associated with the account is empty",
                        AccountApiConstants.ERROR_EMPTY_CONTACT, EbpiError.EBPI_404_ACCT_002.getCode());
            }
        }
    }


    private void validateAccountForBillOrderQuote(ContactDeleteValidationOutput contactDeleteValidationOutput) {
        log.info("Post Siebel deleteContact validation");
        String foundBillToOnOrder = contactDeleteValidationOutput.getFoundBillToOnOrder();
        if(StringUtils.isNotEmpty(foundBillToOnOrder) && AccountApiConstants.FOUND_BILL_TO_ON_ORDER.equalsIgnoreCase(foundBillToOnOrder)) {
            handleBadRequestException("Exception during Seibel deleteContact Validation foundBillToOnQuote",
                    AccountApiConstants.ERROR_CONTACT_IS_PART_OF_BILL_IN_OPEN_ORDER, EbpiError.EBPI_403_ACCT_011.getCode());
        }
        String foundBillToOnQuote = contactDeleteValidationOutput.getFoundBillToOnQuote();
        if(StringUtils.isNotEmpty(foundBillToOnQuote) && AccountApiConstants.FOUND_BILL_TO_ON_QUOTE.equalsIgnoreCase(foundBillToOnQuote)) {
            handleBadRequestException("Exception during Seibel deleteContact Validation foundBillToOnQuote",
                    AccountApiConstants.ERROR_CONTACT_IS_PART_OF_BILL_IN_OPEN_QUOTE, EbpiError.EBPI_403_ACCT_012.getCode());
        }
        String foundOnBillingProfile = contactDeleteValidationOutput.getFoundOnBillingProfile();
        if(StringUtils.isNotEmpty(foundOnBillingProfile) && AccountApiConstants.FOUND_ON_BILLING_PROFILE.equalsIgnoreCase(foundOnBillingProfile)) {
            handleBadRequestException("Exception during Seibel deleteContact Validation foundOnBillingProfile",
                    AccountApiConstants.ERROR_CONTACT_ON_BILLING_PROFILE, EbpiError.EBPI_403_ACCT_013.getCode());
        }
        String foundPrimaryOnOrder = contactDeleteValidationOutput.getFoundPrimaryOnOrder();
        if(StringUtils.isNotEmpty(foundPrimaryOnOrder) && AccountApiConstants.FOUND_PRIMARY_ON_ORDER.equalsIgnoreCase(foundPrimaryOnOrder)) {
            handleBadRequestException("Exception during Seibel deleteContact Validation foundPrimaryOnOrder",
                    AccountApiConstants.ERROR_CONTACT_PRIMARY_ON_ORDER, EbpiError.EBPI_403_ACCT_014.getCode());
        }
        String foundPrimaryOnQuote = contactDeleteValidationOutput.getFoundPrimaryOnQuote();
        if(StringUtils.isNotEmpty(foundPrimaryOnQuote) && AccountApiConstants.FOUND_PRIMARY_ON_QUOTE.equalsIgnoreCase(foundPrimaryOnQuote)) {
            handleBadRequestException("Exception during Seibel deleteContact Validation foundPrimaryOnQuote",
                    AccountApiConstants.ERROR_CONTACT_PRIMARY_ON_QUOTE, EbpiError.EBPI_403_ACCT_015.getCode());
        }
        String foundShipToOnQuote = contactDeleteValidationOutput.getFoundShipToOnQuote();
        if(StringUtils.isNotEmpty(foundShipToOnQuote) && AccountApiConstants.FOUND_SHIP_TO_ON_QUOTE.equalsIgnoreCase(foundShipToOnQuote)) {
            handleBadRequestException("Exception during Seibel deleteContact Validation foundShipToOnQuote",
                    AccountApiConstants.ERROR_CONTACT_FOUND_SHIP_TO_ON_QUOTE, EbpiError.EBPI_403_ACCT_016.getCode());
        }
        String foundShipToOnOrder = contactDeleteValidationOutput.getFoundShipToOnOrder();
        if(StringUtils.isNotEmpty(foundShipToOnOrder) && AccountApiConstants.FOUND_SHIP_TO_ON_ORDER.equalsIgnoreCase(foundShipToOnOrder)) {
            handleBadRequestException("Exception during Seibel deleteContact Validation foundShipToOnOrder",
                    AccountApiConstants.ERROR_CONTACT_FOUND_SHIP_TO_ON_ORDER, EbpiError.EBPI_403_ACCT_017.getCode());
        }
        String primaryBillToOnAccount = contactDeleteValidationOutput.getPrimaryBillToOnAccount();
        if(StringUtils.isNotEmpty(primaryBillToOnAccount) && AccountApiConstants.PRIMARY_BILL_TO_ON_ACCOUNT.equalsIgnoreCase(primaryBillToOnAccount)) {
            handleBadRequestException("Exception during Seibel deleteContact Validation primaryBillToOnAccount",
                    AccountApiConstants.ERROR_CONTACT_PRIMARY_BILL_TO_ON_ACCOUNT, EbpiError.EBPI_403_ACCT_018.getCode());
        }
        String primaryShipToOnAccount = contactDeleteValidationOutput.getPrimaryShipToOnAccount();
        if(StringUtils.isNotEmpty(primaryShipToOnAccount) && AccountApiConstants.PRIMARY_SHIP_TO_ON_ACCOUNT.equalsIgnoreCase(primaryShipToOnAccount)) {
            handleBadRequestException("Exception during Seibel deleteContact Validation primaryShipToOnAccount",
                    AccountApiConstants.ERROR_CONTACT_PRIMARY_SHIP_TO_ON_ACCOUNT, EbpiError.EBPI_403_ACCT_019.getCode());
        }
        String primaryOnAccount = contactDeleteValidationOutput.getPrimaryOnAccount();
        if(StringUtils.isNotEmpty(primaryOnAccount) && AccountApiConstants.PRIMARY_ON_ACCOUNT.equalsIgnoreCase(primaryOnAccount)) {
            handleBadRequestException("Exception during Seibel deleteContact Validation primaryOnAccount",
                    AccountApiConstants.ERROR_CONTACT_PRIMARY_ON_ACCOUNT, EbpiError.EBPI_403_ACCT_020.getCode());
        }
    }

    public void deleteContact(String accountId, String contactId, GetV2AccountContext context)
    {
        Headers headers = context.getHeader();
        String authorization = headers.getAuthorization();
        SecurityResponse securityResponse = context.getSecurityResponse();
        String intuitRealmId =  securityResponse.getRealmID();
        String scheme = "MDM";
        boolean isAgent = securityResponse.isCSRUser();
        final ContactDeleteValidationOutput contactDeleteValidationOutput;
//        isAgent = true;
        log.info("delete Contact service started for accountId={} contactId={} " , accountId,  contactId );
        if (intuitRealmId != null && isAgent) {
            log.info("deleteContact Agent flow");
            Account account = getAccountById(accountId, scheme, context);
            validateAccountForAddressContacts(account);
            ContactDeleteValidationInput contactDeleteValidationInput = new ContactDeleteValidationInput();
            contactDeleteValidationInput.setCAN(accountId);
            contactDeleteValidationInput.setCACI(contactId);
            contactDeleteValidationOutput = validateDeleteContact(contactDeleteValidationInput, headers);
            validateAccountForBillOrderQuote(contactDeleteValidationOutput);
            String okToDelete = contactDeleteValidationOutput.getOkToDelete();
            String noMatchId = contactDeleteValidationOutput.getNoMatchID();
            String errorSpcMessage = contactDeleteValidationOutput.getErrorSpcMessage();
            log.info("okToDelete={} noMatchId={} errorSpcMessage={}", okToDelete , noMatchId , errorSpcMessage);
            if (StringUtils.isNotEmpty(okToDelete) && okToDelete.equalsIgnoreCase(AccountApiConstants.OK_TO_DELETE)) {
                DeleteContactInputDataType deleteContactInputDataType = new DeleteContactInputDataType();
                CustomerAccountHeaderType customerAccountHeaderType = new CustomerAccountHeaderType();
                DeleteContactInputType deleteContactInputType = new DeleteContactInputType();
                deleteContactInputDataType.setCustomerAccountContactId(contactId);
                deleteContactInputDataType.setCustomerAccountNumber(accountId);
                customerAccountHeaderType.setExternalSystemId(AccountApiConstants.SFDC_SYSTEM_ID);
                customerAccountHeaderType.setOperation(AccountApiConstants.MDM_DELETE_CONTACT_OPERATION);
                deleteContactInputType.setCustomerAccountHeader(customerAccountHeaderType);
                deleteContactInputType.setDeleteContactInputData(deleteContactInputDataType);
                try {
                    log.info("deleteContact Call mdm sdk deleteContact");
                    accountMdmSdkService.deleteContact(deleteContactInputType, headers);
                } catch (CustomerAccountException e) {
                    handleBadRequestException("CustomerAccountException during deleteContact at MDM",
                            AccountApiConstants.ERROR_MDM_DATA_VALIDATION, EbpiError.ACCT_401100.getCode());
                } catch (ServiceUnavailableException e) {
                    handleBadRequestException("ServiceUnavailableException during deleteContact at MDM",
                            AccountApiConstants.ERROR_MDM_INVOCATION, EbpiError.ACCT_401101.getCode());
                } catch (Exception e) {
                    log.error("Exception during deleteContact at MDM");
                    List<Pair<String, String>> errorDetails = new ArrayList<>();
                    errorDetails.add(Pair.of(EbpiCommonConstants.INTERNAL_SERVER_ERROR, null));
                    throw new InternalServerException(EbpiError.ACCT_201040.getCode(), errorDetails);
                }
            } else if (StringUtils.isNotEmpty(okToDelete) && !(okToDelete.equalsIgnoreCase(
                    AccountApiConstants.OK_TO_DELETE)))
            {
                handleBadRequestException("Exception during Seibel deleteContact Validation NotOkToDelete",
                        EbpiCommonConstants.RECORD_NOT_OK_TO_DELETE, EbpiError.ACCT_401120.getCode());
            } else if (StringUtils.isNotEmpty(noMatchId) && noMatchId.equalsIgnoreCase(
                    AccountApiConstants.NO_MATCH_ID))
            {
                handleBadRequestException("Exception during deleteContact Siebel Validation and record not found in siebel",
                        EbpiCommonConstants.RECORD_NOT_FOUND_IN_SIEBEL, EbpiError.ACCT_401122.getCode());
            } else if (StringUtils.isNotEmpty(errorSpcMessage)) {
                handleBadRequestException("Exception during deleteContact Siebel Validation and errorSpcMessage is not empty",
                        EbpiCommonConstants.SIEBEL_VALIDATION_FAILURE, EbpiError.ACCT_401123.getCode());
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

    private ContactDeleteValidationOutput validateDeleteContact(ContactDeleteValidationInput contactDeleteValidationInput,
                                                                Headers headers)
    {
        try {
            return siebelAccountSdkService.validateDeleteContact(contactDeleteValidationInput, headers);
        } catch (Exception e) {
            log.error("validateDeleteContact Exception during Siebel Validation");
            if (e instanceof ServiceUnavailableException) {
                handleBadRequestException("validateDeleteContact Exception during Siebel Validation",
                        EbpiCommonConstants.ERROR_INVOKING_SIEBEL, EbpiError.ACCT_401120.getCode());
            } else {
                List<Pair<String, String>> errorDetails = new ArrayList<>();
                errorDetails.add(Pair.of(EbpiCommonConstants.INTERNAL_SERVER_ERROR, null));
                throw new InternalServerException(EbpiError.ACCT_401121.getCode(), errorDetails);
            }
        }
        return null;
    }

}
