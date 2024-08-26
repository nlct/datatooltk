# datatooltk
Java application for use with the datatool LaTeX package.

The DatatoolTk Java application can be used to create
[datatool.sty](https://ctan.org/pkg/datatool) DBTEX files, 
which can be quickly imported into a LaTeX document using 
`\DTLread` (for `datatool.sty` v3.0+ or `\input` for older 
versions). The output format may also be set to DTLTEX
(which contains user-level LaTeX commands rather than special
internals).  If your version of `datatool.sty`
is older than 3.0, be sure to set the format to `dbtex-2` or
`dtltex-2` (`--output-format` switch in batch mode or the
appropriate file filter selector in GUI mode).

For example, the file `booklist.csv` contains:
```csv
Title,Author,Format,Quantity,Price
The Adventures of Duck and Goose,Sir Quackalot,paperback,3,$10.99
The Return of Duck and Goose,Sir Quackalot,paperback,5,$19.99
More Fun with Duck and Goose,Sir Quackalot,paperback,1,$12.99
Duck and Goose on Holiday,Sir Quackalot,paperback,3,$11.99
The Return of Duck and Goose,Sir Quackalot,hardback,3,$19.99
The Adventures of Duck and Goose,Sir Quackalot,hardback,9,$18.99
My Friend is a Duck,A. Parrot,paperback,20,$14.99
Annotated Notes on the ‘Duck and Goose’ chronicles,Prof Macaw,ebook,10,$8.99
‘Duck and Goose’ Cheat Sheet for Students,Polly Parrot,ebook,50,$5.99
‘Duck and Goose’: an allegory for modern times?,Bor Ing,hardback,0,$59.99
Oh No! The Chickens have Escaped!,Dickie Duck,ebook,11,$2.00
```
The LaTeX document (`myDoc.tex`) contains:
```latex
\documentclass{article}
\usepackage[T1]{fontenc}
\usepackage{datatool}% v3.0 required
\DTLsetup{default-name=booklist}

\DTLread[format=dbtex]{booklist-converted}
\begin{document}
\DTLaction{display}
\end{document}
```
The document build:
```bash
datatooltk --output-format dbtex-3 --output booklist-converted.dbtex --sort-locale en --sort 'Author,Title' --literal --csv-sep ',' --csv booklist.csv
pdflatex myDoc
```
The `--literal` switch indicates that the CSV file contains literal
(that is, non-TeX) content, so the `$` symbol will be mapped to `\$`.

Note that `\DTLread` does support CSV and data can be sorted in the document, 
so DatatoolTk isn't required in the following:
```latex
\documentclass{article}

\usepackage[T1]{fontenc}
\usepackage{datatool}% v3.0 required
\DTLsetup{default-name=booklist}

\DTLread[format=csv,csv-content=literal]{booklist.csv}

\begin{document}
\DTLaction[assign={Author,Title}]{sort}
\DTLaction{display}
\end{document}
```
However, extra support needs to be added for localised sorting with
`datatool.sty`, Java can sort and parse formatted numbers more efficiently than TeX,
and DatatoolTk can also import data from other sources that aren't 
supported by `\DTLread`, as well as shuffling and filtering the data.

In the first example, the DatatoolTk call is only needed when the 
source data (`booklist.csv`) changes. In the second example, the data is sorted on 
every LaTeX run.

## Provided Applications

The following applications are available (as from version 2.0):

 - [Basic DatatoolTk](#basic-datatooltk)
 - [DatatoolTk Restricted](#datatooltk-restricted)
 - [DatatoolTk Extra](#datatooltk-extra)

Go to the [Releases page](https://github.com/nlct/datatooltk/releases) for the latest stable release or
the [`dist` directory](https://github.com/nlct/datatooltk/tree/master/dist)
for the most recent development (unstable) version.

### Basic DatatoolTk

The basic application (`datatooltk.jar`) can be run in batch
mode or with a graphical user interface (GUI). That is, it can
either be run from the command line with an input and output
file, or it can open a window with menus and buttons.
Preferred settings (which can be changed in GUI mode) are saved
in a properties directory and picked up on the
next run (`~/.datatooltk` on Unix-like systems
or `datatooltk-settings` for Windows or the path identified by the
environment variable `DATATOOLTK`).

The basic application supports importing data from TeX files
(that contain `datatool.sty` database construction commands or
[`probsoln.sty`](https://ctan.org/pkg/probsoln) problem definitions),
CSV/TSV files,
ODS (OpenDocument Spreadsheet), FODS (Flat OpenDocument Spreadsheet),
XLSX (XML Excel Spreadsheet), and SQL. For database connections (SQL), 
a driver is required.  For example, to provide support for MySQL, the
`mysql-connector-java.jar` library needs to be on the class path.
(Support for MySQL is automatically included.
For other database systems, you will need to add the applicable JDBC driver to
the Java class path.)

```bash
datatooltk [options]
```
The `datatooltk` script simply runs the `datatooltk.jar`
application. You can run it directly using the following
command line invocation, where `path/to/lib` should be
changed to the path to the `lib` directory:
```bash
java -jar path/to/lib/datatooltk.jar [options]
```

If run in batch mode (the default) the options must include an input
file or input source and an output file. To run in GUI mode, the
options list should include `--gui`.

```bash
datatooltk-gui [options]
```
The `datatooltk-gui` script simply runs the `datatooltk.jar`
application with `--gui` and a splash image:
```bash
java -splash:path/to/lib/datatooltk-splash.png -jar path/to/lib/datatooltk.jar --gui [options]
```

### DatatoolTk Restricted

The restricted application (`datatooltk-restricted.jar`)
is a version of the DatatoolTk application that only runs
in batch mode. If you attempt to run this application in GUI mode
it will trigger an "Unsupported option" error. The restricted
application doesn't support SQL connections, but otherwise
supports the import file formats that the basic DatatoolTk
application supports (TeX, CSV/TSV, ODS, FODS and XLSX).

Again, this comes with a helper script to run the Java
application:
```bash
datatooltk-restricted [options]
```

Alternatively:
```bash
java -jar path/to/lib/datatooltk-restricted.jar [options]
```

### DatatoolTk Extra

The DatatoolTk Extra application (`datatooltk-extra.jar`)
has all the features of the basic DatatoolTk application but additionally
supports Microsoft's old binary Excel (`.xls`) file format.
This requires the third party Apache POI library.

Again, this comes with helper scripts to run the Java
application:
```bash
datatooltk-extra [options]
```

Alternatively:
```bash
java -jar path/to/lib/datatooltk-extra.jar [options]
```

Or for GUI mode with a splash screen:
```bash
datatooltk-extra-gui [options]
```

Alternatively:
```bash
java -splash:path/to/lib/datatooltk-splash.png -jar path/to/lib/datatooltk-extra.jar --gui [options]
```

## Requirements

Requires at least Java 8 and the following libraries:

 - [TeX Parser Library](https://github.com/nlct/texparser)
   (GPL-3.0)
   to parse LaTeX files: the file `texparserlib.jar`
   should be placed in the `lib` directory. **This library is
   required by all DatatoolTk applications.**

 - [TeX Java Help](https://github.com/nlct/texjavahelp) (GPL-3.0)
   to convert the LaTeX manual with `texjavahelpmk`
   to the HTML and XML files required by `texjavahelplib.jar`
   which manages the application help system and localised messages:
   the file `texjavahelplib.jar` should be placed in the `lib` directory.
   **This library is required by all DatatoolTk applications.**
   (The restricted application only uses the library for localised
   messages. The other applications additionally use it for
   the in-application help in GUI mode.)

   (The `texjavahelpmk.jar` and `texjavahelp.sty` files are only required 
   when compiling the manual to PDF or HTML. The PDF file needs to be created first
   to ensure that the `toc`, `aux` and `glstex` files are available
   when `texjavahelpmk.jar` parses the document source. Only the
   `texjavahelplib.jar` file needs to be included in the
   distribution.)

 - [MySQL Connector/J](https://dev.mysql.com/downloads/connector/j/)
   (GPL-2.0) 
   to pull data from MySQL databases: the file `mysql-connector-java.jar`
   should be placed in the `lib` directory.
   **This library is required for the basic DatatoolTk application
   and the DatatoolTK Extra application but not for the restricted
   application which doesn't support SQL imports.**

 - [Apache POI](https://poi.apache.org/) (Apache License, Version 2.0)
    to read Microsoft Excel spreadsheets (xls): the files `poi-`_version_`.jar`, 
   `poi-ooxml-`_version_`.jar` and `poi-ooxml-schema-`_version_`.jar` 
   should be placed in the `lib` directory. This library in turn
   requires Apache Commons IO, which requires Log4j, so those
   jar files are also required.
   **This library and its dependencies are only required for the 
   DatatoolTK Extra application.**
   
If any plugins are needed for the applications that have a GUI mode, 
Perl is also required.

## Source Code

The DatatoolTk source code is available at https://github.com/nlct/datatooltk

The TeX Java Help source code is available at
https://github.com/nlct/texjavahelp
This not only provides `texjavahelplib.jar` needed by DatatoolTk to 
manage the localisation messages and (if applicable) the GUI help
window, but also provides the tools needed to create the manual (see
below).

The TeX Parser Library source code is available at
https://github.com/nlct/texparser
This not only provides `texparserlib.jar` needed by DatatoolTk to
parse TeX files, but is also required by the TeX Java Help tools
used to create the manual. (Note that `texparserlib.jar` is also
bundled with `bib2gls`, but it may be a different version. You can
check the version with `bib2gls -v`)

The HTML files for the help window in GUI mode need to be created from the
dictionary XML files and the LaTeX source.

 1. Change to the source code `doc` directory:
    ```bash
   cd doc
    ```
 2. Convert the application dictionary files (`.xml` and `.prop`) to 
    `datatooltk-props-en.bib` (a `bib2gls` file):
    ```bash
   xml2bib --copy-overwrite-xml \
   ../lib/resources/dictionaries/texparserlib-en.xml \
   ../lib/resources/dictionaries/texjavahelplib-en.xml \
   ../lib/resources/dictionaries/datatooltk-en.xml \
   --prop ../lib/resources/dictionaries/plugins-en.prop \
   -o datatooltk-props-en.bib
    ```
   (`xml2bib` is provided by TeX Java Help.)
 3. (Optional) Create the file `version.tex` which should simply
    contain `\date{Version `_version_`}`. For example:
    ```bash
   echo "\\date{Version " > version.tex
   grep 'String APP_VERSION = ' ../java/base/DatatoolTk.java | sed "s/public\sstatic\sfinal\sString\sAPP_VERSION\s=//" | tr -d "\"\; " >> version.tex
   grep 'String APP_DATE = ' .../java/base/DatatoolTk.java | sed "s/public\sstatic\sfinal\sString\sAPP_DATE\s=//" | tr -d "\"\; " >> version.tex
   echo "}" >> version.tex
    ```
   This file is input with `\InputIfFileExists` so it can be omitted.
 4. Build the document (this requires `texjavahelp.sty` provided
    by TeX Java Help). Either use Arara:
    ```bash
   arara datatooltk-en
    ```
   Or:
    ```bash
   lualatex datatooltk-en
   bib2gls --group --no-warn-unknown-entry-types datatooltk-en
   lualatex datatooltk-en
   lualatex datatooltk-en
    ```
 5. Create the XML and HTML files needed for the GUI help window:
    ```bash
   texjavahelpmk datatooltk-en.tex ../lib/resources/helpsets/datatooltk-en
    ```
   (`texjavahelpmk` is provided by TeX Java Help.)
   Note that it's necessary to successfully perform step 4 before
   trying step 5 as `texjavahelpmk` needs the `toc`, `aux` and
   `glstex` files created by LaTeX and `bib2gls`.

The source code for `datatooltklib.jar` is in the `java/base` directory
(including the files in `java/base/io`).

The source code for `datatooltkguilib.jar` is in the `java/gui` directory
(which includes widget icons in `java/gui/icons`). Note that
`texjavahelplib.jar` includes additional widget icons (for the help
window and also a few common icons).

The source code for `datatooltk-restricted.jar` is in the
`java/restricted` directory. This will need `datatooltklib.jar`,
`texjavahelplib.jar` and `texparserlib.jar` on the
class path.

The source code for `datatooltk.jar` is in the 
`java/combined` directory. This will need the same libraries 
as `datatooltk-restricted.jar` but will also require
`datatooltkguilib.jar` on the class path.

The source code for `datatooltk-extra.jar` is in the `java/extra`
directory. This will need the same libraries
as `datatooltk.jar` but will also require the Apache POI, Apache
Commons and Log4j API libraries on the class path.
---

Nicola Talbot [dickimaw-books.com](https://www.dickimaw-books.com/)

License GPLv3+: GNU GPL version 3 or later
http://gnu.org/licenses/gpl.html
This is free software: you are free to change and redistribute it.
There is NO WARRANTY, to the extent permitted by law.
