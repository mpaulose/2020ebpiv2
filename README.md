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
        /Users/mpaulose/dev/platform-ebpi-parent/pom.xml        
        <platform-common-parent.version>1.1.9.0-SNAPSHOT</platform-common-parent.version>
        <platform-common-library.version>1.1.31.0-SNAPSHOT</platform-common-library.version>
        <platform-common-schema.version>1.1.29.0-SNAPSHOT</platform-common-schema.version>
        <platform-common-sdk.version>1.1.45.0-SNAPSHOT</platform-common-sdk.version>

        /Users/mpaulose/dev/platform-common-sdk/pom.xml
        <platform-common-library.version>1.1.31.0-SNAPSHOT</platform-common-library.version>
	<platform-common-schema.version>1.1.30.0-SNAPSHOT</platform-common-schema.version>

         /Users/mpaulose/dev/ebpi-acct-api/app/pom.xml:
         <ebpi-common.version>1.1.29.0-SNAPSHOT</ebpi-common.version>

# mvn clean install - hierarchy issues 
* cd /Users/mpaulose/dev/platform-common-library
* mvn clean install -DskipTests=true
* cd /Users/mpaulose/dev/platform-common-schema
* mvn clean install -DskipTests=true
* cd /Users/mpaulose/dev/platform-common-schema/platform-common-schema-mdm
* mvn clean install -DskipTests=true
* cd /Users/mpaulose/dev/platform-common-schema/platform-common-schema-ebpi/platform-common-schema-ebpi-account
* mvn clean install -DskipTests=true
* cd /Users/mpaulose/dev/platform-common-sdk
* mvn clean install -DskipTests=true
* cd /Users/mpaulose/dev/platform-common-sdk/platform-common-sdk-mdm/
* mvn clean install -DskipTests=true
* cd /Users/mpaulose/dev/platform-common-sdk/platform-common-sdk-siebel/
* mvn clean install -DskipTests=true
* cd /Users/mpaulose/dev/ebpi-common
* mvn clean install -DskipTests=true
* cd /Users/mpaulose/dev/ebpi-acct-api
* mvn clean install -DskipTests=true
* cd /Users/mpaulose/dev/ebpi-acct-api/app/ebpi-account-pub-apis-acct
* mvn clean install -DskipTests=true

# sync remote git
* git remote add upstream https://github.intuit.com/platform-services-common/platform-common-sdk
* git fetch upstream
* git checkout develop
* git rebase upstream/develop
* git push -f origin develop

