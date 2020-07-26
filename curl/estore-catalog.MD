curl --location --request POST 'https://productcatalog10-1.api.intuit.com/v2/search/offerings' \
--header 'Content-Type: application/json' \
--header 'Authorization: Intuit_IAM_Authentication intuit_token_type=IAM-Ticket,intuit_appid=Intuit.tax.ptgdigitalmarketing.dashboardservice,intuit_app_secret=prdi92owhoII5hbR92bTVgTDsNlKLIy0d2VvZZE2,intuit_token=V1-30-X0gk7q7fcj33b35lcirjpj,intuit_userid=123145840530302' \
--header 'intuit_originatingip: 127.0.0.1' \
--header 'Cookie: SHOPPER_USER_ID=498555041890' \
--data-raw '{
   "offeringSearchRequests": [
      {
         "requestId": "677208154744490729",
         "itemNumber": "1101805",
         "transactionalAttributes": {
            "attributes": {
               "Tax Year": "Tax Year 2020",
               "Users": "1",
               "Fulfillment Method": "Download"
            }
         }
      }
   ]
}'
