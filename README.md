#Silverpeas-Tools

##Building the library with MAVEN

> mvn clean install assembly:assembly

assembly:assembly is the goal that permits to build the library containing all the dependencies

##Change the last modified date
(org.silverpeas.tools.file.lastmodifieddate.Executor)

The working root folder is the one from which the program is executed.

#####Program parameters:
* **-ttt** [positive that represents the time from 1970-01-01], sets an explicit date
* **-oms** [negative or positive number], adds or removes milliseconds
* **-os** [negative or positive number], adds or removes seconds
* **-om** [negative or positive number], adds or removes minutes
* **-oh** [negative or positive number], adds or removes hours
* **-oD** [negative or positive number], adds or removes days
* **-oW** [negative or positive number], adds or removes weeks
* **-oM** [negative or positive number], adds or removes monthes
* **-oY** [negative or positive number], adds or removes years
* **-ms** [positive number], sets the milliseconds
* **-s** [positive number], sets the seconds
* **-m** [positive number], sets the minutes
* **-h** [positive number], sets the hours
* **-D** [positive number], sets the day
* **-M** [positive number], sets the month
* **-Y** [positive number], sets the year

#####Execution command
```shell
java java -classpath silverpeas-tools-1.0-SNAPSHOT-jar-with-dependencies.jar org.silverpeas.tools.file.lastmodifieddate.Executor [parameters]+ [file name]*
```

#####Exemples:
Adding one hour and one minute to the last modified date of all files contained into the working folder.
```shell
java -classpath silverpeas-tools-1.0-SNAPSHOT-jar-with-dependencies.jar org.silverpeas.tools.file.lastmodifieddate.Executor -oh 1 -om 1
```

Removing one week to the last modified date of the sepcified *toto.txt* *titi.txt* files (contained directly into the working directory).
```shell
java -classpath silverpeas-tools-1.0-SNAPSHOT-jar-with-dependencies.jar org.silverpeas.tools.file.lastmodifieddate.Executor -oW -1 toto.txt titi.txt
```

Setting to 30 the seconds of the last modified date of all files contained into the working folder.
```shell
java -classpath silverpeas-tools-1.0-SNAPSHOT-jar-with-dependencies.jar org.silverpeas.tools.file.lastmodifieddate.Executor -s 30
```

##Identify the files which the content verify several conditions
(org.silverpeas.tools.file.regexpr.Executor)

The working root folder is the one from which the program is executed.

The aim of this tool is to identify the files which the content verifies a set of conditions.
A condition is expressed by a regular expression.

#####Program parameters:
* **-f** [a relative or absolute path], sets a path into which the search must be done
* **-dirFilter** [a regular expression], only the folders which the name matches the regular expression will be taken into account
* **-!dirFilter** [a regular expression], only the folders which the name does not match the regular expression will be taken into account
* **-fileFilter** [a regular expression], only the files which the name matches the regular expression will be taken into account
* **-!fileFilter** [a regular expression], only the files which the name does not match the regular expression will be taken into account

#####Chaining the conditions:
Several conditions can be specified. The pipe separator `|` is used to separate each one.
The condition N+1 is verified on the files identified by the condition N.

It is better to encapsulate a condition into quotation marks in order to avoid unexpected results.

When a condition starts with `!` character, the file which does not verify the condition will be verified.

When a condition starts with `#` character, it will be simply ignored.
That is useful when the user is dealing with a lot of conditions. Indeed, the user can deactivate a condition in order to see the result and activate again rapidly in a next execution...

#####Execution command
```shell
java -classpath silverpeas-tools-1.0-SNAPSHOT-jar-with-dependencies.jar org.silverpeas.tools.file.regexpr.Executor [parameters]+ [conditions]+
```

#####Exemples:
Identify the files from folder _/Silverpeas/Silverpeas-Core_ or _/Silverpeas/Silverpeas-Components_, which the name corresponds to _`.+[.](j[a-z]+|jsp.inc|tag)$`_ and is not included at folder root or into a folder named _target_ and which following conditions are verified:
- the content must contain _<view:script_ or _<view:link_
- the content must not contain the string _"view"_
```shell
java -classpath silverpeas-tools-1.0-SNAPSHOT-jar-with-dependencies.jar org.silverpeas.tools.file.regexpr.Executor
-f /Silverpeas/Silverpeas-Core -f /Silverpeas/Silverpeas-Components
-fileFilter ".+[.](j[a-z]+|jsp.inc|tag)$" -!dirFilter "^([.]|target).*"
"(<view:script|<view:link)" | "!\"view\""
```

If the user must performed rapidly the previous search whithout verifying the first condition, instead of rewritting a new command, simply get back the previous command and start the first condition with '#'
```shell
java -classpath silverpeas-tools-1.0-SNAPSHOT-jar-with-dependencies.jar org.silverpeas.tools.file.regexpr.Executor
-f /Silverpeas/Silverpeas-Core -f /Silverpeas/Silverpeas-Components
-fileFilter ".+[.](j[a-z]+|jsp.inc|tag)$" -!dirFilter "^([.]|target).*"
"#(<view:script|<view:link)" | "!\"view\""
```
