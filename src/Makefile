APP_VERSION:=$(shell grep "public static final String APP_VERSION" java/DatatoolTk.java | sed "s/public\sstatic\sfinal\sString\sAPP_VERSION=//" | tr -d "\"\; ")
IZPACKDIR=/usr/local/IzPack
POIVER=3.9-20121203
ODFVER=1.3

test-gui	: app
	bin/datatooltk-gui --debug

test-csv	: app
	bin/datatooltk-gui --debug --csv tests/test-para.csv

test-cli	: app
	bin/datatooltk --debug --output tests/test-out.dbtex --in tests/data-raw.dbtex
	bin/datatooltk --debug --output tests/test-csv-out.dbtex --csv tests/test.csv

test-sql	: app
	bin/datatooltk --debug --output tests/test-sql-data.dbtex --sql "SELECT * FROM testsqldata" --sqldb datatooltk --sqluser datatool

app	: lib lib/datatooltk.jar lib/resources lib/datatooltk-splash.png

dist	: ../dist/datatooltk-installer.jar
	cp doc/datatooltk-en-GB.pdf ../dist
	cp doc/datatooltk.1 ../dist

../dist/datatooltk-installer.jar	: app installer/install.xml
	$(IZPACKDIR)/bin/compile installer/install.xml -b . \
	-o ../dist/datatooltk-installer.jar -k standard

lib	:
	mkdir -p lib

lib/resources   :
	cd lib; ln -s ../resources

resources/icons	: 
	cd resources; ln -s ../images/icons

lib/datatooltk-splash.png	: lib
		cd lib; ln -s -f ../images/datatooltk-splash.png

lib/datatooltk.jar	: java/Manifest.txt \
			resources/icons \
			classes/com/dickimawbooks/datatooltk/DatatoolTk.class \
			doc/createdatatooltkdocs doc/datatooltk-*.xml doc/datatooltk.dtd
	make -C doc
	cd classes; \
	jar cmf ../java/Manifest.txt ../lib/datatooltk.jar \
	com/dickimawbooks/datatooltk/*.class \
	com/dickimawbooks/datatooltk/*/*.class 

classes/com/dickimawbooks/datatooltk/DatatoolTk.class	: classes/com/dickimawbooks/datatooltk \
	java/*.java java/*/*.java \
	lib/javahelp2.jar lib/opencsv.jar lib/mysql-connector-java.jar\
	lib/poi-$(POIVER).jar lib/poi-ooxml-$(POIVER).jar \
	lib/poi-ooxml-schemas-$(POIVER).jar \
	lib/jOpenDocument-$(ODFVER).jar \
	lib/texparserlib.jar
	cd java; \
	javac -target 1.7 -source 1.7 -d ../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	-cp ../lib/javahelp2.jar:../lib/opencsv.jar:../lib/mysql-connector-java.jar:../lib/poi-$(POIVER).jar:../lib/poi-ooxml-$(POIVER).jar:../lib/poi-ooxml-schemas-$(POIVER).jar:../lib/jOpenDocument-$(ODFVER).jar:../lib/texparserlib.jar *.java */*.java

classes/com/dickimawbooks/datatooltk	:
	mkdir -p classes/com/dickimawbooks/datatooltk

clean	:
	\rm -f classes/com/dickimawbooks/datatooltk/*.class
	\rm -f classes/com/dickimawbooks/datatooltk/*/*.class

squeaky	:
	\rm -f lib/datatooltk.jar
