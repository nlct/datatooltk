deps:=../lib/datatooltk.jar
arara:=arara --verbose

all	: test-booklist-tsv.pdf

test-booklist-tsv.pdf	: test-booklist-tsv.tex booklist.tsv $(deps)
	$(arara) test-booklist-tsv

clean	:
	\rm -f *.aux *.log test*.pdf
