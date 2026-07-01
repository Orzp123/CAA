@echo off
set JAVA_HOME=D:\software\jdk21
set PATH=D:\software\jdk21\bin;%PATH%
D:\software\apache-maven-3.8.1\bin\mvn.cmd -f D:\agent\CAA\backend\pom.xml test -Dtest=PermissionServiceTest,TenantPermissionServiceTest -Dsurefire.failIfNoSpecifiedTests=false
