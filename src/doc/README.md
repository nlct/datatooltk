# datatooltk

Java application to create and edit files in a format compatible
with the LaTeX datatool.sty package.

Author: Nicola Talbot (https://www.dickimaw-books.com/contact)

Application Home Page : https://www.dickimaw-books.com/software/datatooltk

## Licence

Copyright (C) 2013-2024 Nicola L. C. Talbot (dickimaw-books.com)

License GPLv3+: GNU GPL version 3 or later
http://gnu.org/licenses/gpl.html
This is free software: you are free to change and redistribute it.
There is NO WARRANTY, to the extent permitted by law.

## Requirements

  - Java Runtime Environment (at least Java 8) and, if plugins required, Perl;
  - TeX distribution that includes `datatool.sty`.

## Summary

A Java application that can be used to edit database files
that use `datatool.sty`'s internal format. It can also be used to
import data from CSV files, spreadsheets (ODS and XLSX), 
and TeX files that define problems using commands
from `probsoln.sty`. Depending on your installation, you may also be
able to import data via an SQL SELECT statement or import from Microsoft's
old binary Excel (XLS) format.

## Applications

There are three DatatoolTk applications. Their availability depends
on your installation.

 - [Basic DatatoolTk](#basicdatatooltk): provides both batch and GUI
   mode. Supports imports from CSV/TSV, ODS, XLSX files and from
   MySQL databases.
 - [DatatoolTk Extra](#datatooltkextra): as basic, but additionally provides
   support for importing XLS files.
 - [DatatoolTk Restricted](#datatooltkrestricted): only provides
   batch mode. Supports imports from CSV/TSV, ODS, and XLSX files.
   No database or XLS support.

### Basic DatatoolTk

The basic DatatoolTk application has both a batch mode and a 
graphical user interface (GUI) mode. It can import data from 
CSV/TSV, ODS, XLSX, and `probsoln.sty` files, and also from MySQL
databases. Additional libraries need to be installed and placed on the
Java class path if support for other database engines is required. The basic
application does not support XLS files.

If Perl is installed, the GUI mode can use the supplied Perl plugins
to autofill the entries of a new row for databases with a structure
applicable to the plugin. The batch mode doesn't support this.

Application properties are stored in an application directory (for
example, `~/.datatooltk` on Unix-like systems) so default
preferences are remembered the next time the application is run.

The DatatoolTk application is in a file called
`datatooltk.jar`. If this file is included in your installation,
then you can run it using (replace `path/to/lib` as applicable):
```bash
java -jar path/to/lib/datatooltk.jar [options]
```
If the options list includes `--gui` then DatatoolTk will run in 
GUI mode. Otherwise it will run in batch mode.

You may find that there is also a script called `datatooltk`
included in your installation which does this for you. There may
additionally be a script called `datatooltk-gui`, which is similar
but includes `--gui` and a splash screen on startup.

The `datatooltk.jar` file requires the libraries: `datatooltklib.jar`,
`datatooltkguilib.jar`, 
`texjavahelplib.jar` ([TeX Java Help](https://github.com/nlct/texjavahelp), GPL-3.0),
`texparserlib.jar` ([TeX Parser Library](https://github.com/nlct/texparser), GPL-3.0), and 
`mysql-connector-java.jar` ([MySQL connector](http://dev.mysql.com/downloads/connector/j/), GPL-2.0).

### DatatoolTk Extra

The DatatoolTk Extra application provides all the functionality of
the basic DatatoolTk application, but additionally includes the
ability to import data from Microsoft's old binary Excel (XLS)
files. This requires an additional library:
`poi`_version_`.jar` ([Apache POI](http://poi.apache.org/), Apache-2.0)
which requires `apache-commons-io.jar` ([Apache Commons IO](https://commons.apache.org/io), Apache-2.0),
which in turn requires `log4j-api.jar` ([Log4j](https://logging.apache.org/log4j), Apache-2.0).

The DatatoolTk Extra application is in a file called
`datatooltk-extra.jar`. If this file is included in your installation,
then you can run it using (replace `path/to/lib` as applicable):
```bash
java -jar path/to/lib/datatooltk-extra.jar [options]
```
If the options list includes `--gui` then DatatoolTk Extra will run in 
GUI mode. Otherwise it will run in batch mode.

You may find that there is also a script called `datatooltk-extra`
included in your installation which does this for you. There may
additionally be a script called `datatooltk-extra-gui`, which is similar
but includes `--gui` and a splash screen on startup.


### DatatoolTk Restricted

The DatatoolTk Restricted application can only be run in batch mode
and does not support imports from SQL or XLS.

The DatatoolTk Restricted application is in a file called
`datatooltk-restricted.jar`. If this file is included in your installation,
then you can run it using (replace `path/to/lib` as applicable):
```bash
java -jar path/to/lib/datatooltk-restricted.jar [options]
```
You may find that there is also a script called `datatooltk-restricted`
included in your installation which does this for you.
The `--gui` switch is not available.

There is no support for saving or loading preferred defaults in the
DatatoolTk application directory.

The DatatoolTk Restricted application requires the libraries:
`datatooltklib.jar`, `texjavahelplib.jar` and `texparserlib.jar`
(all GPL-3.0).

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
and `java/thirdparty` directories. This will need the same libraries
as `datatooltk.jar` but will also require the Apache POI, Apache
Commons and Log4j API libraries on the class path.
