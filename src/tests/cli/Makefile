deps:=../../lib/datatooltk.jar

all	: booklist-tsv-doc.pdf

booklist-tsv-doc.pdf	: booklist-tsv-converted.dbtex booklist-tsv-doc.tex
	pdflatex booklist-tsv-doc

booklist-tsv-converted.dbtex	: ../booklist.tsv $(deps)
	datatooltk --output booklist-tsv-converted.dbtex --output-format dbtex-3 \
	--sort Title --csv-literal --tsv ../booklist.tsv \
	--keys "Title,Author,Format,Quantity,Price"

clean	:
	\rm -f *-converted.dbtex
	\rm -f *.{aux,log}
