APP_VERSION:=$(shell grep "public static final String appVersion" src/DatatoolTk.java | sed "s/public\sstatic\sfinal\sString\sappVersion=//" | tr -d "\"\; ")

test-gui	: app
	bin/datatooltk --gui tests/data-raw.dbtex

test-cli	: app
	bin/datatooltk --out tests/test-out.dbtex tests/data-raw.dbtex
	bin/datatooltk --out tests/test-csv-out.dbtex --csv tests/test.csv
	bin/datatooltk --out tests/test-sql-data.dbtex --sql "SELECT * FROM testsqldata" --sqldb datatooltk --sqluser datatool

app	: lib/datatooltk.jar lib/jh.jar lib/jlfgr-1_0.jar lib/resources

lib/resources   :
	cd lib; ln -s ../resources

lib/datatooltk.jar	: src/Manifest.txt \
			classes/com/dickimawbooks/datatooltk/DatatoolTk.class
	cd classes; \
	jar cmf ../src/Manifest.txt ../lib/datatooltk.jar \
	com/dickimawbooks/datatooltk/*.class \
	com/dickimawbooks/datatooltk/*/*.class 

classes/com/dickimawbooks/datatooltk/DatatoolTk.class	: classes/com/dickimawbooks/datatooltk \
	src/*.java src/*/*.java lib/jh.jar lib/jlfgr-1_0.jar
	cd src; \
	javac -d ../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	-cp ../lib/jh.jar:../lib/jlfgr-1_0.jar:../lib/opencsv.jar *.java */*.java

classes/com/dickimawbooks/datatooltk	:
	mkdir -p classes/com/dickimawbooks/datatooltk

clean	:
	\rm -f classes/com/dickimawbooks/datatooltk/*.class

squeaky	:
	\rm -f lib/datatooltk.jar
