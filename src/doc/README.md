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

 - [Basic DatatoolTk](#basic-datatooltk): provides both batch and GUI
   mode. Supports imports from CSV/TSV, ODS, XLSX files and from
   MySQL databases.
 - [DatatoolTk Extra](#datatooltk-extra): as basic, but additionally provides
   support for importing XLS files.
 - [DatatoolTk Restricted](#datatooltk-restricted): only provides
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

