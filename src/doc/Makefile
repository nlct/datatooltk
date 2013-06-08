TARGETDIR=../resources/helpsets
DICTDIR=../resources/dictionaries
TARGETBACKREF=../../doc
MAINCLASS=../java/DatatoolTk.java

all	: en-GB en-US

en-GB	: $(TARGETDIR)/helpset-en-GB/datatooltk.html \
	  datatooltk-en-GB.pdf

en-US	: $(TARGETDIR)/helpset-en-US/datatooltk.html \
	  datatooltk-en-US.pdf

$(TARGETDIR)/helpset-en-GB/datatooltk.html	: $(TARGETDIR) \
	  createdocs \
	  datatooltk-en.xml datatooltk.dtd \
	  $(MAINCLASS) \
	  $(DICTDIR)/datatooltk-en-GB.prop \
	  $(TARGETDIR)/helpset-en-GB/images/
	createdocs en GB

$(TARGETDIR)/helpset-en-US/datatooltk.html	: $(TARGETDIR) \
	  createdocs \
	  datatooltk-en.xml datatooltk.dtd \
	  $(MAINCLASS) \
	  $(DICTDIR)/datatooltk-en-US.prop \
	  $(TARGETDIR)/helpset-en-US/images/
	createdocs en US

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

$(TARGETDIR)/helpset-en-GB/images/	: 
	mkdir -p $(TARGETDIR)/helpset-en-GB
	cd $(TARGETDIR)/helpset-en-GB/ ; \
	ln -s ../$(TARGETBACKREF)/images

$(TARGETDIR)/helpset-en-US/images/	: 
	mkdir -p $(TARGETDIR)/helpset-en-US
	cd $(TARGETDIR)/helpset-en-US/ ; \
	ln -s ../$(TARGETBACKREF)/images

clean	:
	\rm -f datatooltk-en-GB.{tex,aux,log,pdf,glg,glo,gls,ist,ilg,ind,idx,acr,acn,alg}
	\rm -f datatooltk-en-US.{tex,aux,log,pdf,glg,glo,gls,ist,ilg,ind,idx,acr,acn,alg}

