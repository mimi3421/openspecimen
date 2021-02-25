Standalone install on Windows 10 in console
============

> Change {..} to the actual path

1. Download files

   1. https://git-scm.com/download/win
   2. https://tomcat.apache.org/download-80.cgi # 9.0.37
   3. https://dev.mysql.com/downloads/mysql/ # 8.0.23
   4. https://downloads.gradle-dn.com/distributions/gradle-4.10.2-bin.zip # MUST BE VERSION 4.x
   5. https://dev.mysql.com/downloads/connector/j/ # MUST BE 8.0.20 if mysql version is 8.0.23, copy mysql-connector-java-8.0.20.jar to {..}/apache-tomcat-9.0.37/lib
   6. https://nodejs.org/en/download/ # 14.16.0
   7. https://github.com/mimi3421/openspecimen/releases # release v8.0.b1

2. Environment

   ```bash
   set PATH={..}\server\gradle-6.8.3\bin;{..}\node-v14.16.0-win-x64\node_modules\.bin;{..}\Git\bin;{..}\Java\jdk1.8.0_241\bin;{..}\mysql-8.0.23-winx64\bin;{..}\node-v14.16.0-win-x64
   
   set JAVA_HOME={..}\Java\jdk1.8.0_241
   
   set CLASSPATH={..}\apache-tomcat-9.0.37\lib
   
   set TOMCAT_HOME={..}\apache-tomcat-9.0.37
   
   ```

3. Initialize mysql

   1. Create my.ini in {..}/mysql-8.0.23-winx64

      ```ini
      #https://dev.mysql.com/doc/refman/8.0/en/option-files.html
      [mysqld]
      port=3306
      basedir={..}\MySql
      datadir={..}\DataBase\MySql
      max_connections=200
      max_connect_errors=10
      character-set-server=utf8
      default-storage-engine=INNODB
      default_authentication_plugin=mysql_native_password
      disable_log_bin
      skip-log-bin
      
      [mysql]
      default-character-set=utf8
      
      [client]
      port=3306
      default-character-set=utf8
      
      ```
      
   2. Initialization

      ```bash
      mysqld --initialize --console
      # Markdown the original random root password from the output
      ```

   3. Create database

      ```sql
      # Use heidisql or other mysql client to create databse and user, change root password to PASSWORD
      # DROP DATABASE openspecimen
      CREATE DATABASE openspecimen
      ```

4. Prepare tomcat

   1. Create openspecimen.properties in ${TOMCAT_HOME}/conf
      ```ini
      #######################################################################
      # Windows users, please note the directories should be separated using 
      # forward slashes as in below example:
      # D:/openspecimen/tomcat
      # D:/opnespecimen/data
      # D:/openspecimen/plugins
      #######################################################################
      
      # This field is useful for deploying multiple OpenSpecimen 
      # instances on the one Tomcat server. E.g. "os-test" and 
      # "os-prod".
      app.name=openspecimen
      
      # Name of datasource configured in  the Tomcat's context.xml.
      # Usually, it is "jdbc/openspecimen".
      datasource.jndi=jdbc/openspecimen 
      
      # Value can be mysql or oracle
      database.type=mysql
      
      # Value can be fresh or upgrade.
      # This should be "upgrade" if the database was initially upgraded from caTissue.
      # Note: This should be fresh for all other cases.
      datasource.type=fresh
      
      # Absolute path where Tomcat is installed
      tomcat.dir={..}/apache-tomcat-9.0.37
      
      # Absolute path to OpenSpecimen data directory.
      # Best practice: Create a folder in parallel to 'tomcat.dir' with 
      # the name "openspecimen/data"
      app.data_dir={..}/OpenSpeciemen/data
       
      # Absolute path to OpenSpecimen data directory.
      # Best practice: Create a folder in parallel to 'tomcat.dir' with 
      # the name "openspecimen/plugins"
      plugin.dir={..}/OpenSpeciemen/plugins
      
      # Optional. Absolute path of the customised logging configuration file.
      # The configuration file should follow log4j configuration rules.
      app.log_conf=
      
      ```
   3. Add lines in ${TOMCAT_HOME}/conf/context.xml inside \<Context\>. Must use root user or user with SUPER privilege if binary logging is enabled in mysql.

      ```xml
      <Resource name="jdbc/openspecimen" auth="Container" type="javax.sql.DataSource"
            maxTotal="100" maxIdle="30" maxWaitMillis="10000"
            username="root" password="PASSWORD"
            driverClassName="com.mysql.cj.jdbc.Driver"
            url="jdbc:mysql://localhost:3306/openspecimen?serverTimezone=UTC&amp;characterEncoding=utf8&amp;useUnicode=true&amp;useSSL=true"
            testOnBorrow="true" validationQuery="select 1 from dual" />
      <Environment
            name="config/openspecimen"
            value="{..}/apache-tomcat-9.0.37/conf/openspecimen.properties"
            type="java.lang.String"/>
      ```


5. Install npm modules

   1. Install git

   2. Set npm mirrors if needed

      ```bash
      npm config set registry https://registry.npm.taobao.org
      # npm config get registry # Check if success
      ```

   3. Install module 

      ```bash
      pushd {..}\openspecimen-8.0.b1\www
      npm install bower
      npm install grunt-cli 
      ```

   4. Add {..}/node_modules/.bin to PATH

6. Prepare openspecimen compile
      

   1. Edit {..}/openspecimen-8.0.b1/build.properties

      ```ini
      app_home={..}/apache-tomcat-9.0.37
      ```


   3. Add mirrors of maven if needed in {..}/openspecimen-8.0.b1/build.gradle

      ```java
      repositories {
          maven {
            url 'https://maven.aliyun.com/repository/public/'
          }
        mavenCentral()
      }
      ```

   5. Comple everything with gradle

      ```bash
      pushd {..}\openspecimen-8.0.b1\www
      
      # grunt must be installed locally
      npm install grunt --save-dev
      npm install
      bower install
      
      cd ..
      
      # Comple and put generated WAR file to app_home/webapps setted in {..}/openspecimen-8.0.b1/build.properties
      gradle deploy 
      
      # The following is not needed
      
      # Comple jar only
      # gradle compileJava 
      
      # Comple and generate WAR file only
      # gradle build 
      ```


7. Start the daemon

   1. Set the port in {..}/apache-tomcat-9.0.37/conf/server.xml . The default is 8080

      ```xml
      <Connector port="8080" protocol="HTTP/1.1"
                     connectionTimeout="20000"
                     redirectPort="8443" />
      ```


   2. Add tomcat-user：{..}/apache-tomcat-9.0.37/conf/tomcat-users.xml

      ```xml
      <tomcat-users>
      
      ...
      <role rolename="admin-gui"/>
        <role rolename="manager-gui"/>
        <user username="tomcat" password="tomcat" roles="admin-gui,manager-gui"/>
        
      ...
      </tomcat-users>
      ```


   3. Start standalone

      ```bash
      mysqld --standalone --console
      {..}\apache-tomcat-9.0.37\bin\catalina.bat start
      ```


   4. Test from browser

      ```
      # Check app status, username tomcat password tomcat
      http://localhost:8080/manager/html
      # The site, username admin password Login@123
      http://localhost:8080/openspecimen
      ```





OpenSpecimen
============

An Open Source Biobanking Informatics Platform 
Developed by Krishagni Solutions (India)


Introduction
------------
OpenSpecimen (formerly known as caTissue Plus) is a Free & Open Source biobank/biospecimen management software. At the heart of OpenSpecimen is that “biospecimens without high quality data is of no value”. OpenSpecimen is used across the globe in some of the most respected biobanks of various sizes and diseases. OpenSpecimen streamlines management of biospecimens across collection, consent, QC, request and distribution. Finally, OpenSpecimen is highly configurable and customizable. E.g., adding a custom field or form can be done in minutes by a non-IT person. 

Krishagni Solutions (India) actively develops newer versions of OpenSpecimen as well provides professional support. Support includes phone/email, data migration, customizations, integrations, training, and so forth. 

For more details visit: www.openspecimen.org

Features
---------
 * Planned and unplanned biospecimen collections
 * Storage and Inventory management
 * Role based user authentication and authorization
 * Participants and informed consents
 * Path reports
 * Clinical and pathology annotations
 * Specimen request and distribution 
 * Shipping and tracking
 * Track specimen lifecycle events
 * Reports: Data query, Pivot table, Export as CSV, Scheduled reports
 * Custom fields and forms
 * Complete audit trial
 * Bulk Operation to upload CSV data
 * Token based specimen label generation
 * Barcode printing
 * REST APIs
 * Integration with clinical databases
 * Integration with instruments
 * Real time notifications
 
 Upcoming features
 ------------------
 * Patient family relationships (family pedigree)
 * Invoicing
 * Tissue Microarray
 * Highly configurable user interface
 * Mobile and tablet apps
 
Online Resources
----------------
Website: http://www.openspecimen.org
Forums: http://forums.openspecimen.org
Help: http://help.openspecimen.org
Email: mailto:contact@openspecimen.org

License
--------
OpenSpecimen is released under BSD 3 Style license. 
https://github.com/krishagni/openspecimen/blob/trunk/LICENSE.md

Contact Information
-------------------
Project Lead: Srikanth Adiga
Email: srikanth.adiga@openspecimen.org
