# Java 8 extended support until December 2030
target_version := --release 8 -Xlint:-options

APP_VERSION:=$(shell grep "public static final String APP_VERSION" java/DatatoolTk.java | sed "s/public\sstatic\sfinal\sString\sAPP_VERSION=//" | tr -d "\"\; ")
ifeq ($(IZPACKDIR),)
IZPACKDIR:=/usr/local/IzPack
endif
POIVER=5.3.0

test-gui	: app
	bin/datatooltk-gui --debug

test-csv	: app
	bin/datatooltk-gui --debug --csv tests/test-para.csv

test-cli	: app
	bin/datatooltk --debug --output tests/test-out.dbtex --in tests/data-raw.dbtex
	bin/datatooltk --debug --output tests/test-csv-out.dbtex --csv tests/test.csv

test-sql	: app
	bin/datatooltk --debug --output tests/test-sql-data.dbtex --sql "SELECT * FROM testsqldata" --sqldb datatooltk --sqluser datatool

app	: lib lib/datatooltk.jar 

dist	: ../dist/datatooltk-installer.jar
	cp doc/datatooltk-en-GB.pdf ../dist
	cp doc/datatooltk.1 ../dist

../dist/datatooltk-installer.jar	: app installer/install.xml
	"$(IZPACKDIR)/bin/compile" installer/install.xml -b . \
	-h "$(IZPACKDIR)" \
	-o ../dist/datatooltk-installer.jar -k standard

lib/resources/helpsets/datatooltk-en-GB/index.xml	: lib/resources/dictionaries/*.xml doc/datatooltk-en-GB.tex doc/datatooltk-main.tex doc/datatooltk.bib doc/version.tex
	make -C doc en-GB

lib/resources/helpsets/datatooltk-en/index.xml	: lib/resources/dictionaries/*.xml doc/datatooltk-en.tex doc/datatooltk-main.tex doc/datatooltk.bib doc/version.tex
	make -C doc en

lib/datatooltk.jar	: java/combined/Manifest.txt \
			classes/com/dickimawbooks/datatooltk/combined/CombinedDatatoolTk.class \
			lib/resources/helpsets/datatooltk-en/index.xml \
			lib/resources/helpsets/datatooltk-en-GB/index.xml 
	cp -u -r java/gui/icons classes/com/dickimawbooks/datatooltk/gui/
	cd classes; \
	jar cmf ../java/combined/Manifest.txt ../lib/datatooltk.jar \
	com/dickimawbooks/datatooltk/*.class \
	com/dickimawbooks/datatooltk/*/*.class \
	com/dickimawbooks/datatooltk/gui/icons/*.png

classes/com/dickimawbooks/datatooltk/combined/CombinedDatatoolTk.class	: classes/com/dickimawbooks/datatooltk \
	java/*.java java/gui/*.java \
	java/io/*.java java/combined/*.java \
	lib/texjavahelplib.jar lib/mysql-connector-java.jar\
	lib/poi-$(POIVER).jar lib/poi-ooxml-$(POIVER).jar \
	lib/poi-ooxml-full-$(POIVER).jar \
	lib/apache-commons-io.jar \
	lib/log4j-api.jar \
	lib/texjavahelplib.jar \
	lib/texparserlib.jar
	cd java; \
	javac $(target_version) -d ../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	-cp ../lib/texjavahelplib.jar:../lib/mysql-connector-java.jar:../lib/poi-$(POIVER).jar:../lib/poi-ooxml-$(POIVER).jar:../lib/poi-ooxml-full-$(POIVER).jar:../lib/apache-commons-io.jar:../lib/log4j-api.jar:../lib/texparserlib.jar \
	*.java combined/*.java io/*.java gui/*.java

classes/com/dickimawbooks/datatooltk	:
	mkdir -p classes/com/dickimawbooks/datatooltk

clean	:
	\rm -rf classes/com

squeaky	:
	\rm -f lib/datatooltk.jar
	make -C doc clean
