deps:=../../lib/datatooltk.jar

all	: booklist-tsv-doc.pdf booklist-csv-doc.pdf

booklist-tsv-doc.pdf	: booklist-tsv-converted.dbtex booklist-tsv-doc.tex
	pdflatex booklist-tsv-doc

booklist-tsv-converted.dbtex	: ../booklist.tsv $(deps)
	datatooltk --output booklist-tsv-converted.dbtex --output-format dbtex-3 \
	--sort Title --csv-literal --tsv ../booklist.tsv \
	--keys "Title,Author,Format,Quantity,Price"

booklist-csv-doc.pdf	: booklist-csv-converted.dbtex booklist-csv-doc.tex
	pdflatex booklist-csv-doc

booklist-csv-converted.dbtex	: ../booklist.csv $(deps)
	datatooltk --output booklist-csv-converted.dbtex --output-format dbtex-3 \
	--sort Author --csv-literal --csv ../booklist.csv --csv-sep "," \
	--csv-empty-rows end

clean	:
	\rm -f *-converted.dbtex
	\rm -f *.{aux,log}
