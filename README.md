# datatooltk
Java application for use with the datatool LaTeX package.

The datatooltk Java application that can be used to create
[datatool.sty](https://ctan.org/pkg/datatool) databases in
datatool's internal format, which can be quickly imported into a
LaTeX document using `\input`. It can be used in GUI or batch mode to
convert from SQL or CSV or import [probsoln](https://ctan.org/pkg/probsoln) datasets.

## Requirements

Requires at least Java 8 and the following libraries:

 - [jOpenDocument](https://jopendocument.org/) (GPL) to read OpenDocument
   spreadsheets (ods): the file `jOpenDocument-`_version_`.jar`
   should be placed in the `lib` directory;
 - [Apache POI](https://poi.apache.org/) (Apache License, Version 2.0)
    to read Microsoft Excel spreadsheets (xls): the files `poi-`_version_`.jar`, 
   `poi-ooxml-`_version_`.jar` and `poi-ooxml-schema-`_version_`.jar` 
   should be placed in the `lib` directory;
 - [mysql-connector-java.jar](https://dev.mysql.com/downloads/connector/j/)
   (GPL-2.0-only) 
   to pull data from MySQL databases: the file `mysql-connector-java.jar`
   should be placed in the `lib` directory;
 - [TeX Parser Library](https://github.com/nlct/texparser)
   (GPL-3.0)
   to parse LaTeX files: the file `texparserlib.jar`
   should be placed in the `lib` directory;
 - [TeX Java Help](https://github.com/nlct/texjavahelp) (GPL-3.0)
   to convert the LaTeX manual with `texjavahelpmk`
   to the HTML and XML files required by `texjavahelplib.jar`
   which manages the application help system: the file `texjavahelplib.jar`
   should be placed in the `lib` directory. (The `texjavahelpmk.jar`
   and `texjavahelp.sty` files are only required when compiling the
   manual to PDF or HTML. The PDF file needs to be created first
   to ensure that the `toc`, `aux` and `glstex` files are available
   when `texjavahelpmk.jar` parses the document source.)
   
If any plugins are needed, Perl is also required.

---

Nicola Talbot [dickimaw-books.com](https://www.dickimaw-books.com/)

License GPLv3+: GNU GPL version 3 or later
http://gnu.org/licenses/gpl.html
This is free software: you are free to change and redistribute it.
There is NO WARRANTY, to the extent permitted by law.
