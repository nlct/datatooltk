targetdir=../resources/helpsets
dictdir=../resources/dictionaries
targetbackref=../../doc
mainclass=../java/DatatoolTk.java
arara=arara --verbose
texjavahelpmk=texjavahelpmk
xml2bib=xml2bib --copy-overwrite-xml
testfiles=../tests/test-para.csv \
          ../tests/HelloWorld.java \
          ../tests/HelloUser.java \
          ../tests/test-probsoln-doc.tex \
          ../tests/prob-mixed.tex \
          ../tests/test-shuffle.tex \
          ../tests/test-shuffle-level.tex

all	: en-GB en-US datatooltk.1

datatooltk.1	: datatooltk.pod
		pod2man -c datatooltk datatooltk.pod datatooltk.1

datatooltk-props-en.bib	: $(dictdir)/datatooltk-en.xml
	$(xml2bib) $(dictdir)/texparserlib-en.xml $(dictdir)/texjavahelplib-en.xml $(dictdir)/datatooltk-en.xml -o datatooltk-props-en.bib

datatooltk-props-en-GB.bib	: $(dictdir)/datatooltk-en.xml $(dictdir)/datatooltk-en-GB.xml
	$(xml2bib)  $(dictdir)/texparserlib-en.xml $(dictdir)/texjavahelplib-en.xml $(dictdir)/datatooltk-en.xml $(dictdir)/datatooltk-en-GB.xml -o datatooltk-props-en-GB.bib

version.tex	: $(mainclass)
	@echo "\\date{Version " > version.tex
	@grep 'String APP_VERSION = ' $(mainclass) | sed "s/public\sstatic\sfinal\sString\sAPP_VERSION\s=//" | tr -d "\"\; " >> version.tex
	@grep 'String APP_DATE = ' $(mainclass) | sed "s/public\sstatic\sfinal\sString\sAPP_DATE\s=//" | tr -d "\"\; " >> version.tex
	@echo "}" >> version.tex

datatooltk-en-GB.pdf	: datatooltk.bib datatooltk-props-en-GB.bib \
			 datatooltk-en-GB.tex datatooltk-main.tex version.tex \
			$(testfiles) images/*.png
	$(arara) datatooltk-en-GB

datatooltk-en-US.pdf	: datatooltk.bib datatooltk-props-en.bib \
			  datatooltk-en-US.tex datatooltk-main.tex version.tex \
			$(testfiles) images/*.png
	$(arara) datatooltk-en-US

en-GB	: $(targetdir)/datatooltk-en-GB/index.xml

en-US	: $(targetdir)/datatooltk-en-US/index.xml

$(targetdir)/datatooltk-en-GB/index.xml	: $(targetdir) \
	  datatooltk-en-GB.pdf  
	$(texjavahelpmk) datatooltk-en-GB.tex $(targetdir)/datatooltk-en-GB

$(targetdir)/datatooltk-en-US/index.xml	: $(targetdir) \
	  datatooltk-en-US.pdf  
	$(texjavahelpmk) datatooltk-en-US.tex $(targetdir)/datatooltk-en-US


$(targetdir)	:
	mkdir $(targetdir)

clean	:
	\rm -f datatooltk-en*.{aux,log,pdf,glg,glstex,out,toc,lof}

