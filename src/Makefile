APP_VERSION:=$(shell grep "public static final String appVersion" java/DatatoolTk.java | sed "s/public\sstatic\sfinal\sString\sappVersion=//" | tr -d "\"\; ")

test-gui	: app
	bin/datatooltk --gui tests/data-raw.dbtex

test-cli	: app
	bin/datatooltk --out tests/test-out.dbtex tests/data-raw.dbtex
	bin/datatooltk --out tests/test-csv-out.dbtex --csv tests/test.csv

test-sql	: app
	bin/datatooltk --out tests/test-sql-data.dbtex --sql "SELECT * FROM testsqldata" --sqldb datatooltk --sqluser datatool

app	: lib/datatooltk.jar lib/javahelp2.jar lib/resources

lib/resources   :
	cd lib; ln -s ../resources

resources/icons	: 
	cd resources; ln -s ../images/icons

lib/datatooltk.jar	: java/Manifest.txt \
			resources/icons \
			classes/com/dickimawbooks/datatooltk/DatatoolTk.class \
			doc/createdocs doc/datatooltk-*.xml doc/manual.dtd
	make -C doc
	cd classes; \
	jar cmf ../java/Manifest.txt ../lib/datatooltk.jar \
	com/dickimawbooks/datatooltk/*.class \
	com/dickimawbooks/datatooltk/*/*.class 

classes/com/dickimawbooks/datatooltk/DatatoolTk.class	: classes/com/dickimawbooks/datatooltk \
	java/*.java java/*/*.java
	cd java; \
	javac -d ../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	-cp ../lib/javahelp2.jar:../lib/opencsv.jar:../lib/mysql-connector-java.jar *.java */*.java

classes/com/dickimawbooks/datatooltk	:
	mkdir -p classes/com/dickimawbooks/datatooltk

clean	:
	\rm -f classes/com/dickimawbooks/datatooltk/*.class
	\rm -f classes/com/dickimawbooks/datatooltk/*/*.class

squeaky	:
	\rm -f lib/datatooltk.jar
