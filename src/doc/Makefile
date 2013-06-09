TARGETDIR=../resources/helpsets
DICTDIR=../resources/dictionaries
TARGETBACKREF=../../doc
MAINCLASS=../java/DatatoolTk.java

all	: en-GB en-US

en-GB	: $(TARGETDIR)/datatooltk-en-GB/datatooltk.html \
	  datatooltk-en-GB.pdf

en-US	: $(TARGETDIR)/datatooltk-en-US/datatooltk.html \
	  datatooltk-en-US.pdf

$(TARGETDIR)/datatooltk-en-GB/datatooltk.html	: $(TARGETDIR) \
	  createdocs \
	  datatooltk-en.xml manual.dtd \
	  $(MAINCLASS) \
	  $(DICTDIR)/datatooltk-en-GB.prop \
	  $(TARGETDIR)/datatooltk-en-GB/images/
	createdocs en GB
	cd $(TARGETDIR)/datatooltk-en-GB; \
	  \rm -r -f JavaHelpSearch ; \
	  jh2indexer .

$(TARGETDIR)/datatooltk-en-US/datatooltk.html	: $(TARGETDIR) \
	  createdocs \
	  datatooltk-en.xml manual.dtd \
	  $(MAINCLASS) \
	  $(DICTDIR)/datatooltk-en-US.prop \
	  $(TARGETDIR)/datatooltk-en-US/images/
	createdocs en US
	cd $(TARGETDIR)/datatooltk-en-US; \
	  \rm -r -f JavaHelpSearch ; \
	  jh2indexer .

$(TARGETDIR)	:
	mkdir $(TARGETDIR)

datatooltk-en-GB.pdf	: datatooltk-en-GB.tex
	arara datatooltk-en-GB

datatooltk-en-US.pdf	: datatooltk-en-US.tex
	arara datatooltk-en-US

datatooltk-en-GB.tex	: createdocs datatooltk-en.xml \
	  $(MAINCLASS) \
	  $(DICTDIR)/datatooltk-en-GB.prop
	createdocs en GB

datatooltk-en-US.tex	: createdocs datatooltk-en.xml \
	  $(MAINCLASS) \
	  $(DICTDIR)/datatooltk-en-US.prop
	createdocs en US

$(TARGETDIR)/datatooltk-en-GB/images/	: 
	mkdir -p $(TARGETDIR)/datatooltk-en-GB
	cd $(TARGETDIR)/datatooltk-en-GB/ ; \
	ln -s ../$(TARGETBACKREF)/images

$(TARGETDIR)/datatooltk-en-US/images/	: 
	mkdir -p $(TARGETDIR)/datatooltk-en-US
	cd $(TARGETDIR)/datatooltk-en-US/ ; \
	ln -s ../$(TARGETBACKREF)/images

clean	:
	\rm -f datatooltk-en-GB.{tex,aux,log,pdf,glg,glo,gls,ist,ilg,ind,idx,acr,acn,alg}
	\rm -f datatooltk-en-US.{tex,aux,log,pdf,glg,glo,gls,ist,ilg,ind,idx,acr,acn,alg}

