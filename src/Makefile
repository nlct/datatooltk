# Java 8 extended support until December 2030
target_version := --release 8 -Xlint:-options

# This assumes TeX Java Parser is installed in TEXMF/scripts/texjavaparser/
texjavaparserlib=$(shell kpsewhich --format=texmfscripts texjavaparserlib.jar)

ifeq ($(texjavaparserlib),)
# Not found, use environment variable instead which will need to be
# defined
  texjavaparserlib = $(TEXJAVAPARSERLIBDIR)/texjavaparserlib.jar
endif

# This assumes TeX Java Help is installed in TEXMF/scripts/texjavahelp/
texjavahelplib=$(shell kpsewhich --format=texmfscripts texjavahelplib.jar)

ifeq ($(texjavahelplib),)
# Not found, use environment variable instead which will need to be
# defined
  texjavahelplib = $(TEXJAVAHELPLIBDIR)/texjavahelplib.jar
endif

app_version:=$(shell grep "public static final String APP_VERSION" java/lib/DatatoolTk.java | sed -E  "s/.*\"(.*)\";/\1/")

app_date:=$(shell grep "public static final String APP_DATE" java/lib/DatatoolTk.java | sed -E  "s/.*\"(.*)\";/\1/")

ifeq ($(IZPACKDIR),)
IZPACKDIR:=/usr/local/IzPack
endif
POIVER=5.3.0

arara = arara --verbose
tjhxml2bib=tjhxml2bib --copy-overwrite-xml
tjhziphelpset=tjhziphelpset
texjavahelpmk=texjavahelpmk #--debug-mode sty-data --log texparser.log

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
	cp doc/en/datatooltk-en.pdf ../unstable
	cp doc/en/datatooltk.1 ../unstable
	sed "s/APPVERSION/$(app_version)/g" README-dist.md > ../unstable/README.md

doc/LICENSE	:
	cd doc; ln -s ../../LICENSE

# For complete distribution (not for TeX Live):

lib/texjavaparserlib.jar	: $(texjavaparserlib)
	cp $(texjavaparserlib) lib/texjavaparserlib

lib/texjavahelplib.jar	: $(texjavahelplib)
	cp $(texjavahelplib) lib/texjavahelplib

../unstable/datatooltk-installer.jar	: lib/datatooltk-restricted.jar lib/datatooltk.jar lib/datatooltk-extra.jar lib/texjavahelplib.jar lib/texjavaparserlib.jar installer/install.xml
	"$(IZPACKDIR)/bin/compile" installer/install.xml -b . \
	-h "$(IZPACKDIR)" \
	-o ../unstable/datatooltk-installer.jar -k standard

../unstable/datatooltk-all.zip	: lib/datatooltk-extra.jar lib/datatooltk-restricted.jar lib/datatooltk.jar
	mkdir -p ../unstable/datatooltk
	mkdir -p ../unstable/datatooltk/doc
	rsync -rLKptgov --exclude=".*" lib ../unstable/datatooltk
	rsync -rLKptgov --exclude=".*" bin ../unstable/datatooltk
	cp doc/*/*.{1,pdf} ../unstable/datatooltk/doc
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
	datatooltk/lib/texjavaparserlib.jar \
	datatooltk/lib/texjavahelplib.jar \
	datatooltk/lib/datatooltklib.jar \
	datatooltk/lib/datatooltk-restricted.jar 

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
	datatooltk/lib/texjavaparserlib.jar \
	datatooltk/lib/texjavahelplib.jar \
	datatooltk/lib/datatooltklib.jar \
	datatooltk/lib/datatooltkguilib.jar \
	datatooltk/lib/datatooltk-restricted.jar \
	datatooltk/lib/datatooltk.jar \
	datatooltk/lib/mysql-connector-java.jar \
	datatooltk/lib/datatooltk-splash.png \
	datatooltk/lib/datatooltk-icons/ \
	datatooltk/lib/datatooltk-plugins/ \
	datatooltk/lib/datatooltk-templates/ 

../unstable/datatooltk-extra.zip	: ../unstable/datatooltk-all.zip
	cd ../unstable; \
	zip datatooltk-extra.zip -r  \
	datatooltk/README.md \
	datatooltk/CHANGES \
	datatooltk/bin/datatooltk-restricted* \
	datatooltk/bin/datatooltk-extra* \
	datatooltk/doc/*.{1,pdf} \
	datatooltk/lib/texjavaparserlib.jar \
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
	datatooltk/lib/datatooltk-splash.png \
	datatooltk/lib/datatooltk-icons/ \
	datatooltk/lib/datatooltk-plugins/ \
	datatooltk/lib/datatooltk-templates/ 

lib/datatooltk-helpset.tjh	: doc/gpl-3.0-standalone.html \
	lib/helpset/en/index.xml \
	lib/helpset/en-GB/index.xml
	$(tjhziphelpset) \
	--license-file doc/gpl-3.0-standalone.html '' \
	--locales 'en,en-GB' \
	lib \
	--output lib/datatooltk-helpset.tjh

doc/en/datatooltk-en.pdf	: doc/en/datatooltk-dict-en.bib \
	doc/en/datatooltk-en.tex \
	doc/en/datatooltk-main.tex \
	doc/en/datatooltk.bib \
	doc/version.tex
	$(arara) --working-directory doc/en datatooltk-en

doc/en/datatooltk-en-GB.pdf	: doc/en/datatooltk-dict-en-GB.bib \
	doc/en/datatooltk-en-GB.tex \
	doc/en/datatooltk-main.tex \
	doc/en/datatooltk.bib \
	doc/version.tex
	$(arara) --working-directory doc/en datatooltk-en-GB

lib/helpset/en-GB/index.xml	: doc/en/datatooltk-en-GB.pdf
	$(texjavahelpmk) doc/en/datatooltk-en-GB.tex lib/helpset/en-GB
	@grep -E 'Package glossaries(-extra)? Warning' doc/en/datatooltk-en-GB.log || true

lib/helpset/en/index.xml	: doc/en/datatooltk-en.pdf
	$(texjavahelpmk) doc/en/datatooltk-en.tex lib/helpset/en
	@grep -E 'Package glossaries(-extra)? Warning' doc/en/datatooltk-en.log || true

doc/version.tex	:
	echo "\\versiondate{$(app_version)}{$(app_date)}" > doc/version.tex

doc/en/datatooltk-dict-en.bib	: dictionaries/datatooltk-en.xml
	$(tjhxml2bib) \
	--resource /com/dickimawbooks/texparserlib/dictionaries/texjavaparserlib-en.xml \
	--resource /com/dickimawbooks/texjavahelplib/dictionaries/texjavahelplib-en.xml \
	--prop dictionaries/plugins-en.prop \
	dictionaries/datatooltk-en.xml \
	-o doc/en/datatooltk-dict-en.bib

doc/en/datatooltk-dict-en-GB.bib	: dictionaries/datatooltk-en.xml \
	dictionaries/datatooltk-en-GB.xml
	$(tjhxml2bib) \
	--resource /com/dickimawbooks/texparserlib/dictionaries/texjavaparserlib-en.xml \
	--resource /com/dickimawbooks/texjavahelplib/dictionaries/texjavahelplib-en.xml \
	--prop dictionaries/plugins-en.prop \
	dictionaries/datatooltk-en.xml \
	dictionaries/datatooltk-en-GB.xml \
	-o doc/en/datatooltk-dict-en-GB.bib

lib/datatooltklib.jar	: classes/com/dickimawbooks/datatooltk/DatatoolTk.class \
	classes/com/dickimawbooks/datatooltk/dictionaries/ \
	dictionaries/datatooltk-en.xml \
	dictionaries/datatooltk-en-GB.xml 
	cp dictionaries/*.xml classes/com/dickimawbooks/datatooltk/dictionaries/
	cd classes; \
	jar cf ../lib/datatooltklib.jar \
	com/dickimawbooks/datatooltk/*.class \
	com/dickimawbooks/datatooltk/io/*.class \
	com/dickimawbooks/datatooltk/dictionaries/ \
	com/dickimawbooks/datatooltk/dictionaries/*.xml

classes/com/dickimawbooks/datatooltk/dictionaries/	:
	mkdir -p classes/com/dickimawbooks/datatooltk/dictionaries

classes/com/dickimawbooks/datatooltk/DatatoolTk.class	: classes/com/dickimawbooks/datatooltk \
	java/lib/*.java java/lib/io/*.java 
	cd java/lib; \
	javac $(target_version) -d ../../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	-cp $(texjavahelplib):$(texjavaparserlib) \
	*.java io/*.java

lib/datatooltkguilib.jar	: classes/com/dickimawbooks/datatooltk/gui/DatatoolGUI.class \
	java/gui/icons/*.png \
	lib/datatooltk-helpset.tjh
	cp -u -r java/gui/icons classes/com/dickimawbooks/datatooltk/gui/
	cd classes; \
	jar cf ../lib/datatooltkguilib.jar \
	com/dickimawbooks/datatooltk/gui/*.class \
	com/dickimawbooks/datatooltk/gui/icons/*.png 

classes/com/dickimawbooks/datatooltk/gui/DatatoolGUI.class	: java/gui/*.java \
	lib/datatooltklib.jar 
	cd java/gui; \
	javac $(target_version) -d ../../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	-cp ../../lib/datatooltklib.jar:$(texjavahelplib):$(texjavaparserlib) \
	*.java

lib/datatooltk.jar	: lib/datatooltkguilib.jar java/basic/Manifest.txt \
			classes/com/dickimawbooks/datatooltk/basic/BasicDatatoolTk.class 
	cd classes; \
	jar cmf ../java/basic/Manifest.txt ../lib/datatooltk.jar \
	com/dickimawbooks/datatooltk/basic/*.class 

classes/com/dickimawbooks/datatooltk/basic/BasicDatatoolTk.class	: classes/com/dickimawbooks/datatooltk \
	java/basic/*.java \
	lib/datatooltkguilib.jar 
	cd java; \
	javac $(target_version) -d ../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	-cp ../lib/datatooltklib.jar:../lib/datatooltkguilib.jar:$(texjavahelplib):$(texjavaparserlib) \
	basic/*.java 

lib/datatooltk-extra.jar	: lib/datatooltkguilib.jar java/extra/Manifest.txt \
			classes/com/dickimawbooks/datatooltk/extra/ExtraDatatoolTk.class 
	cd classes; \
	jar cmf ../java/extra/Manifest.txt ../lib/datatooltk-extra.jar \
	com/dickimawbooks/datatooltk/extra/*.class 

classes/com/dickimawbooks/datatooltk/extra/ExtraDatatoolTk.class	: classes/com/dickimawbooks/datatooltk \
	java/extra/*.java \
	lib/datatooltkguilib.jar \
	lib/mysql-connector-java.jar \
	lib/poi-$(POIVER).jar lib/poi-ooxml-$(POIVER).jar \
	lib/poi-ooxml-full-$(POIVER).jar \
	lib/apache-commons-io.jar \
	lib/log4j-api.jar lib/log4j-core.jar 
	cd java; \
	javac $(target_version) -d ../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	-cp ../lib/datatooltklib.jar:../lib/datatooltkguilib.jar:$(texjavahelplib):../lib/mysql-connector-java.jar:../lib/poi-$(POIVER).jar:../lib/poi-ooxml-$(POIVER).jar:../lib/poi-ooxml-full-$(POIVER).jar:../lib/apache-commons-io.jar:../lib/log4j-api.jar:../lib/log4j-core.jar:$(texjavaparserlib) \
	extra/*.java

lib/datatooltk-restricted.jar	: lib/datatooltklib.jar java/restricted/Manifest.txt \
			classes/com/dickimawbooks/datatooltk/restricted/RestrictedDatatoolTk.class 
	cd classes; \
	jar cmf ../java/restricted/Manifest.txt ../lib/datatooltk-restricted.jar \
	com/dickimawbooks/datatooltk/restricted/*.class 

classes/com/dickimawbooks/datatooltk/restricted/RestrictedDatatoolTk.class	: classes/com/dickimawbooks/datatooltk \
	java/restricted/*.java \
	lib/datatooltklib.jar 
	cd java; \
	javac $(target_version) -d ../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	-cp ../lib/datatooltklib.jar:$(texjavahelplib):$(texjavaparserlib) \
	restricted/*.java

classes/com/dickimawbooks/datatooltk	:
	mkdir -p classes/com/dickimawbooks/datatooltk

clean	:
	\rm -rf classes/com
	\rm -f doc/en/datatooltk-dict-en.bib
	\rm -f doc/en/datatooltk-dict-en-GB.bib
	\rm -rf lib/helpset

squeaky	:
	\rm -f lib/datatooltk.jar
	\rm -f lib/datatooltk-restricted.jar
	\rm -f lib/datatooltk-extra.jar
	\rm -f lib/datatooltklib.jar
	\rm -f lib/datatooltkguilib.jar
	\rm -f lib/datatooltk-helpset.tjh
	make -C doc clean
