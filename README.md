# 2020ebpiv2
EBPI opensource project contribution 
* delete contact
* com.intuit.platform.services.ebpi.account.apis.exe.AccountServiceApplication
* -Dspring.profiles.active=local
* mvn dependency:tree -Dincludes=com.intuit.platform.services.common:platform-common-sdk-mdm
# Lessions learned
* mvn clean install - prehook git push
# mvn clean install - hierarchy issues 
* cd /Users/mpaulose/dev/platform-common-library
* mvn clean install 
cd /Users/mpaulose/dev/platform-common-schema
mvn clean install
cd /Users/mpaulose/dev/platform-common-sdk
mvn clean install
cd /Users/mpaulose/dev/ebpi-common
mvn clean install
cd /Users/mpaulose/dev/ebpi-acct-api
mvn clean install
