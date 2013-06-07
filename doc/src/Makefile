DICTDIR=../../resources/dictionaries
MAINCLASS=../../src/DatatoolTk.java

all	: en-GB en-US

en-GB	: $(DICTDIR)/helpset-en-GB/datatooltk.html \
	  ../datatooltk-en-GB.pdf

en-US	: $(DICTDIR)/helpset-en-US/datatooltk.html \
	  ../datatooltk-en-US.pdf

$(DICTDIR)/helpset-en-GB/datatooltk.html	: createdocs datatooltk-en.xml \
	  $(MAINCLASS) \
	  $(DICTDIR)/datatooltk-en-GB.prop
	createdocs en GB

$(DICTDIR)/helpset-en-US/datatooltk.html	: createdocs datatooltk-en.xml \
	  $(MAINCLASS) \
	  $(DICTDIR)/datatooltk-en-US.prop
	createdocs en US

../datatooltk-en-GB.pdf	: datatooltk-en-GB.tex
	arara datatooltk-en-GB
	mv datatooltk-en-GB.pdf ..

../datatooltk-en-US.pdf	: datatooltk-en-US.tex
	arara datatooltk-en-US
	mv datatooltk-en-US.pdf ..

datatooltk-en-GB.tex	: createdocs datatooltk-en.xml \
	  $(MAINCLASS) \
	  $(DICTDIR)/datatooltk-en-GB.prop
	createdocs en GB

datatooltk-en-US.tex	: createdocs datatooltk-en.xml \
	  $(MAINCLASS) \
	  $(DICTDIR)/datatooltk-en-US.prop
	createdocs en US

clean	:
	\rm -f datatooltk-en-GB.{tex,aux,log,pdf,glg,glo,gls,ist,ilg,ind,idx,acr,acn,alg}
	\rm -f datatooltk-en-US.{tex,aux,log,pdf,glg,glo,gls,ist,ilg,ind,idx,acr,acn,alg}

