# 2020ebpiv2
EBPI opensource project contribution 
* delete contact
* com.intuit.platform.services.ebpi.account.apis.exe.AccountServiceApplication
* -Dspring.profiles.active=local
* mvn dependency:tree -Dincludes=com.intuit.platform.services.common:platform-common-sdk-mdm
* mvn dependency:tree -Dincludes=com.intuit.platform.services.common:platform-common-schema-ebpi-account

# Lessions learned
* mvn clean install - prehook git push

# child versions are in parent 
        <platform-common-parent.version>1.1.9.0-SNAPSHOT</platform-common-parent.version>
        <platform-common-library.version>1.1.31.0-SNAPSHOT</platform-common-library.version>
        <platform-common-schema.version>1.1.29.0-SNAPSHOT</platform-common-schema.version>
        <platform-common-sdk.version>1.1.45.0-SNAPSHOT</platform-common-sdk.version>

# mvn clean install - hierarchy issues 
* cd /Users/mpaulose/dev/platform-common-library
* mvn clean install 
* cd /Users/mpaulose/dev/platform-common-schema
* mvn clean install
* cd /Users/mpaulose/dev/platform-common-sdk
* mvn clean install
* cd /Users/mpaulose/dev/ebpi-common
* mvn clean install
* cd /Users/mpaulose/dev/ebpi-acct-api
* mvn clean install
