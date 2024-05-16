targetdir=../resources/helpsets
dictdir=../resources/dictionaries
targetbackref=../../doc
mainclass=../java/DatatoolTk.java
arara=arara --verbose

all	: en-GB en-US datatooltk.1

datatooltk.1	: datatooltk.pod
		pod2man -c datatooltk datatooltk.pod datatooltk.1

datatooltk-props-en.bib	: $(dictdir)/datatooltk-en.xml
	xml2bib --copy-overwrite-xml $(dictdir)/texparserlib-en.xml $(dictdir)/texjavahelplib-en.xml $(dictdir)/datatooltk-en.xml -o datatooltk-props-en.bib

datatooltk-props-en-GB.bib	: $(dictdir)/datatooltk-en.xml $(dictdir)/datatooltk-en-GB.xml
	xml2bib --copy-overwrite-xml  $(dictdir)/texparserlib-en.xml $(dictdir)/texjavahelplib-en.xml $(dictdir)/datatooltk-en.xml $(dictdir)/datatooltk-en-GB.xml -o datatooltk-props-en-GB.bib

version.tex	: $(mainclass)
	@echo "\\date{Version " > version.tex
	@grep 'String APP_VERSION = ' $(mainclass) | sed "s/public\sstatic\sfinal\sString\sAPP_VERSION\s=//" | tr -d "\"\; " >> version.tex
	@grep 'String APP_DATE = ' $(mainclass) | sed "s/public\sstatic\sfinal\sString\sAPP_DATE\s=//" | tr -d "\"\; " >> version.tex
	@echo "}" >> version.tex

datatooltk-en-GB.pdf	: datatooltk.bib datatooltk-props-en-GB.bib \
			 datatooltk-en-GB.tex datatooltk-main.tex version.tex
	$(arara) datatooltk-en-GB

datatooltk-en-US.pdf	: datatooltk.bib datatooltk-props-en.bib \
			  datatooltk-en-US.tex datatooltk-main.tex version.tex
	$(arara) datatooltk-en-US

en-GB	: $(targetdir)/datatooltk-en-GB/datatooltk.html \
	  datatooltk-en-GB.pdf

en-US	: $(targetdir)/datatooltk-en-US/datatooltk.html \
	  datatooltk-en-US.pdf

$(targetdir)/datatooltk-en-GB/datatooltk.html	: $(targetdir) \
	  datatooltk-en-GB.pdf datatooltk.dtd \
          ../tests/test-para.csv \
          ../tests/HelloWorld.java \
          ../tests/HelloUser.java \
          ../tests/test-probsoln-doc.tex \
          ../tests/prob-mixed.tex \
          ../tests/test-shuffle.tex \
          ../tests/test-shuffle-level.tex \
	  images/*.png \
	  $(mainclass) \
	  $(dictdir)/datatooltk-en-GB.prop \
	  $(targetdir)/datatooltk-en-GB/images/
	createdatatooltkdocs en GB
	cd $(targetdir)/datatooltk-en-GB; \
	  \rm -r -f JavaHelpSearch ; \
	  jh2indexer .

$(targetdir)/datatooltk-en-US/datatooltk.html	: $(targetdir) \
	  datatooltk-en.pdf datatooltk.dtd \
          ../tests/test-para.csv \
          ../tests/HelloWorld.java \
          ../tests/HelloUser.java \
          ../tests/test-probsoln-doc.tex \
          ../tests/prob-mixed.tex \
          ../tests/test-shuffle.tex \
          ../tests/test-shuffle-level.tex \
	  images/*.png \
	  $(mainclass) \
	  $(dictdir)/datatooltk-en-US.prop \
	  $(targetdir)/datatooltk-en-US/images/
	createdatatooltkdocs en US
	cd $(targetdir)/datatooltk-en-US; \
	  \rm -r -f JavaHelpSearch ; \
	  jh2indexer .

$(targetdir)	:
	mkdir $(targetdir)


$(targetdir)/datatooltk-en-GB/images/	: 
	mkdir -p $(targetdir)/datatooltk-en-GB
	cd $(targetdir)/datatooltk-en-GB/ ; \
	ln -s ../$(targetbackref)/images

$(targetdir)/datatooltk-en-US/images/	: 
	mkdir -p $(targetdir)/datatooltk-en-US
	cd $(targetdir)/datatooltk-en-US/ ; \
	ln -s ../$(targetbackref)/images

clean	:
	\rm -f datatooltk-en*.{aux,log,pdf,glg,glstex,out,toc,lof}

