# Java 8 extended support until December 2030
target_version := --release 8 -Xlint:-options

APP_VERSION:=$(shell grep "public static final String APP_VERSION" java/lib/DatatoolTk.java | sed -E  "s/.*\"(.*)\";/\1/")
ifeq ($(IZPACKDIR),)
IZPACKDIR:=/usr/local/IzPack
endif
POIVER=5.3.0

all	: lib/datatooltk.jar lib/datatooltk-restricted.jar lib/datatooltk-extra.jar

test-gui	: lib/datatooltk.jar
	bin/datatooltk-gui --debug

test-csv	: lib/datatooltk.jar
	bin/datatooltk-gui --debug --csv-sep ',' --csv tests/test-para.csv

test-cli	: lib/datatooltk-restricted.jar
	bin/datatooltk-restricted --debug --output tests/test-out.dbtex --in tests/data-raw.dbtex
	bin/datatooltk-restricted --debug --output tests/test-csv-out.dbtex --csv tests/test.csv

test-sql	: lib/datatooltk.jar
	bin/datatooltk --debug --output tests/test-sql-data.dbtex --sql "SELECT * FROM testsqldata" --sqldb datatooltk --sqluser datatool

test-extra	: lib/datatooltk-extra.jar
	bin/datatooltk-extra --debug --gui --xls tests/booklist.xls

dist	: ../unstable/datatooltk-installer.jar \
	../unstable/datatooltk-all.zip doc/LICENSE
	cp doc/datatooltk-en.pdf ../unstable
	cp doc/datatooltk.1 ../unstable
	sed "s/APPVERSION/$(APP_VERSION)/g" README-dist.md > ../unstable/README.md

doc/LICENSE	:
	cd doc; ln -s ../../LICENSE

../unstable/datatooltk-installer.jar	: lib/datatooltk-restricted.jar lib/datatooltk.jar lib/datatooltk-extra.jar installer/install.xml
	"$(IZPACKDIR)/bin/compile" installer/install.xml -b . \
	-h "$(IZPACKDIR)" \
	-o ../unstable/datatooltk-installer.jar -k standard

../unstable/datatooltk-all.zip	: lib/datatooltk-extra.jar lib/datatooltk-restricted.jar lib/datatooltk.jar
	mkdir -p ../unstable/datatooltk
	mkdir -p ../unstable/datatooltk/doc
	rsync -rLKptgov --exclude=".*" lib ../unstable/datatooltk
	rsync -rLKptgov --exclude=".*" bin ../unstable/datatooltk
	cp doc/*.{1,pdf} ../unstable/datatooltk/doc
	cp doc/README.md ../unstable/datatooltk/
	cp CHANGES ../unstable/datatooltk/
	cd ../unstable; zip -r datatooltk-all.zip datatooltk

../unstable/datatooltk-restricted.zip	: ../unstable/datatooltk-all.zip
	cd ../unstable; \
	zip datatooltk-restricted.zip datatooltk/ \
	datatooltk/README.md \
	datatooltk/CHANGES \
	datatooltk/bin/datatooltk-restricted* \
	datatooltk/doc/*.{1,pdf} \
	datatooltk/lib/texparserlib.jar \
	datatooltk/lib/texjavahelplib.jar \
	datatooltk/lib/datatooltklib.jar \
	datatooltk/lib/datatooltk-restricted.jar \
	datatooltk/lib/resources/ \
	datatooltk/lib/resources/dictionaries \
	datatooltk/lib/resources/dictionaries/*.xml 

../unstable/datatooltk.zip	: ../unstable/datatooltk-all.zip
	cd ../unstable; \
	zip datatooltk.zip -r  \
	datatooltk/README.md \
	datatooltk/CHANGES \
	datatooltk/bin/datatooltk-restricted* \
	datatooltk/bin/datatooltk-gui* \
	datatooltk/bin/datatooltk \
	datatooltk/bin/datatooltk.* \
	datatooltk/doc/*.{1,pdf} \
	datatooltk/lib/texparserlib.jar \
	datatooltk/lib/texjavahelplib.jar \
	datatooltk/lib/datatooltklib.jar \
	datatooltk/lib/datatooltkguilib.jar \
	datatooltk/lib/datatooltk-restricted.jar \
	datatooltk/lib/datatooltk.jar \
	datatooltk/lib/mysql-connector-java.jar \
	datatooltk/lib/*.{html,png} \
	datatooltk/lib/resources/ 

../unstable/datatooltk-extra.zip	: ../unstable/datatooltk-all.zip
	cd ../unstable; \
	zip datatooltk-extra.zip -r  \
	datatooltk/README.md \
	datatooltk/CHANGES \
	datatooltk/bin/datatooltk-restricted* \
	datatooltk/bin/datatooltk-extra* \
	datatooltk/doc/*.{1,pdf} \
	datatooltk/lib/texparserlib.jar \
	datatooltk/lib/texjavahelplib.jar \
	datatooltk/lib/datatooltklib.jar \
	datatooltk/lib/datatooltkguilib.jar \
	datatooltk/lib/datatooltk-restricted.jar \
	datatooltk/lib/datatooltk-extra.jar \
	datatooltk/lib/mysql-connector-java.jar \
	datatooltk/lib/poi-*.jar \
	datatooltk/lib/apache-commons-io.jar \
	datatooltk/lib/log4j-api.jar \
	datatooltk/lib/log4j-core.jar \
	datatooltk/lib/*.{html,png} \
	datatooltk/lib/resources/ 

lib/resources/helpsets/datatooltk-en-GB/index.xml	: lib/resources/dictionaries/*.xml doc/datatooltk-en-GB.tex doc/datatooltk-main.tex doc/datatooltk.bib doc/version.tex
	make -C doc en-GB

lib/resources/helpsets/datatooltk-en/index.xml	: lib/resources/dictionaries/*.xml doc/datatooltk-en.tex doc/datatooltk-main.tex doc/datatooltk.bib doc/version.tex
	make -C doc en

lib/datatooltklib.jar	: classes/com/dickimawbooks/datatooltk/DatatoolTk.class 
	cd classes; \
	jar cf ../lib/datatooltklib.jar \
	com/dickimawbooks/datatooltk/*.class \
	com/dickimawbooks/datatooltk/io/*.class 

classes/com/dickimawbooks/datatooltk/DatatoolTk.class	: classes/com/dickimawbooks/datatooltk \
	java/lib/*.java java/lib/io/*.java \
	lib/texjavahelplib.jar \
	lib/texparserlib.jar
	cd java/lib; \
	javac $(target_version) -d ../../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	-cp ../../lib/texjavahelplib.jar:../../lib/texparserlib.jar \
	*.java io/*.java

lib/datatooltkguilib.jar	: classes/com/dickimawbooks/datatooltk/gui/DatatoolGUI.class \
	java/gui/icons/*.png \
	lib/resources/helpsets/datatooltk-en/index.xml \
	lib/resources/helpsets/datatooltk-en-GB/index.xml
	cp -u -r java/gui/icons classes/com/dickimawbooks/datatooltk/gui/
	cd classes; \
	jar cf ../lib/datatooltkguilib.jar \
	com/dickimawbooks/datatooltk/gui/*.class \
	com/dickimawbooks/datatooltk/gui/icons/*.png

classes/com/dickimawbooks/datatooltk/gui/DatatoolGUI.class	: java/gui/*.java \
	lib/datatooltklib.jar \
	lib/texjavahelplib.jar \
	lib/texparserlib.jar
	cd java/gui; \
	javac $(target_version) -d ../../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	-cp ../../lib/datatooltklib.jar:../../lib/texjavahelplib.jar:../../lib/texparserlib.jar \
	*.java

lib/datatooltk.jar	: lib/datatooltkguilib.jar java/basic/Manifest.txt \
			classes/com/dickimawbooks/datatooltk/basic/BasicDatatoolTk.class 
	cd classes; \
	jar cmf ../java/basic/Manifest.txt ../lib/datatooltk.jar \
	com/dickimawbooks/datatooltk/basic/*.class 

classes/com/dickimawbooks/datatooltk/basic/BasicDatatoolTk.class	: classes/com/dickimawbooks/datatooltk \
	java/basic/*.java \
	lib/datatooltkguilib.jar \
	lib/texjavahelplib.jar \
	lib/texparserlib.jar
	cd java; \
	javac $(target_version) -d ../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	-cp ../lib/datatooltklib.jar:../lib/datatooltkguilib.jar:../lib/texjavahelplib.jar:../lib/texparserlib.jar \
	basic/*.java 

lib/datatooltk-extra.jar	: lib/datatooltkguilib.jar java/extra/Manifest.txt \
			classes/com/dickimawbooks/datatooltk/extra/ExtraDatatoolTk.class 
	cd classes; \
	jar cmf ../java/extra/Manifest.txt ../lib/datatooltk-extra.jar \
	com/dickimawbooks/datatooltk/extra/*.class 

classes/com/dickimawbooks/datatooltk/extra/ExtraDatatoolTk.class	: classes/com/dickimawbooks/datatooltk \
	java/extra/*.java \
	lib/datatooltkguilib.jar \
	lib/texjavahelplib.jar \
	lib/texparserlib.jar \
	lib/mysql-connector-java.jar \
	lib/poi-$(POIVER).jar lib/poi-ooxml-$(POIVER).jar \
	lib/poi-ooxml-full-$(POIVER).jar \
	lib/apache-commons-io.jar \
	lib/log4j-api.jar lib/log4j-core.jar 
	cd java; \
	javac $(target_version) -d ../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	-cp ../lib/datatooltklib.jar:../lib/datatooltkguilib.jar:../lib/texjavahelplib.jar:../lib/mysql-connector-java.jar:../lib/poi-$(POIVER).jar:../lib/poi-ooxml-$(POIVER).jar:../lib/poi-ooxml-full-$(POIVER).jar:../lib/apache-commons-io.jar:../lib/log4j-api.jar:../lib/log4j-core.jar:../lib/texparserlib.jar \
	extra/*.java

lib/datatooltk-restricted.jar	: lib/datatooltklib.jar java/restricted/Manifest.txt \
			classes/com/dickimawbooks/datatooltk/restricted/RestrictedDatatoolTk.class 
	cd classes; \
	jar cmf ../java/restricted/Manifest.txt ../lib/datatooltk-restricted.jar \
	com/dickimawbooks/datatooltk/restricted/*.class 

classes/com/dickimawbooks/datatooltk/restricted/RestrictedDatatoolTk.class	: classes/com/dickimawbooks/datatooltk \
	java/restricted/*.java \
	lib/datatooltklib.jar \
	lib/texjavahelplib.jar \
	lib/texparserlib.jar
	cd java; \
	javac $(target_version) -d ../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	-cp ../lib/datatooltklib.jar:../lib/texjavahelplib.jar:../lib/texparserlib.jar \
	restricted/*.java

classes/com/dickimawbooks/datatooltk	:
	mkdir -p classes/com/dickimawbooks/datatooltk

clean	:
	\rm -rf classes/com

squeaky	:
	\rm -f lib/datatooltk.jar
	\rm -f lib/datatooltk-restricted.jar
	\rm -f lib/datatooltk-extra.jar
	\rm -f lib/datatooltklib.jar
	\rm -f lib/datatooltkguilib.jar
	make -C doc clean
