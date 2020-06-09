curl --location --request GET 'https://enterprise-account-e2e.platform.intuit.com/v1/accountDetails?accountNumber=800599235' \
--header 'Authorization: Intuit_IAM_Authentication intuit_appid=Intuit.tech.ebs.account.test, intuit_app_secret=qrmzkskHlc3DlxqeaUZANZ,intuit_token=V1-213-X3b27zz1k3sujn03is2svq, intuit_userid=9130350965525906' \
--header 'Accept: application/json' \
--header 'Content-Type: application/xml'

curl --location --request DELETE 'https://enterprise-account-e2e.platform.intuit.com/v1/accounts/c71bb361-9997-476f-9c95-9d99c6f7710f/contacts/0261f351-808e-461b-8a48-a2f2be0506b6' \
--header 'Authorization: Intuit_IAM_Authentication intuit_appid=Intuit.tech.ebs.account.test, intuit_app_secret=qrmzkskHlc3DlxqeaUZANZ,intuit_token=V1-160-b3v0zpa79pi4tgoxuva6to, intuit_userid=123148661833039' \
--header 'Accept: application/json' \
--header 'Content-Type: application/xml' \
--data-raw ''

